import java.io.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson; 
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.*;

/*
 * È un task che verra gestito da uno schedulatore di task.
 * Tale task si occupa di effettuare il salvataggio  delle strutture dati in memoria
*/

public class Salvataggio implements Runnable{

    private ConcurrentHashMap<String,List<Coppia>> hashMap;
    private ConcurrentHashMap<String,User> utenti;
    private ConcurrentHashMap<String,String> topHotels;
    private File file_gestore;
    private File file_utenti;
    private File file_topHotels;

    public Salvataggio(ConcurrentHashMap<String,List<Coppia>> hashMap, File file_gestore,
        ConcurrentHashMap<String,User> utenti, File file_utenti,
        ConcurrentHashMap<String,String> topHotels, File file_topHotels){
        
        // Struttura dati che mantiene le informazioni degli hotels e delle loro recensioni 
        this.hashMap = hashMap;
        this.file_gestore = file_gestore;
        // Struttura dati che mantiene le informazioni sugli utenti registrati
        this.utenti = utenti;
        this.file_utenti = file_utenti;
        // Struttura dati di supporto che mantiene le informazioni in merito ai rank locali
        this.topHotels = topHotels;
        this.file_topHotels = file_topHotels;
    }
    

    public void run(){
        System.out.println("[SERVER]: Salvataggio delle strutture dati iniziato..");
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
        } catch (IOException e) {
            System.err.printf("Errore: %s", e.getMessage());
        }
        System.out.println("[SERVER]: Salvataggio effettuato con successo.");
    }
}
