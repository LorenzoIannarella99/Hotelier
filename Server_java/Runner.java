import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import com.google.gson.Gson; 
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.*;


public class Runner implements Runnable{

    private Socket sok;
    private ConcurrentHashMap<String,User> utenti;
    private ConcurrentHashMap<String,List<Coppia>> hashMap;
    private Gson gson;
    private Scanner input;
    private PrintWriter out;
    private Boolean login;
    
    public Runner(Socket sok, ConcurrentHashMap<String,User> utenti,ConcurrentHashMap<String,List<Coppia>> hashMap){
        this.sok = sok;
        this.utenti = utenti;
        this.hashMap = hashMap;
        gson = new GsonBuilder().create();
        login = false;
    }
    // Gestisce la registrazione di un nuovo utente
    private void register_operation(String username, String password){
        long old_size = this.utenti.mappingCount();
        this.utenti.putIfAbsent(username, new User(username, password));
        long new_size = this.utenti.mappingCount();
        if (old_size < new_size){
            out.println("OK");
        }else{
            out.println("ERRORE");
        }  
    }
    // Gestisce il logging dell'utente
    private void login_operation(String username, String password){
        if(this.utenti.containsKey(username)){
            User user=this.utenti.get(username);
            // verifico se l'utente non è già online
            if(!user.getStatus()){
                if(user.getPassword().equals(password)){
                    out.println("OK");
                    login = true;
                    user.setStatus(true);
                }else{
                    out.println("ERRORE1");
                }
            }else{
                out.println("ERRORE3");
            }
        }else{
            out.println("ERRORE2");
        }
    }
    // Gestisce la ricerca di un preciso hotel di una città
    private void search_operation(String nome_hotel, String nome_città){
        if(hashMap.containsKey(nome_città)){ 
            List<Coppia> list = hashMap.get(nome_città);
            Type HotelType = new TypeToken<Hotel>(){}.getType();
            Boolean find = false;
            for (Coppia coppia : list) {
                if(coppia.getHotel().getName().equals(nome_hotel)){
                    String messaggio = gson.toJson(coppia.getHotel(), HotelType);
                    out.println("OK");
                    if (input.nextLine().equals("OK")) out.println(messaggio);
                    find = true;
                    System.out.println(find);
                    System.out.println(messaggio);
                    break;
                }
            }
            if(!find){
                out.println("ERRORE");
            }
        }else{
            out.println("ERRORE");
        }
    }
    // Funzione di supporto per verificare l'esistenza di un hotel
    private void check_oparation(String nome_città,  String nome_hotel){
        if(hashMap.containsKey(nome_città)){
            List<Coppia> lista = hashMap.get(nome_città);
            for (Coppia coppia : lista) {
                if (coppia.getHotel().getName().equals(nome_hotel)) {
                    break;
                }
            }
            out.println("OK");
        }else{
            out.println("ERRORE");
        }
    }
    // Gestisce la ricerca di tutti gli hotel presenti in una città
    private void searchAll_operation(String nome_città){
        if(hashMap.containsKey(nome_città)){
            List<Coppia> lista_hotel = hashMap.get(nome_città);
            Type TypeHotel = new TypeToken<Hotel>(){}.getType();
            for (Coppia coppia : lista_hotel) {
                out.println("OK");
                Hotel h = coppia.getHotel();
                String mess = gson.toJson(h, TypeHotel);
                System.out.println(mess);
                out.println(mess);
            }
            out.println("FINITO");
        }else{
            out.println("ERRORE");
        }
    }
    // Gestisce l'inserimento di una recensione effettuata da un utenete 
    private void insertReview_operation(String nome_città, String nome_hotel, String str_recensione, String username){
        Type TypeRecensione = new TypeToken<Recensioni>(){}.getType();
        Recensioni new_recensione = gson.fromJson(str_recensione,TypeRecensione);
        hashMap.compute(nome_città,(K,V)->{
            for (Coppia coppia : V) {
                if(coppia.getHotel().getName().equals(nome_hotel)){
                    // aggiunge la recensione alla lista di recensioni associata all'hotel
                    coppia.getRecensioni().add(new_recensione);
                    System.out.println("[SERVER] recensione inserita dell'hotel :"+nome_hotel+"nella città :"+nome_città);
                    // modifica le valutazioni e il global score dell'hotel in base alle recensioni presenti 
                    coppia.make_avg();
                    break;
                }
            }
            return V;
        });
        utenti.compute(username,(K,V)->{V.incr_Num_recensioni();V.addbadge();return V;});
        out.println("OK");    
    }
    // Funzione che restistuisce le informazioni inerenti ai badge degli utenti registrati 
    private synchronized void badge_operation(String username){
        out.println(this.utenti.get(username).getBadge());   
    }
    // Funzione che gestisce il logout lato server
    private void logout_operation(String username){
        if(login){
            // setto login a false in modo tale da uscire dal while del metodo "run()" e far terminare il thread  che manteneva
            // la connessione con il client
            login = false;
            // cambio lo status dell'utente cosi risulta essere non loggato
            this.utenti.get(username).setStatus(false);
            out.println("OK");
        }else{
            out.println("ERRORE");
        }
        
    }

    public synchronized void run(){
        try{
            input = new Scanner(this.sok.getInputStream());
            out = new PrintWriter(this.sok.getOutputStream(),true);

            do {
                String[] richiesta_utente = input.nextLine().split("#");
                String operazione = richiesta_utente[0];
                switch (operazione) {
                    case "R":
                        register_operation(richiesta_utente[1], richiesta_utente[2]);
                        break;

                    case "L":
                        login_operation(richiesta_utente[1], richiesta_utente[2]);
                        break;

                    case "SH":
                        search_operation(richiesta_utente[1], richiesta_utente[2]);
                        break;

                    case "SHA":
                        searchAll_operation(richiesta_utente[1]);
                        break;

                    case "C":
                        check_oparation(richiesta_utente[1], richiesta_utente[2]);
                        break;

                    case "IR":
                        insertReview_operation(richiesta_utente[1], richiesta_utente[2], richiesta_utente[3], richiesta_utente[4]);
                        break;

                    case "B":
                        badge_operation(richiesta_utente[1]);
                        break;

                    case "O":
                        logout_operation(richiesta_utente[1]);
                        break;

                    default:
                        break;
                }
            // caso in cui è stato effettuato il login, viene mantenuta la connessione e quindi un thread che gestisce
            // le comunicazione con il client 
            } while (login);

            input.close();out.close();
        } catch (Exception e) {
            System.err.printf("Errore : %s", e.getMessage());
        }
    }
}
