import java.io.Serializable;
import java.util.Date;

/*
 * Classe che mi permette di rappresentare le recensioni effettuate dagli utenti
 * che vengono poi salvate nella lista di recensioni associata ad ogni hotel.
 * Sono molto simili ai "ratings" ad eccezion fatta per la data,
 * ogni recensione conserva la data in cui è stata effettuata, questo sarà utile
 * per il calcolo dei rank locali
 */

public class Recensioni implements Serializable { 
     
    private int rate;
    private int cleaning;
    private int position;
    private int services;
    private int quality;
    private Date data ;

    public Recensioni(int r, int c, int p, int s, int q){
        this.rate = r;
        this.cleaning = c;
        this.position = p;
        this.services = s;
        this.quality = q;
        this.data = new Date();
    }

    public int getRate(){
        return this.rate;
    }

    public void setRate(int new_rate){
        this.rate = new_rate;
    }

    public int  getCleaning(){
        return this.cleaning;
    }
    
    public void setCleaning(int new_cleaning){
        this.cleaning = new_cleaning;
    }

    public int getPosition(){
        return this.position;
    }

    public void setPosition(int new_position){
        this.position = new_position;
    }

    public int getQuality(){
        return this.quality;
    }

    public void setQuality(int new_quality){
        this.quality = new_quality;
    }
 
    public int getServices(){
        return this.services;
    }

    public void setServices(int new_servicies){
        this.services = new_servicies;
    }

    public Date getData(){
        return this.data;
    }

    public String toString(){
        return "Rate : "+this.getRate()+",\n"+
        "Cleaning : "+this.getCleaning()+",\n"+
        "Position : "+this.getPosition()+",\n"+
        "Quality : "+this.getQuality()+",\n"+
        "Services : "+this.getServices()+".\n"; 
    }

}
