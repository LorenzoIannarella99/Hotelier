import java.io.*;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.*;

public class MainServer { 
    public static final String configFile = "server.properties";
    private static File file_hotel;
    private static File file_gestore;
    private static File file_utenti;
    private static File file_topHotels;
    private static int startDelay;
    private static int salvataggioDelay;
    private static int maxDelay;
    private static int notificaDelay; // mi dice il tempo che intercorre tra un esecuizione e l'altra del thread che gestisce il calcolo del ranking con eventuale notifica 
    private static int corePoolSize;
    private static int maxPoolSize;
    private static int keepAliveTime;
    private static int portTCP;
    private static ServerSocket serverSocket;
    private static MulticastSocket senderSocket;
    private static String addressMulticast;
    private static int multicastPort;
    // Struttura dati che gestisce le informazioni inerenti agli utenti registrati
    private static ConcurrentHashMap<String,User> utenti; 
    // Struttura dati che associa ad ogni città una lista di coppie composta da hotel e lista di rencensioni
    private static ConcurrentHashMap<String,List<Coppia>> hashMap;
    // Struttura dati che mantiene per ogni città l'hotel che possiede il top rank
    private static ConcurrentHashMap<String,String> topHotels;
    // Gestore del pool di thread che gestiranno le richeste dei vari utenti
    private static ThreadPoolExecutor gestore;
    // Gestore che si occupa della schedulazione del task che calcola e aggiorna periodicamente il rank locale degli hotel e
    // si occupa schedulare un task che si occupa di effettuare il salvataggio delle strutture dati in memoria.
    private static ScheduledExecutorService schedulatore;


    /* Permette di effettuare una corretta chiusura del server indotta dal segnale provocato dal comando "ctrl+c" */
    private static class TerminationHandlerServer extends Thread {
        private int maxDelay;
        private ThreadPoolExecutor pool;
        private ServerSocket socketTCP;
        private MulticastSocket mSocket;
        private ScheduledExecutorService schedulatore;

        public TerminationHandlerServer(int maxDelay, ThreadPoolExecutor pool, ServerSocket socket, MulticastSocket mSocket, ScheduledExecutorService schedulatore){
            this.maxDelay = maxDelay; this.pool = pool; this.socketTCP = socket; this.mSocket = mSocket; this.schedulatore = schedulatore;
        }

        /* Effettua il salvataggio delle strutture dati in memoria, effettua la chiusura delle socket e fa terminare tutti thread */  
        public void run() {
            try {
                if(this.socketTCP != null) this.socketTCP.close();
                if(this.mSocket != null) this.mSocket.close();
            
            } catch (IOException e) {
                System.err.printf("Errore: %s", e.getMessage());
            }

            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                // Effettuo il salvataggio della struttura dati che mantiene tutte le informazioni inerenti agli hotel.
                FileWriter file1 = new FileWriter(file_gestore);
                Type localRankType = new TypeToken<ConcurrentHashMap<String,List<Coppia>>>(){}.getType();
                String salvataggio1 = gson.toJson(hashMap,localRankType);
                file1.write(salvataggio1);
                file1.close();
                
                // Effettuo il salvataggio della struttura dati che mantiene le informazioni in merito agli utenti registrati.
                FileWriter file2 = new FileWriter(file_utenti);
                Type utentiType = new TypeToken<ConcurrentHashMap<String,User>>(){}.getType();
                String salvataggio2 =gson.toJson(utenti, utentiType);
                file2.write(salvataggio2);
                file2.close();
                
                // Effettuo il salvataggio della struttura dati che mantiene informazioni dei tophotels.
                FileWriter file3 = new FileWriter(file_topHotels);
                Type topHotelsType = new TypeToken<ConcurrentHashMap<String,String>>(){}.getType();
                String salvataggio3 = gson.toJson(topHotels, topHotelsType);
                file3.write(salvataggio3);
                file3.close();
            } catch (Exception e) {
                System.err.printf("Errore: %s", e.getMessage());
            }
            this.pool.shutdown();
            this.schedulatore.shutdown();
            try {
                if(!pool.awaitTermination(this.maxDelay, TimeUnit.MILLISECONDS)) {
                    this.pool.shutdownNow();
                    System.out.println("[SERVER]: Threads terminati.");
                    this.schedulatore.shutdownNow();
                    System.out.println("[SERVER]: Threads per la schedulazione delle notifiche e salvataggio dati, terminati.");
                    System.out.println("[SERVER]: Chiusura avvenuta con successo.");
                }
            } catch (InterruptedException e) {
                this.pool.shutdownNow();
                this.schedulatore.shutdownNow();
                System.out.println("[SERVER] Hoteliers chiuso.");
            }

            System.out.println("[SERVER]: Chiusura avvenuta con successo.");
        }
    } 



    /* Qui devono essere inilizializzate tutte le strutture dati caricate dai file */
    private static void inizianilzzatore(){
        System.out.println("[SERVER] Inizio inizializzazione\n");
        try {

            /* Inizializzazione della HashMap che mantiene le informazioni in merito agli hotel */     
            hashMap = new ConcurrentHashMap<String,List<Coppia>>();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            // In caso di prima attivazione del server creo la struttura dati che dovrà persistere 
            if(!file_gestore.exists()){
                Type listType = new TypeToken<List<Hotel>>(){}.getType();
                List<Hotel> listaH = gson.fromJson(new FileReader(file_hotel), listType);
                for (Hotel h : listaH) {
                    hashMap.compute(h.getCity(),(K,V)->{
                        if( V==null ){
                            Coppia coppia = new Coppia(h,new ArrayList<Recensioni>());
                            List<Coppia> new_v = new ArrayList<Coppia>(); 
                            new_v.add(coppia);
                            return new_v;
                        }
                        else {
                            V.add(new Coppia(h,new ArrayList<Recensioni>()));
                            return V;
                        }
                    });
                }

            // Caso in cui la struttura dati dedicata al mantenimento delle informazioni degli hotels esiste già 
            }else{
                Type typeHashmap = new TypeToken<ConcurrentHashMap<String,List<Coppia>>>(){}.getType();
                hashMap = gson.fromJson(new FileReader(file_gestore), typeHashmap);
            }

            /* Inizializzazione della struttura dati che mantiene le informazioni degli utenti registrati */
            
            // Caso in cui la struttura dati esiste già            
            if(file_utenti.exists()){
                Type typeUtenti = new TypeToken<ConcurrentHashMap<String,User>>(){}.getType();
                utenti = gson.fromJson(new FileReader(file_utenti), typeUtenti);

            // In caso di prima attivazione del server, creo la struttura dati che dovrà persiste
            }else{
                utenti = new ConcurrentHashMap<String,User>();
            }

            /* Inizializzazione della struttura dati che mantiene le informazioni sui rank locali */

            // Caso in cui la struttura dati è già esistente 
            if(file_topHotels.exists()){
                Type typeTopHotels = new TypeToken<ConcurrentHashMap<String,String>>(){}.getType();
                topHotels = gson.fromJson(new FileReader(file_topHotels), typeTopHotels);

            // In caso di prima attivazione del server, creo la struttura dati che dovrà persistere 
            }else{
                topHotels = new ConcurrentHashMap<String,String>();
                for (Map.Entry<String,List<Coppia>> entry :hashMap.entrySet()) {
                    String città = entry.getKey();
                    topHotels.compute(città,(K,V)->"");
                }
            }
        } catch (Exception e) {
            System.err.printf("Errore : %s\n",e.getMessage());
        }
        System.out.println("[SERVER] Inizializzazione terminata correttamente\n");
    }

    /* Permette d'inizializzare tutte le variabili di configurazione del server  */
    
    private static void readConfig() throws FileNotFoundException,IOException{
        System.out.println("[SERVER] Inzio lettura del file di configurazione \n");
        InputStream input=MainServer.class.getResourceAsStream(configFile);
        Properties prop = new Properties(); 
        prop.load(input);
        file_gestore = new File(prop.getProperty("file_gestore"));
        file_hotel = new File(prop.getProperty("file_hotel"));
        file_utenti = new File(prop.getProperty("file_utenti"));
        file_topHotels = new File(prop.getProperty("file_topHotels"));
        maxDelay = Integer.parseInt(prop.getProperty("maxDelay"));
        startDelay = Integer.parseInt(prop.getProperty("startDelay"));
        salvataggioDelay = Integer.parseInt(prop.getProperty("salvataggioDelay"));
        corePoolSize = Integer.parseInt(prop.getProperty("corePoolSize"));
        maxPoolSize = Integer.parseInt(prop.getProperty("maxPoolSize"));
        keepAliveTime = Integer.parseInt(prop.getProperty("keepAliveTime"));
        portTCP = Integer.parseInt(prop.getProperty("portTCP"));
        multicastPort = Integer.parseInt(prop.getProperty("multicastPort"));
        addressMulticast = prop.getProperty("addressMulticast");
        notificaDelay = Integer.parseInt(prop.getProperty("notificaDelay"));
        input.close();
        System.out.println("[SERVER] Lettura file di configurazione completata\n");
    }


    public static void main(String[] args){
        try {
            readConfig();
            inizianilzzatore();

            //Inizializzazione delle scockets
            serverSocket = new ServerSocket(portTCP);
            senderSocket = new MulticastSocket(multicastPort);

            // Inizializzazione del gestore di richieste client 
            gestore = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

            // Inizializzazione dello schedulatore dei messassi Multicast
            schedulatore = Executors.newSingleThreadScheduledExecutor();

            // Lancio dell'handler per una corretta chiusura del MainServer 
            Runtime.getRuntime().addShutdownHook(new TerminationHandlerServer(maxDelay, gestore, serverSocket, senderSocket, schedulatore));

            // Lancio dello schedulatore dei messaggi Multicast
            schedulatore.scheduleWithFixedDelay(new MulticastHandler(senderSocket, addressMulticast, multicastPort, hashMap, topHotels),startDelay, notificaDelay, TimeUnit.SECONDS);

            // Lancio dello schedulatore per la gestone dei salvataggi delle strutture dati
            schedulatore.scheduleWithFixedDelay(new Salvataggio(hashMap, file_gestore, utenti, file_utenti, topHotels, file_topHotels), startDelay, salvataggioDelay , TimeUnit.SECONDS);

            // Gestione delle richieste da parte dei clients
            while (true) {
                try {
                    System.out.println("[SERVER] In ascolto sulla porta: "+portTCP+"\n");
                    gestore.execute(new Runner(serverSocket.accept(),utenti, hashMap));
                    System.out.println("[SERVER] Ho ricevuto una richiesta\n");   
                } catch (SocketException e) {break;}
            }
        }catch (Exception e) {
            System.err.printf("[SERVER] : %s\n",e.getMessage());
            System.exit(1);
        }
    }


}