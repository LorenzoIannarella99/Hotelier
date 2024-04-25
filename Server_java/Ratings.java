import java.io.Serializable;
/*
 * Classe che mi permette di rappresentare le valutazioni effetuabili su un hotel 
 */

public class Ratings implements Serializable {
    
    private int cleaning; 
    private int position;
    private int services;
    private int quality;

    public Ratings(int c, int p, int s, int q){
        this.cleaning = c;
        this.position = p;
        this.services = s;
        this.quality = q; 
    }

    // ritorna il punteggio associato alla valutazione  puliza dell'hotel 
    public int  getCleaning(){
        return this.cleaning;
    }
    // permette di modificare il punteggio associato alla valutazione della pulizia dell'hotel 
    public void setCleaning(int new_cleaning){
        this.cleaning = new_cleaning;
    }
    // ritona il punteggio associato alla valutazione della posizione dell'hotel
    public int getPosition(){
        return this.position;
    }
    // permette di modificare il punteggio associato alla valutazione della posizione dell'hotel
    public void setPosition(int new_position){
        this.position = new_position;
    }
    // ritorna il punteggio associato alla valutazione della qualità dell'hotel
    public int getQuality(){
        return this.quality;
    }
    // permette di modificare il punteggio associato alla valutazione della qualità dell'hotel 
    public void setQuality(int new_quality){
        this.quality = new_quality;
    }
    // ritorna il punteggio associato alla valutazione dei servizi offerti dall'hotel 
    public int getServices(){
        return this.services;
    }
    // permette di modificare il punteggio associato alla valutazione dei servizi offerti dall'hotel 
    public void setServices(int new_servicies){
        this.services = new_servicies;
    }
    // ritona una stringa formattata che rapprententa tutte le valutazioni dell'hotel
    public String toString(){
        return 
        "Position -> "+this.getPosition()+"\n"+
        "Cleaning -> "+this.getCleaning()+"\n"+
        "Services -> "+this.getServices()+"\n"+
        "Quality -> "+this.getQuality();
    }

}
