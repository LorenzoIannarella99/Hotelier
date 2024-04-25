import java.net.*;
import java.util.Comparator;
import java.util.List; 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*Questa classe rapprensenta un thread che viene schedulato periodicamente per effettuare l'aggiornamento del 
 * rank locale e per notificare a tutti gli utenti loggati, gli hotel che sono i migliori nella loro città
 */

public class MulticastHandler implements Runnable {

    private MulticastSocket sendSocket;
    private String multicastAddress;
    private int multicastPort;
    private ConcurrentHashMap<String,List<Coppia>> hashMap;
    private ConcurrentHashMap<String,String> topHotels;

    public MulticastHandler(MulticastSocket s, String address, int port, ConcurrentHashMap<String,List<Coppia>> hashMap, ConcurrentHashMap<String,String> topHotels){
        this.sendSocket = s; 
        this.multicastAddress = address; 
        this.multicastPort = port;
        this.hashMap = hashMap; 
        this.topHotels = topHotels;
    }
    
    public void run() {
        System.out.println("[SERVER]: Inizio controllo dei rank locali ..");
        try {
            InetAddress grop = InetAddress.getByName(this.multicastAddress);
            this.sendSocket = new MulticastSocket(this.multicastPort);
        
            for (Map.Entry<String,List<Coppia>> entry : this.hashMap.entrySet()){
                String città = entry.getKey();
                this.hashMap.compute(città,(K,V)->{
                // per ogni città riorna gli hotel dal migliore al peggiore basandosi sul valore restituito dal metodo "getScore"
                // della classe coppia
                    Comparator<Coppia>comparaScore = Comparator.comparingInt(Coppia::getScore).reversed();
                    V.sort(comparaScore);
                    return V;
                });
                // controlla se il miglior hotel di una città è cambiato
                String name_top_hotel = entry.getValue().get(0).getHotel().getName();
                // se il miglior hotel appena calcolato è diverso da quello che era precedentemente il migliore 
                if(!topHotels.get(città).equals(name_top_hotel)){
                    // aggiorna la struttura dati di supporto che conserva le informazioni inerenti ai miglior hotel delle città
                    topHotels.compute(città,(K,V)->name_top_hotel);
                    // notifica tutti gli utenti loggati su qual'è il nuovo miglior hotel della zona
                    String messaggio = città+"#"+name_top_hotel;
                    byte[] notifica = messaggio.getBytes("UTF-8");
                    DatagramPacket pacchetto = new DatagramPacket(notifica, notifica.length, grop, this.multicastPort);
                    sendSocket.send(pacchetto);
                }
            }
            sendSocket.close();
        } catch (Exception e) {
            e.getStackTrace();
        }
        System.out.println("[SERVER]: Controllo effettuato con successo");
    }
}
