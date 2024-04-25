import com.google.gson.Gson; 
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.lang.reflect.*;

public class MainClient {
    public static final String configFile ="client.properties";

// per la notifica multicast
    private static String multicast_Address;
    private static int multicast_Port;
    public static volatile Boolean multicastC_end;
    private static ClientMulticast thread_Multicast;
    private static String netIface;

// per le connessioni TCP con il server
    private static Socket socketTCP; 
    private static Scanner inTCP;
    private static PrintWriter outTCP;
    public static String hostname; 
    public static int PORT;
// variabili utilizzate per aumentare la leggibilità del codice
    private static Boolean login;
    private static String username;
    private static String password;
    private static String hotel;
    private static String città;
// variabile che mi permette di gestire il while, per una corretta terminazione del programma 
    private static Boolean endCLI = false;
// scanner che mi permette di catturare i comandi dell'utente 
    private static Scanner scanner = new Scanner(System.in);
// Coda che mi permette di salvare e gestire le notifiche ricevute dal server 
    private static BlockingQueue<String> coda_notifiche;

    // classe di supporto definita per poter raccoglie le valutazioni degli utenti
    public static class Giudizio {
        private String parametro;
        private int valutazione;
        public Giudizio(String parametro, int valutazione){
            this.parametro = parametro; this.valutazione = valutazione;
        }
        public String getParametro(){
            return this.parametro;
        }
        private int getValutazione(){
            return this.valutazione;
        }
        
    }

    private static class TerminationHandlerClient extends Thread {
        
        public void run(){
            if(login){
                cli("exit");
            }else{
                System.out.println("> Uscita da Hoteliers effettuata con successo");
            }
        }
    }
    

    private static void readConfig() throws FileNotFoundException,IOException{
        InputStream input=MainClient.class.getResourceAsStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        PORT = Integer.parseInt(prop.getProperty("PORT"));
        login = Boolean.parseBoolean(prop.getProperty("login"));
        endCLI = Boolean.parseBoolean(prop.getProperty("endCLI"));
        multicast_Address = prop.getProperty("multicast_Address");
        multicast_Port = Integer.parseInt(prop.getProperty("multicast_Port"));
        multicastC_end = Boolean.parseBoolean(prop.getProperty("multicastC_end"));
        netIface = prop.getProperty("netIface");
        input.close();
    }

/*Gestisce  la ricezione di messaggi di multicast 
 *la "notifica" contiene il nome dell'hotel primo nel ranking locale e la città.
 */
    public static class ClientMulticast extends Thread {
        
        private final BlockingQueue<String> coda_notifiche;
        
        public ClientMulticast(BlockingQueue<String> coda){
            this.coda_notifiche = coda;
        }

        public synchronized void run(){
            try (MulticastSocket receiverSocket = new MulticastSocket(multicast_Port)){
                //  Setto il timeOut di attesa per la ricezione del pacchetto UDP
                receiverSocket.setSoTimeout(2000);
                // Ottengo l'indirizzo del gruppo multicast
                InetAddress ia = InetAddress.getByName(multicast_Address);
                // Ottengo l'interfaccia di rete associata all'IP  di muticast
                NetworkInterface iface = NetworkInterface.getByName(netIface);
                // Mi unisco al gruppo multicast specificando ip e interfaccia di rete 
                receiverSocket.joinGroup(new InetSocketAddress(ia,multicast_Port),iface);
                // Inizializzo il buffer e il pachetto datagram per la ricezione dei messaggi inviati sul gruppo Multicast
                byte[] buf = new byte[1024];
                DatagramPacket receiver = new DatagramPacket(buf,buf.length);
                // Continuo ad attendere e ricevere pacchetti finchè l'utente non effettua il loguot oppure esce da Hoteliers
                while (!multicastC_end) {
                    // try-carch usato per gestire l'eccezione generata dallo scadere del timeout di attesa 
                    try{receiverSocket.receive(receiver);}catch(SocketTimeoutException e){continue;}
            
                    // Manipolo i bytes ricevuti e li converto in stringa per poi inserirli in una coda di notifiche 
                    try {
                        String[] mess = new String(receiver.getData(),0,receiver.getLength(),"UTf-8").split("#");
                        String notifica = "\n"+"[NOTIFICA]: Aggiornamento sui migliori Hotels"+"\n"+"<TOP> Hotel: "+mess[1]+" Città: "+mess[0]+"\n";
                        this.coda_notifiche.put(notifica);
                    }catch(Exception e){
                        System.err.println("Errore :"+e.getMessage());
                    }
                }
                // Lascio il gruppo e chiudo la socket 
                receiverSocket.leaveGroup(new InetSocketAddress(ia,multicast_Port),iface);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
    
    // Metodo che gestisce la fase di registrazione comunicando con il server 
    private static void register(String username, String password){
        try {
            socketTCP = new Socket(InetAddress.getLoopbackAddress(), PORT);
            inTCP = new Scanner (socketTCP.getInputStream());
            outTCP = new PrintWriter(socketTCP.getOutputStream(),true);
            // Inizio della comunicazione
            outTCP.println("R"+"#"+username+"#"+password);
            if(inTCP.nextLine().equals("OK")){
                System.out.println("> Registrazione avvenuta con successo");
            }else{
                System.out.println("> Username già in uso");
            }
            socketTCP.close();inTCP.close();outTCP.close();
        } catch (Exception e) {
            System.err.printf("Errore : %s\n",e.getMessage());
            System.exit(1);
        }

    }
    // Metodo che gestisce la fase di login comunicando con il server
    // Questo metodo instaura una connessione TCP permamente 
    private static void login(String username, String password){
        try {
            // Inizializzo la socket sul quale verrà mantenuta una connessione persistente con il server
            socketTCP = new Socket(hostname, PORT);
            // Inizializzo lo scanner che mi permette di leggere dalla socket quello che arriva dal server
            inTCP = new Scanner (socketTCP.getInputStream());
            // Inizializzo il printwriter che mi permette di scrivere nella socket ciò che deve arrivare al server
            outTCP = new PrintWriter(socketTCP.getOutputStream(),true);
            // Inizio della comunicazione
            outTCP.println("L"+"#"+username+"#"+password);
            String codice = inTCP.nextLine();
            if(codice.equals("OK")) {
                login=true;
                thread_Multicast = new ClientMulticast(coda_notifiche);
                thread_Multicast.start();
                System.out.println("> Accesso effettuato con successo!");
            }else if(codice.equals("ERRORE1")){
                System.out.println("> Password non corretta");
            }else if(codice.equals("ERRORE3")){ 
                System.out.println("> Utente già loggato");
            }else{
                System.out.println("> Username non trovato");
            }
        } catch (Exception e) {
        System.err.printf("Errore : %s\n",e.getMessage());
        System.exit(1);
        }       
    };

    // Metodo che effettua il logout e cominca al server tale informazione in modo da poter interrompere
    // correttamente la connessione persistente 

    private static void logout(String username){
        if (login){
            login=false;
            outTCP.println("O"+"#"+username);
            if(inTCP.nextLine().equals("OK")){
                try {
                    socketTCP.close();inTCP.close();outTCP.close();
                } catch (Exception e) {
                    System.err.printf("Errore : %s\n", e.getMessage());
                }
                multicastC_end = true;
                System.out.println("> Hai effettuato il logout da HOTELIERS");
            }
        }else{
            System.out.println("> Non sei loggato");
        }
    };

    // Metodo che permette di effettuare la ricerca mirata di un Hotel;
    // tale metodo passa al server infomrazioni necessarie per potegli permettere di effettuare la ricerca.
    private static void searchHotel(String nomeHotel, String città){
        try {
            Gson gson = new GsonBuilder().create();
            Type TypeHotel = new TypeToken<Hotel>(){}.getType();
            // caso di utente non loggato 
            if(!login){
                // viene instaurata la connessione con il server 
                socketTCP = new Socket(hostname, PORT);
                inTCP = new Scanner (socketTCP.getInputStream());
                outTCP = new PrintWriter(socketTCP.getOutputStream(),true);
                
                // invio delle informazioni che permetteranno al server di effettuare la ricerca di tale hotel
                outTCP.println("SH"+"#"+nomeHotel+"#"+città);
                // risposta del server

                // caso in cui l'hotel è presente:
                // il server invia il json che rappresenta l'hotel ricercato
                if(inTCP.nextLine().equals("OK")){
                    outTCP.println("OK");
                    Hotel hotel = gson.fromJson(inTCP.nextLine(), TypeHotel);
                    // viene stampato a video le informazioni dell'hotel 
                    System.out.println(hotel.getInfo());
                }
                // caso in cui non viene trovato
                else{
                    System.out.println("Hotel non trovato");
                }
            // caso in cui l'utente è loggato
            }else{ 
                // non viene instaurata alcuna connessione in quanto è già esistente 
                // avviene direttamente la comunizione tra client e server
                outTCP.println("SH"+"#"+nomeHotel+"#"+città);
                if(inTCP.nextLine().equals("OK")){
                    outTCP.println("OK");
                    Hotel hotel = gson.fromJson(inTCP.nextLine(), TypeHotel);
                    System.out.println(hotel.getInfo());
                }
                else{
                    System.out.println("Hotel non trovato");
                } 
            }  
        } catch (Exception e) {
            System.err.printf("Errore : %s\n", e.getMessage());
        }
    };

    // Funzione di supporto per la "searchAllHotels"
    private static void info_hotels(String città){
        // Definisco ciò che mi serve per poter ricostruire l'istanza di un hotel a partire da un json
        Gson gson = new GsonBuilder().create();
        Type TypeHotel = new TypeToken<Hotel>(){}.getType();
        String  codice = inTCP.nextLine();
        // while che controlla comunicando con il server se sono prensenti altri hotel 
                while(codice.equals("OK")){
                    // nel caso in cui sono presente effettua la conversione da json a istanza di hotel 
                    Hotel h = gson.fromJson(inTCP.nextLine(), TypeHotel);
                    // stampa a video le informazioni necessarie
                    System.out.println(h.getInfo());
                    codice = inTCP.nextLine();
                }
                if(codice.equals("FINITO")){
                    System.out.println("> Questi sono gli hotel presenti in "+città);
                }else if(codice.equals("ERRORE")){
                    System.out.println("> Non ci sono hotel presenti in "+città);
                }
    }
 
    // Metodo che permette di ottenere le informazioni inerenti a tutti gli hotel presenti in una città selezionata
    public static void searchAllHotels(String città){
        try {
            // caso in cui l'utente non è loggato 
            if(!login){
                // viene instaurata la connessione
                socketTCP = new Socket(hostname, PORT);
                inTCP = new Scanner (socketTCP.getInputStream());
                outTCP = new PrintWriter(socketTCP.getOutputStream(),true);
                outTCP.println("SHA"+"#"+città);
                info_hotels(città);
            }
            // caso in cui l'utente è loggato
            else{
                outTCP.println("SHA"+"#"+città);
                info_hotels(città);
            }
        } catch (Exception e) {
            System.err.printf("Errore : %s\n", e.getMessage());
        }


    };

    // Metodo ausiliario che mi dice se un hotel è presente in una tale città
    private static boolean check(String hotel, String città){
        outTCP.println("C"+"#"+città+"#"+hotel);
        if(inTCP.nextLine().equals("ERRORE")) return false;
        return true;
    }

    // Metodo che permette di scrivere una recenzione di un hotel e di comunicarla al server 
    private static void insertReview (String nomeHotel, String nomeCittà, int globalScore, List<Giudizio> singleScores){
        // definisco il tipo della recenzione, mi serve per trasformarla in un json 
        Type TypeRecensione = new TypeToken<Recensioni>(){}.getType();
        Gson gson = new GsonBuilder().create();
        // creo la rencesione basandomi sui dati presi in input 
        Recensioni recensione = new Recensioni(globalScore, singleScores.get(0).getValutazione(), 
        singleScores.get(1).getValutazione(), singleScores.get(2).getValutazione(), 
        singleScores.get(3).getValutazione());
        // creo il json della recensione 
        String str_recensione = gson.toJson(recensione, TypeRecensione);
        // invio la recensione e l'username dell'utente al server 
        outTCP.println("IR"+"#"+nomeCittà+"#"+nomeHotel+"#"+str_recensione+"#"+username);
        // attendo l'esito del server
        String codice = inTCP.nextLine();
        if(codice.equals("OK")) System.out.println("> Recensione inserita con successo\n");
        
    };

    // Metodo che mi permette di visualizzare l'ultimo badge ottenuto
    private static synchronized void showMyBadges(){
        if(!login){
            System.out.println("> Per effettuare questa operazione devi essere loggato\n");
        }else{
            outTCP.println("B"+"#"+username);
            String bagde = inTCP.nextLine();
            System.out.println("> "+bagde);
        }
        
    };

    // Metodo che auita l'utente, informandolo di tutte le operazioni eseguibili
    private static void stampaInfo(){
        System.out.println("> Digita <REGISTER> per registarti se non sei registrato\n");
        System.out.println("> Digita <LOGIN> per accedere ad HOTELIERS se sei registrato\n");
        System.out.println("> Digita <LOGOUT> per effettuare il logout\n");
        System.out.println("> Digita <SEARCH> per cercare un hotel\n");
        System.out.println("> Digita <SEARCH ALL> per trovare tutti gli hotel in una città\n ");
        System.out.println("> Digita <INSERT REVIEW> per aggiungere una recenzione\n ");
        System.out.println("> Digita <BADGE> per controllare il tuo distintivo\n");
        System.out.println("> Digita <RETURN> per tornare all'azione precendete\n");
        System.out.println("> Digita <EXIT> per uscire da HOTELIERS\n");
        System.out.println("> Digita <INFO> per visualizzare tutte le operazioni\n");
    }

    // Metodo che gestisce l'interazione con l'utente 
    private static synchronized void cli(String output){
        // Espressione regolare con la quale limito i caratteri utilizzabili per la registrazione 
        String valid_input = "[\\p{Alnum}]+" ;
        // switch che gestisce tutte le casistiche 
        switch (output) {
            case "register":
                if(login){ 
                    System.out.println("> Non puoi effettuare questa operazione\n");
                    break;
                }
                System.out.println("> Crea un Username\n");
                System.out.printf(">>> ");
                username=scanner.nextLine().trim();
                if(username.equals("return")) break;

                System.out.println("> Crea una password\n");
                System.out.printf(">>> ");
                password=scanner.nextLine().trim();
                if(password.equals("return")) break;

                if(!password.matches(valid_input) || !username.matches(valid_input)){
                    System.out.println("> Registrazione fallita, puoi inserire solo caratteri alfanumerici\n");
                    break;}
                register(username, password);
                break;

            case "login":
                if(!login){ 
                    System.out.println("> Inserisci Username\n");
                    System.out.printf(">>> ");
                    username=scanner.nextLine().trim();
                    if(username.equals("return")) break;

                    System.out.println("> Inserisci Password\n");
                    System.out.printf(">>> ");
                    password=scanner.nextLine().trim();
                    if(password.equals("return")) break;

                    if(!username.matches(valid_input) || !password.matches(valid_input)){
                        System.out.println("> Inserimento non valido\n");
                        break;
                    }
                    login(username, password);
                }else{
                    System.out.println("> Sei già loggato\n");
                }
                break;

            case "logout":
                logout(username);
                break;

            case "search":
                System.out.println("> Inserisci il nome dell'Hotel che stai cercando\n");
                System.out.printf(">>> ");
                hotel = scanner.nextLine().trim();
                if(hotel.equals("return")) break;
                
                System.out.println("> Inserisci il nome della città in cui risiede l'Hotel\n");
                System.out.printf(">>> ");               
                città = scanner.nextLine().trim();  
                if(città.equals("return")) break;
                if(città.equals("")|| hotel.equals("")){
                    System.out.println("> Inserimento non validi\n");
                    break;
                }
                searchHotel(hotel, città); 
                break;

            case "search all":
                System.out.println("> Inserisci il nome della città\n");
                System.out.printf(">>> ");
                città = scanner.nextLine().trim();
                if (città.equals("")){
                    System.out.println("> Inserimento non valido\n");
                    break;
                }
                if(città.equals("return")) break;
                searchAllHotels(città);
                break;

            case "insert review":
                String cattura ;
                int global_score = 0;
                int servicies;
                int position;
                int quality;
                int cleaning;
                
                if(login){
                    // lista in cui raccolgo le valutazioni espresse dall'utente 
                    List<Giudizio> giudizi = new ArrayList<Giudizio>();
                    // gestisco la cattura degli hotel e delle città 
                    System.out.println("> Inserisci il nome dell'Hotel che vuoi recensire");
                    System.out.println("> Per tornare indietro digita < RETURN >\n");
                    System.out.printf(">>> ");
                    hotel = scanner.nextLine().trim();
                    if(hotel.equals("return")) break;
            
                    if(hotel.equals("")){
                        System.out.println("> Inserimento errato\n");
                        break;
                    }
                    System.out.println("> Inserisci il nome della città in cui risiede l'Hotel che vuoi recensire\n");
                    System.out.printf(">>> ");
                    città = scanner.nextLine().trim();
                    if(città.equals("return")) break;
                    if(città.equals("")) {
                        System.out.println("> Inserimento errato\n");
                        break;
                    }
                    //Controllo se l'hotel è presente nella città
                    if (!check(hotel, città)){
                        System.out.println("> Non è presente nessun hotel\n");
                        break;
                    }
                    // gestisco la raccolta del global score
                    while (true) {
                        try {
                            System.out.println("> Inserisci un valore da 0 a 5 per dare un giudizio complessivo all'Hotel\n");
                            System.out.printf(">>> ");
                            cattura = scanner.nextLine().toLowerCase().trim();
                            if (cattura.equals("return")) break;
                            global_score = Integer.parseInt(cattura);
                            if(global_score>=0 && global_score<=5){
                                break;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("> Inserisci un valore corretto\n");   
                        }
                    }

                    if (cattura.equals("return")) break;
                    //gestisco la raccolta delle valutazioni
                    while(true){
                        try{
                            System.err.println("> Inserisci un valore da 0 a 5 per dare un giudizio sulla pulizia\n");
                            System.out.printf(">>> ");
                            cattura = scanner.nextLine().toLowerCase().trim();
                            if (cattura.equals("return")) break;
                            cleaning = Integer.parseInt(cattura);
                            if(cleaning>=0 && cleaning<=5){
                                giudizi.add(new Giudizio("cleaning", cleaning));
                                break;
                            }
                        }catch(NumberFormatException e){
                            System.out.println("> Inserisci un valore corretto\n");
                        }
                    }

                    if (cattura.equals("return")) break;

                    while(true){
                        try{ 
                            System.err.println("> Inserisci un valore da 0 a 5 per dare un giudizio sulla posizione\n");
                            System.out.printf(">>> ");
                            cattura = scanner.nextLine().toLowerCase().trim();
                            if (cattura.equals("return")) break;
                            position = Integer.parseInt(cattura);
                            if(position>=0 && position<=5){
                                giudizi.add(new Giudizio("position", position));   
                                break;
                            }
                        }catch(NumberFormatException e){
                            System.out.println("> Inserisci un valore corretto\n");
                        }    
                    }

                    if (cattura.equals("return")) break;
                    
                    while(true){
                        try{ 
                            System.err.println("> Inserisci un valore da 0 a 5 per dare un giudizio sui servizi\n");
                            System.out.printf(">>> ");
                            cattura = scanner.nextLine().toLowerCase().trim();
                            if (cattura.equals("return")) break;
                            servicies = Integer.parseInt(cattura);
                            if(servicies>=0 && servicies<=5){
                                giudizi.add(new Giudizio("servicies", servicies));   
                                break;
                            }
                        }catch(NumberFormatException e){
                            System.out.println("> Inserisci un valore corretto\n");
                        }
                    }

                    if (cattura.equals("return")) break;

                    while(true){
                        try{ 
                            System.err.println("> Inserisci un valore da 0 a 5 per dare un giudizio sulla qualità\n");
                            System.out.printf(">>> ");
                            cattura = scanner.nextLine().toLowerCase().trim();
                            if (cattura.equals("return")) break;
                            quality = Integer.parseInt(cattura);
                            if(quality>=0 && quality<=5){
                                giudizi.add(new Giudizio("quality", quality));   
                                break;
                            }
                        }catch(NumberFormatException e){
                            System.out.println("> Inserisci un valore corretto\n");
                        }
                    }

                    if (cattura.equals("return")) break;
                    insertReview(hotel, città, global_score, giudizi);
                    break;
                }else{
                    System.out.println("> Devi essere loggato per rilasciare una recensione\n");
                }
                break;
                
            case "badge":
                showMyBadges();
                break;
            
            case "return":
                System.out.println("> Non puoi tornare indietro \n");
                break;
            
            case "info":
                stampaInfo();
                break;
            
            case "exit":
                endCLI = true;
                if(login){
                    thread_Multicast.interrupt();
                    if(!thread_Multicast.isInterrupted()){
                        thread_Multicast.interrupt();
                    }
                    logout(username);
                    System.out.println("> Stai uscendo da HOTELIERS\n");
                }
                break;
            
            case "":
                break;
                
            default:
                System.out.println("> comando non valido\n");
                break;
        }
    }

    public static void main(String[] args){
        try {
            readConfig();
        } catch (Exception e) {
            System.err.printf("Errore : %s", e.getMessage());
            System.exit(1);
        }
        // Per la gestione delle notifiche
        String notifica;
        coda_notifiche = new LinkedBlockingQueue<String>();

        // Lancio l'handler che gestisce correttamente la chiusura effettuata mediante "ctrl+c"
        Runtime.getRuntime().addShutdownHook(new TerminationHandlerClient());

        // Stampe iniziali del programma        

        System.out.println("> Benvenuto in HOTELIERS\n");
        System.out.println("> Digita < INFO > per visualizzare tutti i comandi se non li conosci\n");
        System.out.println("> Altrimenti digita un comando\n");
        System.out.printf(">>> ");
        String command=scanner.nextLine().toLowerCase().trim();
        
        cli(command);

        while (!endCLI) {
            
            System.out.println("> Cosa vuoi fare ?\n");
            System.out.printf(">>> ");
            command = scanner.nextLine().toLowerCase().trim();
            cli(command);

            
            while(!coda_notifiche.isEmpty() && login){
                try {notifica = coda_notifiche.take();} catch (InterruptedException e) {continue;}
                System.out.println(notifica);
            }
            
        }
        System.out.println("> HOTELIERS ti saluta \n");

        return;
    }

}