import java.io.Serializable;
/*
 * Classe che rappresenta tutte le informazioni in merito agli utenti registrati 
 */

public class User implements Serializable { 

    private String username;
    private String password;
    // mantiene il numero di recensioni effettuate dall'utente, inizialemtente settato a 0
    private int num_recensioni=0;
    // mantiene  tutti i badge ottenuti dall'utente
    private String[] badges = new String[5];
    // mantiene lo stato di un utente, ovvero se è loggato o meno, di default non è loggato 
    private Boolean status = false;

    public User(String username, String password){
        this.username=username; this.password=password;
    }
    
    // ritorna l'username dell'utente 
    public String getUsername(){
        return this.username;
    }
    // ritorna la password dell'utente 
    public String getPassword(){
        return this.password;
    }
    // ritorna il numero di recensioni effettuate dall'utente 
    public int getN_recensioni(){
        return num_recensioni;
    }
    // incrementa il numero di recensioni effettuate dall'utente 
    public void incr_Num_recensioni(){
        num_recensioni=num_recensioni+1;
    }
    // ritorna lo stato attuale dell'utente 
    public Boolean getStatus(){
        return status;
    }
    // permette di settare lo stato dell'utente 
    public void setStatus(Boolean value){
        status = value;
    }
    // ritorna l'ultimo badge ottenuto dall'utente 
    public String getBadge(){
        String distintivo="";
        if(badges[0] == null){return "nessun Distintivo";}
        else if(badges[badges.length-1] != null) return badges[badges.length-1];
        for (int i = 0 ;i < badges.length-1;i++){
            if(badges[i+1] == null){ 
                distintivo = badges[i];
                break;
            }
        }
        return distintivo;
    }
    // in base al numero di recensioni aggiuge o meno un badge all'utente 
    public void addbadge(){
        if(num_recensioni>0 && num_recensioni<=20) badges[0]="Recensore";
        else if(num_recensioni>20 && num_recensioni<=50) badges[1] ="Recensore Esperto";
        else if(num_recensioni>50 && num_recensioni<=100) badges[2] ="Contributore";
        else if(num_recensioni>100 && num_recensioni<=200) badges[3] ="Contributore Esperto";
        else if(num_recensioni>200) badges[4] ="Contributore Super";
        
    }

}
