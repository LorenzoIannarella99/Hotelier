# HOTELIER
## INFO
**JDK**

**The project was developed with JDK 19. In terms of how the code is written, there are no major differences compared to JDK 8. The only difference is in the multicast group join, where the `joinGroup` method now takes both an "InetAddress" and a "NetworkInterface" as input, i.e., the network interface.**

**Ubuntu**  
The project was developed on an Ubuntu 22.04.4 LTS system. In order for the client to receive UDP packets sent by a server thread on the multicast group, I had to add a rule to the Ubuntu firewall to allow UDP packets on port 4000. This is done by using the following command:
```bash
sudo ufw allow 4000/udp
```

**External Libraries**  
For educational purposes, I chose to use Google’s Gson library. Specifically, I used **gson-2.10.1**, which enables **serialization** and **deserialization** of Java objects, a feature used to persist or retrieve the data structures that the server uses to manage all information.

## INSTRUCTIONS  
The code is divided into two folders: one contains all the classes that ensure the server functions correctly, and the other contains all the classes needed for the client to work properly.  
### 1) Compilation:  
The compilation, both for the server and client code, is done by executing the same terminal command:
```bash
javac -cp gson-2.10.1.jar:. *.java
```
Essentially, this compiles the code while considering the Gson library, which is loaded dynamically through the `-cp` parameter. This parameter tells the Java compiler where to look for classes and packages. In this case, the first is `gson-2.10.1.jar`, followed by a `:` separator, and then `.` to indicate the current directory. The `*.java` specifies that all Java files in the current directory should be compiled.

### 2) Execution  
Execution can occur in two different ways: The first is by running the following commands from the terminal:  
```bash
java -cp gson-2.10.1.jar:. MainClient
```
to start the client and  
```bash
java -cp gson-2.10.1.jar:. MainServer
```
to start the server.  
Alternatively, both the server and client can be executed via JAR files using the following commands:
```bash
java -cp Server.jar:gson-2.10.1.jar MainServer
```
to run the server and  
```bash
java -cp Client.jar:gson-2.10.1.jar MainClient
```
to run the client.

### 3) Arguments to Pass to the Code:  
No arguments are required as all the necessary variables for the proper execution of the code, both on the client and server sides, are defined in their respective configuration files.

### 4) Command Syntax:  

**On the Server Side**  
The server can be terminated by typing **"ctrl+c"**, which will trigger a proper shutdown of the `MainServer` and all other running threads, as explained in the general thread structure.

**On the Client Side**  
The client mainly interacts with the user, and it can respond to various commands. Upon starting, the client suggests typing **info** to display all the operations it can handle.  
For each operation, the client guides the user on what to input and communicates invalid entries. This approach makes the interaction with the client intuitive and easy for the user.  
In addition to the operations defined in the specifications, I have added two additional operations:  
**return** and **exit**.  
The **return** command allows the user to go back if they decide not to execute a command anymore, and this can be done at any point in the interaction.  
The **exit** command allows for an elegant termination of the program, also logging the user out if they have logged in.  
The program can also be terminated by typing **"ctrl+c"**, and in this case, if the user is logged in, they will be logged out as well.

---

## IMPLEMENTATION CHOICES
### 1) SERVER SIDE:
I chose to implement the server in such a way that it handles (via threads managed by a *ThreadPool*) all the data structures needed to maintain information about hotels, reviews, and registered users.  
A key service of Hotelier is maintaining a ranking of hotels in each city and informing logged-in users whenever the identity of the top-ranked hotel in any local ranking changes.  
The ranking is determined based on the quantity of reviews, the quality of reviews (based on the global score of each review), and the recency of the reviews.  
I decided to implement this calculation by considering the **quality** of the reviews as the sum of the global scores collected from the reviews of each hotel. The global score is influenced by a weight based on the **recency** of the reviews; if a review was made more than two months ago, its global score will be multiplied by a weight of "0.4". Otherwise, if the review was made within the past two months, the global score will be multiplied by a weight of "1".  
The resulting **quality** value is then influenced by the **quantity** of reviews. If the number of reviews is *less than 50*, the quality value will be multiplied by a weight of "0.4"; if the number of reviews is *between 50 (inclusive) and 100 (exclusive)*, the weight will be "0.6". If the number of reviews is *greater than or equal to 100*, the weight will be "0.8".  
The final result will be the value by which hotels are ranked in descending order in the lists associated with each city.

### 2) CLIENT SIDE:
I decided to implement the client so that it is responsible only for handling user interaction via a *cli()* method, which captures the operations the user wants to perform using a **scanner**.  
The client is also responsible for receiving notifications sent by the server regarding updates to the local rankings. This is done through a *ThreadSafe* queue, which then displays the updates to the user.

### 3) PERSISTENT AND NON-PERSISTENT CONNECTIONS:
The project specifications required establishing a persistent connection between the client and the server once the user logged in and defining non-persistent connections for all operations performed by users who had not logged in.  
I decided to implement this specification by handling the connections differently on both the client and server sides. The distinction is made using the boolean variable **login**.  
For each operation on the client side, the *login* variable is checked. If it is *true*, the method uses the socket opened during the login phase to communicate with the server. Otherwise, a socket is opened and closed after executing the operation requested by the user, which does not require login.  
On the server side (the code describing the behavior of the Runner threads managed by the **ThreadPoolExecutor manager**), the login variable is used as the condition for the *while* loop that prevents the thread from terminating and closing the connection. This means that the user who logged in will have a dedicated server thread. Otherwise, after providing the service, the thread terminates its execution.

### 4) CLASSES:
**- User**  
The *User* class allows for the creation of objects representing registered users.  
Instances of this class have attributes representing the user's properties, such as *username, password, number of reviews made, badges owned, and status*.  
The status attribute helps the server determine whether the user is logged in or not.  
The User class also includes methods to modify these attributes, such as:  
- *setStatus()*: modifies the user's status.  
- *getBadge()*: returns the last badge acquired.  
- *addBadge()*: adds a badge to the list of owned badges.

**- Hotel**  
The *Hotel* class allows for the creation of objects representing the hotels managed by the server, from which the server can extract information to respond to client requests.  
Hotel instances have attributes representing the hotel's characteristics, such as *id, name, city, phone number, description, score (rating), and reviews*.  
Additionally, they have methods to modify certain attributes, such as:  
- *setRate()*: modifies the hotel's overall rating (score), which is visible to the user when requesting hotel information.  
- *setRatings()*: modifies the ratings of the hotel.  
- *getInfo()*: returns a formatted string containing the information visible to the user.

**- Reviews**  
The *Review* class allows for the creation of objects representing the reviews made by users. These objects contain attributes such as global score (rating) and ratings for specific hotel features defined by the user. These objects are then stored in a list of reviews associated with each hotel.  
Instances of the Review class have an attribute of type *Date*, which represents the date and time when the review was made.

**- Ratings**  
This class represents the ratings associated with hotel features or reviews, such as *cleanliness, location, services, and quality*.  
Instances of this class have methods that allow for viewing and modifying these attributes.

**- Coppia**  
The *Coppia* class is a support class that helped me organize hotels and reviews into a single data structure.  
This class represents the association between a hotel and its list of reviews. Each city is associated with a list of Coppia objects, where each Coppia instance represents a hotel and its reviews.  
This approach allowed me to easily manage the calculation of hotel scores for local rankings by defining a method *getScore()* that calculates the hotel's score based on its list of reviews using the algorithm defined in point 1 of this section.  
Coppia instances also have another method *make_avg()*, which is executed whenever a server thread adds a review to the review list. This method computes the average score and ratings from the reviews and replaces the existing values visible to the users.

---

## DATA STRUCTURES USED  
The server is the only component that uses data structures, as it handles all requests made by the client. Specifically, in this project, the following have been defined:

**- Hashmap:**  
This is a `ConcurrentHashMap` of type **<String, List<Pair>>**, where the key is a string representing the name of the cities, and the value is a **List of Pairs**, where each **Pair** is an object of type **<Hotel, List<Review>>**. Therefore, for each hotel in a city (defined as the key of the hashmap), there corresponds a list of reviews.  
This choice made updating local rankings very easy, and the update process is explained in the *Implementation Choices* section.  
By managing both hotels and reviews in a concurrent hashmap, I was able to resolve synchronization issues without explicitly using any synchronization mechanisms. It was sufficient to handle writes to the hashmap using atomic methods like **compute()** and **putIfAbsent()**.

**- Users:**  
This is a `ConcurrentHashMap` of type **<String, User>**; this structure contains all information about registered users.  
The keys of this hashmap are the **usernames** that users registered with.  
I decided to do this because the project document specified that usernames must be unique, and this automatically handled the impossibility of having multiple users with the same username by taking advantage of the properties of this data structure.  
The value associated with each key is the **User** object representing the user, which contains information such as password, number of reviews made, and the badges they have earned.

**- TopHotels:**  
This is a `ConcurrentHashMap` of type **<String, String>**; it is a support data structure used to maintain information about the best hotel in each city. The keys are the names of the cities, and the values are strings representing the names of the hotels with the highest score in that city.

**- NotificationQueue:**  
This is a *LinkedBlockingQueue* of strings shared between the main thread on the client side and the thread that handles receiving notifications from the server to inform the user about changes in local rankings.  
I decided to use this data structure to manage the orderly display of notifications with the necessary print statements from **cli()**, the method that handles user interaction.

## GENERAL THREAD SCHEMA USED

### CLIENT SIDE:  
On the client side, there is the `MainClient`, a `TerminationHandlerClient`, and a `ClientMulticast`:

**- MainClient:**  
It starts the **readConfig()** method, which reads from the **client_properties** file to initialize all the configuration variables.  
Next, it enters the `while` loop and begins handling user interaction using the **cli()** method, which also checks the notification queue and, if there are notifications, displays them to the user. If there are no notifications, it re-executes the loop.

**- TerminationHandlerClient:**  
This thread is triggered when an interruption occurs, either from **System.exit(1)** or by pressing **"ctrl+c"**. It ensures the program terminates correctly. Specifically, if the user is logged in, it will close the socket through which the client maintains a connection with the server, as well as the **PrintWriter outTCP** (through which the client writes to the socket) and the **Scanner inTCP** (through which the client reads from the socket).

**- ClientMulticast:**  
This thread is launched when the user logs in. It is responsible for receiving notifications from the server about updates to the local rankings. This is done by receiving UDP packets sent by the server to a multicast group. Once the thread is executed, it creates a **MulticastSocket** and joins the multicast group. This is done using the **joinGroup()** method, which takes **InetAddress** (the IPv4 address of the multicast group defined in the configuration file) and **NetworkInterface** (the network interface).  
Then, `ClientMulticast` defines a **DatagramPacket** that will represent the received packet from which it will extract the information.  
The reception of the packet is handled inside a `while` loop:  
```java
try{
	receiverSocket.receive(receiver);
}catch(SocketTimeoutException  e){
	continue;
}
```  
The reception is wrapped in a try-catch block because a *timeout* has been defined for the waiting time for receiving the packet. This ensures the thread does not stay in a waiting state indefinitely and allows it to terminate when the user logs out, exits, or presses "ctrl+c".

### SERVER SIDE:  
On the server side, there is the main thread, a `TerminationHandlerServer`, a `SaveThread`, a `MulticastHandler` thread, and the `Runner` thread.

**- MainServer:**  
The `MainServer` is responsible for initializing the data structures needed to respond to client requests using the **initialize()** method. Before this, it calls **readConfig()** to initialize all variables with configuration values defined in the **server_properties** file.  
`MainServer` initializes a **serverSocket**, which is crucial for establishing TCP connections with various clients, which are then managed by **Runner** threads generated by the **ThreadPoolExecutor Manager**.  
Lastly, it initializes a **MulticastSocket**, through which UDP packets containing updates to the local rankings will be sent.

**- TerminationHandlerServer:**  
This thread ensures proper server termination when **ctrl+c** is pressed. Before terminating, it:  
1. Closes any open sockets if they haven't been closed yet.  
2. Changes the status of all users to "false" so they are not marked as logged in when the server is inactive.  
3. Saves the data structures.  
4. Ends the program.

**- SaveThread:**  
This thread is scheduled by the **ScheduledExecutorService scheduler** using the **scheduleWithFixedDelay** method to periodically save the in-memory data structures. All parameters related to the timing intervals for the thread execution are defined in the **server_properties** file.

**- MulticastHandler:**  
Like the `SaveThread`, this thread is also scheduled by the **ScheduledExecutorService scheduler** using the **scheduleWithFixedDelay** method to periodically check local rankings and notify logged-in users of changes.  
The check is performed by calculating the **score** of hotels based on the reviews associated with them. Using this **score**, the list of hotels in each city is reordered in descending order. Then, the name of the hotel at the top of the list is compared with the name of the hotel currently stored in the **topHotels** data structure for that city. If they differ, the `MulticastHandler` updates the **topHotels** structure and sends a UDP packet to the multicast group containing the city and the name of the new top-ranked hotel.

**- Runner:**  
This thread handles the interaction with the client and provides all the services requested by the client, such as **registration, login, searching for a specific hotel or all hotels in a city, and receiving user reviews**.  
When it receives a review, the thread inserts it into the **hashmap** data structure and updates all ratings related to the reviewed hotel.  
The `Runner` threads are managed by the **manager**, which is a **ThreadPool**. Specifically, each client request is handled by a dedicated thread. This means each `Runner` thread has a short lifespan, as it serves the client's request and then terminates, unless the user is logged in. In that case, a persistent connection is established, and a dedicated `Runner` thread remains active for the duration of the connection (i.e., until the user logs out).  
All parameters for the **ThreadPoolExecutor Manager** are defined in the **server_properties** file.



