import java.io.Serializable;
/*
 * Classe che mi permette di rappresentare gli hotels 
 */

public class Hotel  implements Serializable{
    private String id;
    private String name;
    private String description;
    private String phone;
    private String city;
    private String [] services;
    private int rate;
    private Ratings ratings;
    

    public Hotel(String id, String name, String description, String phone, String city, String[] servicies, int rate, Ratings ratings ){
        this.id = id; 
        this.name = name; 
        this.phone = phone;
        this.description = description; 
        this.city = city;
        this.services = servicies;
        this. rate = rate;
        this.ratings = ratings;
    }
    // restituisce l'id dell'hotel
    public String getID(){
        return this.id;
    }
    // restituisce il nome dell'hotel
    public String getName(){
        return this.name;
    }
    // restituisce il numero dell'hotel
    public String getPhone(){
        return this.phone;
    }
    // restituisce la descrizione associata all'hotel
    public String getDescription(){
        return this.description;
    }
    // restituisce la città in cui ha sede l'hotel
    public String getCity(){
        return this.city;
    }
    // restituisce una stringa che illustra i servizi offerti dall'hotel
    public String getServices(){
        if(this.services.length==0) return "Non offre alcun servizio";
        return String.join(",",this.services);
    }
    // restitusce il global score dell'hotel, ottenuto dalla media di tutti global score delle recensioni
    public int getRate(){
        return this.rate; 
    }
    // restitusce i punteggi delle altre valutazioni 
    public String getRating(){
        return this.ratings.toString();
    }
    // permette di modificare il global score dell'hotel
    public void setRate(int avg_rate){
        this.rate = avg_rate;
    }
    // permette di modificare i punteggi delle altre valutazioni
    public void setRatings(int avg_c, int avg_p, int avg_s, int avg_q ){
        this.ratings.setCleaning(avg_c);
        this.ratings.setPosition(avg_p);
        this.ratings.setServices(avg_s);
        this.ratings.setQuality(avg_q);
    }
    // ritorna una stringa formattata che illustra le info dell'hotel 
    public String getInfo(){
        
        return 
        "Nome --> "+this.getName()+"\n"+
        "Global Rate --> "+this.getRate()+"\n"+
        this.getRating()+"\n\n"+
        "Servizi :"+"\n"+this.getServices()+"\n\n"+
        "Descrizione :"+"\n"+this.getDescription()+"\n"+
        "Telefono -> "+this.getPhone()+"\n";
    }

}