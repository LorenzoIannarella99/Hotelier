import java.io.Serializable;
import java.util.*;

/*
 * Classe fondamentale per la gestione degli hotels, delle recensioni ad essi associate.
 */

public class Coppia implements Serializable{
    private Hotel hotel;
    private ArrayList<Recensioni> recensioni;

    public Coppia(Hotel h, ArrayList<Recensioni> r){
        this.hotel = h;
        this.recensioni = r; 
    }
    // restitusisce l'istanza hotel 
    public Hotel getHotel(){
        return this.hotel;
    }
    // restitusce la lista di recensioni associate all'hotel
    public List<Recensioni> getRecensioni(){
        return this.recensioni;
    }

    /*
     * metodo fondamentale per il calcolo del rank locale,
     * tale metodo restituisce un valore che permette di classificare l'hotel rispetto agli altri nella stessa città.
     * Il valore viene calcolato in base ai global score definiti nelle recensioni, il peso dei global score è influenzato
     * dall'anzianità delle recensioni (recensioni più recenti hanno peso maggiore), dal numero di recensioni (maggiore è 
     * il numero di recensioni e più peso queste avranno).
     */
    
    public int getScore(){
        long due_mesi = 5184000000L;
        long tempo_attuale = System.currentTimeMillis();
        List<Recensioni> recensioni = this.getRecensioni();
        int quantità = recensioni.size();
        int qualità = 0;
        for (Recensioni r : recensioni) {
            long tempo_recensione = r.getData().getTime();
            qualità+=(tempo_attuale-tempo_recensione<=due_mesi)?r.getRate():(r.getRate()*0.8);
        } 
        double score = (quantità<=50)?qualità*0.4:
        (quantità<=100)?qualità*0.6:
        (quantità<=200)?qualità*0.8:qualità;
        return (int) score;
    }
    // metodo che calcola la media sia delle valutazioni che del global score, tale media si basa su tutte le recensioni
    // ed infine modifica le valutazioni e il global score dell'hotel.  
    public void make_avg(){
        int rate = 0;
        int cleaning = 0;
        int position = 0;
        int quality = 0;
        int services = 0;
        List<Recensioni> recensioni = this.getRecensioni();
        for (Recensioni r : recensioni){
            rate += r.getRate();
            cleaning += r.getCleaning();
            position += r.getPosition();
            quality += r.getQuality();
            services += r.getServices();
        }
        Hotel hotel = this.getHotel();
        hotel.setRate(rate/recensioni.size());
        hotel.setRatings((int)cleaning/recensioni.size(),
                        (int)position/recensioni.size(),
                        (int)quality/recensioni.size(),
                        (int)services/recensioni.size());
    }
}