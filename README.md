# PROGETTO HOTELIER
## INFO
**JDK**
Il progetto è stato realizzato con JDK 19. Per come è stato scritto il codice non ci sono grosse  differenze con JDK 8, l'unica differenza  presente è nell'inserimento al gruppo multicast dove il metodo  joinGroup oltre a prendere in input un "InetAddress",  prende in input anche un "NetworkInterface" ovvero l'interfaccia di rete.

**Ubuntu**
Il progetto è stato realizzato su un sistema Ubuntu 22.04.4 LTS e per far si che il client recevesse i pacchetti UDP, inviati da un thread del server sul gruppo multicast,  ho dovuto aggiungere una regola al firewall di ubuntu dove specifico al firewall di far passare i pacchetti UDP sulla porta 4000 utilizzando il comando :
```bash
sudo ufw allow 4000/udp
```

**Librerie esterne**
Per scopi didattici ho deciso di utilizzare la libreria gson di Google. Nello specifico ho usato la **gson-2.10.1**, che permette di effettuare **serializzazione** e **deserializzazione** di oggetti java, funzionalità che sono state sfruttate per rendere persistenti o per reperire dalla memoria le strutture dati che il server usa per gestire tutte le informazioni .

## ISTRUZIONI 
Il codice è diviso in due cartelle, una contiene tutte le classi che consentono al server di funzionare correttamente e l'altra contiene tutte le classi per far funzionare correttamente il client. 
### 1) Compilazione :
La compilazione, sia del codice pertiente al server che quello pertinente al client, avviene eseguendo le stesse istruzioni da terminale ovvero :
```bash
javac -cp gson-2.10.1.jar:. \*.java
```
In sostanza, si effettua una compilazione considerando anche la libreria gson che viene caricata dinamicamente tramite il paramentro `-cp`. Tale parametro informa il compilatore java sul dove cercare le classi e i pacchetti, in questo caso il primo è `gson-2.10.1.jar` poi vi è il `:` che fa da separatore e poi vi è `. ` che indica la directory corrente. Da tale directory corrente con `*.java` viene indicato che devono essere compilati tutti i file java presenti.
### 2) Esecuzione
L'esecuzione può avvenire in due diverse modalità, la prima è eseguendo il comando da terminale ```java -cp gson-2.10.1.jar:. MainClient``` per lanciare il client e 
```java -cp gson-2.10.1.jar:. MainServer``` per lanciare il server.
Un'altra modalità con cui possono essere eseguiti sia il server che il client è mediante jar eseguendo i comandi da terminale ```java -cp Server.jar:gson-2.10.1.jar MainServer``` per lanciare il server e ```java -cp Client.jar:gson-2.10.1.jar MainClient``` per lanciare il client.

### 3) Argomenti da passare al codice :
Non sono previsti argomenti da passare al codice in quanto tutte le variabili utili da poter permettere una corretta esecuzione del codice, sia lato client che lato server, sono definite nei rispettivi file di configurazione.

### 4) Sintassi dei comandi :

**Lato Server**
È possibile terminare l'esecuzione del server mediante la digitazione del **"ctrl+c"**, questo provocherà come già spiegato nello schema generale dei thread, una corretta terminazione del MainServer e di tutti gli altri thread in esecuzione. 

**Lato Client**
Il client si occupa principalmente d'interagire con l'utente infatti esso è in grado di rispondere a diversi comandi.
Il client all'avvio suggerisce all'utente di digitare **info** per poter visualizzare tutte le operazioni che esso è in grado di gestire.
Per ogni operazione il client guida l'utente indicandogli cosa inserire e comunica gli inserimenti non validi. Questo approccio permette all'utente di avere un'iterazione con il client,  facile ed intuitiva.
In aggiunta alle operazioni definite nelle specifiche ho aggiunto due operazioni aggiuntive:
**return** e **exit**.
Il comando **return** permette all'utente di tornare indietro qual'ora decidesse di non voler più eseguire un comando e questo può essere fatto in qualunque fase dell'interazione. 
Il comando **exit** permette di effettuare una terminazione elegante del programma, effettuando anche il logout se l'utente ha effettuato il login.
La terminazione del programma può avvenire anche digitando **"ctrl+c"** anche in questo caso se l'utente ha effettuato il login viene effettuato il logout.

## SCELTE IMPLEMENTATIVE
### 1) LATO SERVER :
Ho scelto di implementare il server in modo tale che sia lui a gestire (tramite dei thread gestiti da un *ThreadPool*) tutte le strutture dati, utili a mantenere le informazioni in merito agli hotels,recensioni e gli utenti registrati.
Un servizio fondamentale di Hotelier è quello di mantenere una classifica degli hotel di ogni città  e d'informare gli utenti loggati ogni volta che cambia l'identità del primo classificato in qualche rancking locale.
La classifica viene definita basandosi sulla quantità di recensioni, la qualità delle recensioni (basandosi sul globar score di ogni recensione) e della attualità delle recensioni.
Ho scelto d'implementare tale calcolo considerando la **qualità** delle recensioni come la somma dei global score raccolti della recensioni di ogni hotel. Il global score viene influenzato con un peso che varia in base **all'attualità** delle recensioni ovvero, se una recensione è stata effettuata da più di due mesi, al suo global score verrà moltiplicato un peso di "0,4" altrimenti, se essa è stata effettuata da meno di due mesi, al global score verrà moltiplicato un peso di "1".
Il valore risultante di **qualità** viene poi influenzato dalla **quantità** di rencensioni, infatti, se esse risultano essere *minori di 50* al valore qualità viene moltiplicato un peso di "0,4", se risultano essere *comprese tra 50 incluso e 100 escluso* si moltiplica un peso di "0,6" altrimenti, se risultano essere *maggiori o uguali di 100* viene moltiplicato un  peso di "0.8".
Il risultato finale sarà il valore con il quale vengono disposti gli hotels in ordine decrescente nelle liste associate ad ogni città.

### 2) LATO CLIENT :
Ho scelto d'implementare il client in modo che si occupi solo di gestire l'interazione con l'utente mediante un metodo *cli()* che cattura per mezzo di uno **scanner** le operazioni che l'utente vuole effettuare.
Il client si occupa anche di ricevere le notifiche inviate dal server in merito all'aggiornamento dei rank locali, ciò avviene per mezzo di una coda *ThreadSafe* per poi mostrarle all'utente.

### 3) CONNESSIONE PERSISTENTE E NON :
Nelle specifiche del progetto era richiesto di stabilire una connessione persistente tra client e server dal momento in cui l'utente effettuava il login e di definire connessioni non persistenti per tutte le operazioni effettuate dagli utenti che non avevano effettuato il login.
Ho deciso d'implementare questa specifica gestendo in modo diverso le connessioni sia lato server che lato client, la distinzione viene fatta mediante la variabile booleana **login**. 
Per ogni operazione lato client vi è il controllo della variabile *login* che se *true* il metodo usa la socket aperta durante la fase di login ed usa quella per comunicare con il server, altrimenti, apre e chiude una socket dopo aver eseguito l'operazione richiesta dall'utente che ovviamente non necessitava del login per essere effettuata.
Per il lato Server (il codice che descrive il comportamento dei thread Runner gestiti dal **ThreadPoolExecutor gestore** ) la variabile di login viene utlizzata come condizione del *while* che impedisce al thread di terminare e quindi di chiudere la connessione. Ciò implica che l'utente che ha effettuato il login avrà un thread del server completamento dedicato.
Altrimenti, dopo aver fornito il servizio, il thread termina la sua esecuzione.
### 4) CLASSI :
**- User**
La classe *User* permette di creare oggetti che rappresentano gli utenti registrati.
Le istanze di questa classe posseggono attributi che rappresentano le proprietà dell'utente come *username, password, numero di recensioni effettuate, badge posseduti, status*.
Quest'ultimo permette al server di capire se tale utente registrato è loggato o meno.
La classe User racchiude dei metodi che gli permettono di modificare tali attributi come :
*- setStatus()* che permette di modificare lo stato dell'utente;
*- getBadge()* che ritorna l'ultimo badge ottenuto;
*- addBagde()* che permette di aggiungere un badge alla lista di badge posseduti.

**- Hotel**
La classe *Hotel* permette di creare oggetti che rappresentano gli hotels che il server gestisce, dai quali è in grado di estrapolare informazioni per rispondere alle richieste del client.
Le istanze hotel posseggono attributi che rappresentano le caratteristiche dell'hotel come ad esempio: *id, nome, città, numero di telefono, descrizione, punteggio sintetico (score) e le valutazioni*.
Inoltre, posseggono dei metodi che permettono di modificare alcuni attributi come :
*- setRate()* che permette di modificare il punteggio sintetico (score)  dell'hotel, visibile all'utente quando richiede le informazioni in merito all'hotel;
*-setRatings()* che permette di modificare le valutazioni dell'hotel;
*- getInfo()* che restituisce una stringa formattata contenente le informazioni che sono rese visibili all'utente.

**- Recensioni**
La classe *Recensioni* permette di creare oggetti recensioni che rappresentano le recensioni effettuate dall'utente, infatti, tali oggetti contengono attributi come global score (score) e le valutazioni di determinate caratteristiche dell'hotel definite dall'utente. Tali oggetti vengono poi raccolti in una lista di recensioni associata a ciascun hotel.
Le istanze di tipo Recensione posseggono un attributo di tipo *Date* che rappresenta la data con l'ora in cui è stata effettuata la recensione.

**- Ratings**
Questa classe rappresenta gli oggetti ratings, ovvero, le valutazioni associtate alle caratteristiche degli hotels o alle recensioni come *pulizia, posizione, servizi e qualità*.
Tali istanze posseggono dei metodi che permettono di visualizzare e modificare tali attrubuti.

**- Coppia**
La classe coppia è una classe di supporto che mi ha permesso di organizzare gli hotel e le  recensioni in un'unica struttura dati.
Tale classe rappresenta l'associazione tra l'hotel e la sua lista di recensioni, infatti, ad ogni città è associta una lista di tipo Coppia, dove ogni instanza di Coppia rappresenta un hotel e le sue recensioni.
Questo mi ha permesso di gestire facilmente il calcolo del punteggio degli hotels validi per il ranking locale mediante la definizione di un metodo *getScore()* che calcola  il punteggio dell' hotel basandosi sulla sua lista di recensioni utilizzando l'algoritmo definito nel punto 1 di questa sezione.
Le instanze coppia posseggono un altro metodo *make_avg()* che viene eseguito ogni qualvolta che un thread del server aggiunge una recensione alla lista di recensioni. Ciò, serve per effettuare la media dello score e delle valutazioni estrapolate dalle recensioni per poi sostituirle a quelle dell'hotel che sono rese visibili agli utenti.


## STRUTTURE DATI UTILIZZATE
Il server è l'unico ad usare strutture dati, in quanto si occupa della gestione di tutte le richieste effettuate dal client, in particolare nel progetto sono state definite: 

**- hashmap :** 
è una ConcurrentHashMap con tipo **<String,List<Coppia\>>**, nel quale la chiave è la stringa che rappresenta il nome delle città e il valore è una **Lista di coppie** dove ogni **Coppia** è un oggetto che ha tipo **<Hotel,List<Recensioni\>>**, quindi, ad ogni hotel che è presente nella città, definita come chiave della hashmap, corrisponde la rispettiva lista di recensioni. 
Questa scelta mi ha permesso di rendere molto facile l'aggiormaneto dei rank locali, il funzionamento dell'aggiornamento è spiegato nelle *scelte implementative*.
Tale scelta, quella di gestire sia gli hotels che le recensioni in una hashmap concorrente, mi ha permesso di risolvere tutti i problemi dovuti alla sincronizzazione non utlizzando esplicitamente nessun meccanismo di sincronizzazione. È bastato gestire le scritture nell'hashmap usando metodi atomici come la **compute()** e **putIfAbsent()**.

**- utenti :** 
è una ConcurrenthashMap con tipo **<String,User\>>**; tale struttura contiene tutte le informazioni in merito agli utenti registrati.
Le chiavi di questa hashmap sono gli **username** con cui gli utenti si sono registrati.
Ho deciso di fare ciò in quanto nel documento del progetto era speficato che gli username dovessero essere univoci e cosi ho in automatico gestito l'impossibilità di avere più utenti con lo  stesso username sfruttando le proprietà della struttura dati.
Il valore associato alle chiavi è l'oggetto **User** che rappresenta l'utente, nel quale sono contenute le informazioni come password, numero di recensioni effettuate e i badges da lui ottenuti.

**-topHotels :**
 è una ConcurrentHashMap con tipo **<String,String>**; essa è una struttura dati di  supporto utilizzata per mantenere l'informazione del  miglior hotel per ogni città , le chiavi sono i nomi delle città e i valori sono delle stringhe che rappresentano i nomi degli hotel che risultano avere lo score migliore tra gli hotel presenti in tale città.

**-Coda_notifiche:** 
è una *LinkedBlockingQueue* di strighe condivisa tra il thread main lato client e il thread che si occupa di ricevere le notifiche inviate dal server per informare l'utente del cambiamento dei rank locali.
Ho deciso di utilizzare questa struttura dati in modo da gestire in maniera ordinata la stampa delle notifiche con le stampe dovute dal **cli()**, metodo che si occupa di gestire l'interazione con l'utente.

## SCHEMA GENERALE DEI THREAD UTILIZZATI
### LATO CLIENT :
Nel lato client sono presenti il MainClient, un terminationHandlerClient ed un Clientmulticast:

**- MainClient:**
 Esso lancia il **readConfig()**, metodo che permette di effettuare la lettura dal file **client_properies** in modo da inizializzare tutte le variabili per la configurazione. 
Successivamente entra nel while e inzia a gestire l'interazione con l'utente usando il metodo **cli()**  ed oltre, questo effettua il cotrollo sulla coda di notifiche e se sono presenti notifiche le mostra all'utente, stampandole a video altrimenti riesegue il while.

**- TerminationHandlerClient:**
Tale thread viene eseguito quando si verifica un interruzione che può essere provocata da **System.exit\(1)** oppure dalla digitazione di **"ctrl+c"**. Esso si occupa di far terminare correttamente il programma, in particolare, nel caso in cui l'utente è loggato, provvederà ad effettuare la chiusura della socket attraverso la quale il client mantiene una connessione con il server ed effettua anche la chiusura del  **PrintWriter  outTCP** (tramite il quale il client scrive nella socket) e dello **Scanner  inTCP** (tramite il quale il client legge dalla socket).
 
**- Clientmulticast:**
Tale thread viene laciato quando l'utente effettua il login, esso, si occupa di ricevere le notifiche inviate dal server in merito agli aggiornamenti sui rank locali. Ciò avviene mediante la ricezione di pacchetti UDP inviati dal server su un gruppo multicast, infatti, non appena viene eseguito tale thread, quello che fa è creare un **MulticastSocket** e unire tale socket al gruppo mutlicast. Il tutto avviene mediante il metodo **joinGroup()** che prende come input **InetAddress** ovvero, l'indirizzo IPv4 del gruppo multicast definito come valore di configurazione e l'interfaccia di rete **NetworkInterface**.
In seguito il Clientmulticast definisce un **DatagramPacket** che rappresenterà il pacchetto ricevuto dal quale estrapolerà le informazioni.
La recezione del pacchetto è definita all'interno di un ciclo while: 
```java
try{
	receiverSocket.receive(receiver);
	}catch(SocketTimeoutException  e){
	continue;
	}
```
La ricezione è racchiusa in un blocco try-catch perchè è stato definito un *time-out* sul tempo di attesa dedicato alla ricezione del pacchetto, in modo tale da non mantenere sempre il thread in attesa del pacchetto e quindi di poter terminare mediante il cambio di stato della condizione del while **multicast_end**, che avviene quando l'utente effettua il logout, exit oppure "ctrl+c".


### LATO SERVER :
Nel lato server è presente il thread main, un TerminationHandlerServer, un thread Salvataggio, un thread Multicasthandler ed il thread Runner.

**- MainServer :** 
Il MainServer si occupa di inizializzare le strutture dati a lui utili per poter rispondere alle esigenze del client mediante il metodo **inizializzazione()**. Prima di quest'ultimo esegue il metodo **readConfig()** per poter inizializzare tutte le variabili con dei valori di configurazione definiti nel file **server_properties**.
Il MainServer inizializza una **serverSocket** fondamentale per l'instaurazione di connessioni TCP con i vari client che poi vengono gestite dai thread **Runner** generati dal **ThreadPoolExecutor gestore**.
Infine, inzializza una **MulticastSocket** attraverso la quale verranno inviati i pacchetti UDP conteneti gli aggiornamenti sui ranck locali. 

**- TerminationHandlerServer :**
È il thread che si occupa di effettuare una corretta terminazione del server successivamente alla digitazione del "ctrl+c".
Prima di terminare effettua:
1) la chiusura delle socket se esse ancora non sono state chiuse;
2) modifica lo status di tutti gli utenti a "false" in modo che non risultino loggati anche quando il server non è attivo;
3) effettua il salvataggio delle strutture dati;
4) fa terminare il programma.

**- Salvataggio :**
È un thread che viene schedulato da **ScheduledExecutorService  schedulatore** mediante il metodo **scheduleWithFixedDelay** per effettuare periodicamente il salvataggio delle strutture dati in memoria. Tutti i parametri inerenti gli intervalli di tempo che regolano l'esecuzione periodica del thread, passati al metodo scheduleWithFixedDelay, sono definiti nel file *server_properties*.

**- MulticastHandler :**
Come il thread Salvataggio anche esso viene schedultato da **ScheduledExecutorService  schedulatore** mediante il metodo **scheduleWithFixedDelay** per effettuare periodicamente il controllo dei rank locali e di notificare gli utenti loggati nel caso di cambianti.
Il controllo avviente calcolando lo **score** degli hotels in base alle recenzioni ad essi associate e mediante questo **score** viene riodinata in modo decrescente la lista degli hotels associta ad ogni città. Successivamente viene confrontato il nome dell'hotel primo in lista con il nome dell'hotel presente nella struttura dati **topHotels** associato alla città presa in esame, se questi differiscono, il MulticastHandler aggiornerà la struttura topHotels e provvederà ad inviare un pacchetto UDP al gruppo multicast contenente la città e il nome del nuovo hotel che è il primo in classifica.

**- Runner :**
Questo thread si occupa dell'interazione con il client, fornisce tutti i servizi richiesti dal client come la **registrazione, il login, la ricerca di un hotel specifico oppure di tutti gli hotel presenti in una città e riceve le recenzioni degli utenti**. 
Quando riceve una recensione il thread si occupa anche di inserirla nella struttura dati **hashmap** e di aggiornare tutte le valutazioni inerenti all'hotel recensito.
I thread Runner sono gestiti dal **gestore** che, come detto prima, è un **ThreadPool** che in particolare dedica un thread ad ogni richiesta che un client effettua. Ciò comporta che ogni thread Runner ha vita breve in quanto offre il servizio richiesto dal client per poi terminare ad eccezion fatta se l'utente effettua il login. In questo caso viene definita una connessione persistente e quindi viene dedicato un thread Runner a tale client per tutta durata della connessione, ovvero, fin quando l'utente non effettua il logout.
Tutti i parametri con cui è stato definito il **ThreadPoolExecutor gestore** sono definiti nel file server_properties. 






