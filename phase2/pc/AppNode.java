import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.FileInputStream;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.project_ds_2021.AppNodeInfo;
import com.example.project_ds_2021.BrokerInfo;
import com.example.project_ds_2021.ChannelName;
import com.example.project_ds_2021.Topic;
import com.example.project_ds_2021.VideoFile;


/*

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp4.MP4Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
*/



public class AppNode implements Node {

    private ArrayList<String> brokerHashCodes = new ArrayList<>(); //η λίστα με τα hashes των Brokers
    private static HashMap<String, BrokerInfo> brokerHashCodesMap = new HashMap<>(); //map με key ένα hash και value τον αντίστοιχο Broker

    private String subscribedTo; //το topic στο οποίο έχει γίνει το subscription
    private boolean ifAlreadySubscribed = false; //το αν έχει γίνει ήδη subscription ή όχι
    private HashMap<String, ArrayList<String>> videosOfThisTopic = new HashMap<>(); //map με key ένα topic και value μία λίστα με τα video που έχω λάβει για αυτό το topic

    private ChannelName channelName; //το κανάλι
    private String filepath; //το directory
    private BrokerInfo brokerResponsible; //ο Broker που είναι υπέυθυνος γι'αυτό το AppNode
    private AppNodeInfo appNodeInfo; //oι πληροφορίες για το AppNode

    private int port;
    private boolean running = true; //το αν τρέχει ή αν έχει γίνει disconnect απ'το χρήστη
    private boolean publisherMode = true; //το αν είναι σε publisher mode ή σε consumer mode

    private ServerSocket serverSocket;
    private Socket connection, serverConnection; //η χρήση του κάθε Socket εξηγείται παρακάτω
    private ObjectOutputStream out, serverOut;
    private ObjectInputStream in, serverIn;

    public List<BrokerInfo> brokers = new ArrayList<>();
    String initIp;
    int initPort;


    private Scanner sc = new Scanner(System.in); //scanner

    public AppNode(int i, String initIp , String initPort) {

        this.initIp = initIp;  //ip και port για την πρώτη τυχαία σύνδεση
        this.initPort = Integer.parseInt(initPort);

        filepath = System.getProperty("user.dir") + "\\src\\main\\java\\AppNode"+i+"Videos"; //αρχικοποιεί το directory ανάλογα με το input
        port = i*1000 + 1500; //παίρνει τυχαία ένα port ανάλογα με το input (εδώ 2500 ή 3500)

        System.out.println("Type your Channel Name: ");
        channelName = new ChannelName(sc.next()); //παίρνει το όνομα του καναλιού

        init(-1);
    }

    public void init(int i) {
        firstConnection(initIp , initPort);
        hashTheBrokers(); //παίρνει τα στοιχεία των Brokers ώστε να τα hashάρει
        brokerResponsible = hashTopic(channelName.getChannelName()); //βρίσκει τον υπέυθυνο Broker, κάνοντας hash στο channel name, ώστε να κάνει connection κατ'ευθείαν σε αυτόν
        System.out.println("Broker responsible for this AppNode is:\t" + brokerResponsible.getBrokerName());

        try {
        	
			appNodeInfo = new AppNodeInfo(getChannelName(), brokerResponsible, port, InetAddress.getLocalHost().getHostAddress());
			connect(); //κάνει τη σύνδεση με τον πρώτο Broker
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //φτιάχνει ένα αντικείμενο AppNodeInfo που κρατά τις πληροφορίες για τον τρέχοντα AppNode
       
    }


    public void firstConnection(String ip , int port){ // πρώτη τυχαία σύνδεση για την λήψη της λίστας με τους διαθέσιμους Brokers

        Socket requestSocket = null;
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;


        try {
            requestSocket = new Socket(ip, port);
            objectOutputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(requestSocket.getInputStream());


            objectOutputStream.writeInt(4);
            objectOutputStream.flush();

            brokers = (ArrayList<BrokerInfo>)objectInputStream.readObject();


        } catch (
                UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException | ClassNotFoundException ioException) {
            ioException.printStackTrace();
        } finally {
            try{

                objectOutputStream.close();
                objectInputStream.close();
                requestSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }

    private void hashTheBrokers(){

        for(BrokerInfo brokerInfo: brokers){
            brokerHashCodes.add(hash(brokerInfo.getIp() + brokerInfo.getPort()));
            brokerHashCodesMap.put(hash(brokerInfo.getIp() + brokerInfo.getPort()), brokerInfo);

        }

        Collections.sort(brokerHashCodes); //τα κάνει sort

    }

    private BrokerInfo hashTopic(String topic){

        for(int i=0; i<brokerHashCodes.size(); i++){

            if(hash(topic).compareTo(brokerHashCodes.get(i)) < 0){ //κάνει το compare, κι αν το hash του channel name είναι μικρότερο απ'το hash του Broker, επιστρέφει τον υπεύθυνο Broker
              return brokerHashCodesMap.get(brokerHashCodes.get(i));
            }

        }
        return brokerHashCodesMap.get(brokerHashCodes.get(0)); //αλλιώς επιστρέφει τον Broker με το μικρότερο hash
    }

    private String hash(String input){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashText = no.toString(10);


            return hashText;
        } catch(NoSuchAlgorithmException e){
            throw new RuntimeException();
        }
    }

    public void connect(){

            try {

                connection = new Socket(brokerResponsible.getIp(), brokerResponsible.getPort()); //κάνει τη σύνδεση στον κατάλληλο Broker
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());

                out.writeInt(0); //στέλνει flag=0 για να του πει ότι δέχεται σύνδεση απο AppNode
                out.flush();

                out.writeObject(appNodeInfo); //στέλνει τα στοιχεία του στον Broker
                out.flush();

            } catch (IOException exc) {
                exc.printStackTrace();
            }

            Thread t1 = new Thread(new Runnable(){
                public void run(){
                    try {
                        publisher();  //φτιάχνει ένα νήμα για το publisher mode
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            });

            Thread t2 = new Thread(new Runnable(){ // Set Listener
                public void run(){
                    try {
                        consumer(); //φτιάχνει ένα νήμα για το consumer mode
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            });

            Thread t3 = new Thread(new Runnable(){ // Set Listener
                public void run(){
                    try { //
                         server(); //φτιάχνει ένα νήμα για το server mode
                    }catch(IOException e){
                        e.printStackTrace();
                    }catch(ClassNotFoundException e){
                        e.printStackTrace();
                    }
                 }
            });

            t1.start();
            t2.start(); //ξεκινά τα νήματα
            t3.start();

    }

    private void publisher() throws InterruptedException {

        String message, hashtag;
        int answer, initialAnswer = 0;

        synchronized (this) {

            while(running) {

                while(!publisherMode){
                    wait(); //όσο δεν είσαι σε publisher mode, περίμενε
                }

                do {
                    try {
                        message = (String) in.readObject();
                        System.out.println(message);

                        initialAnswer = sc.nextInt(); //δώσε την αρχική απάντηση

                        out.writeObject(String.valueOf(initialAnswer));
                        out.flush();

                        if (initialAnswer == 1) { //θες να κανεις upload

                            message = (String) in.readObject();
                            System.out.println(message);

                            File file = new File(filepath+"\\toProduce"); //directory για τα video προς δημοσίευση

                            String[] availableVideos;
                            availableVideos = file.list(); //λίστα των διαθέσιμων video

                            for (int i = 0; i < availableVideos.length; i++) {
                                System.out.println((i + 1) + ". " + availableVideos[i]);
                            }

                            System.out.println("\nChoose a video to upload ");

                            int videoToUpload = sc.nextInt(); //διάλεξε ποιο video θες να κάνεις upload
                            String vName = availableVideos[videoToUpload - 1]; //κράτα το όνομα του video
                            channelName.getPublishedVideos().add(vName); //και βαλ'το στη λίστα

                            System.out.println("\nType a hashtag: ");

                            hashtag = sc.next(); //πρώτο hashtag

                            if (!channelName.getPublishedHashTags().contains(hashtag)) { //αν δεν υπάρχει
                                channelName.getPublishedHashTags().add(hashtag); //βαλ'το στη λίστα
                            }

                            channelName.getPublishedHashtagVideosMap().putIfAbsent(hashtag, new ArrayList<>());
                            channelName.getPublishedHashtagVideosMap().get(hashtag).add(vName); //και στο map

                            out.writeObject(hashtag); //στείλε το hashtag
                            out.flush();

                            out.writeObject(vName); //και τ'όνομα του video
                            out.flush();

                            BrokerInfo b = hashTopic(hashtag); //επιστρέφει ποιος broker είναι ο υπεύθυνος γι'αυτό το hashtag

                            out.writeObject(b);
                            out.flush();

                            do { //για την προσθήκη κι άλλων hashtag
                                message = (String) in.readObject();
                                System.out.println(message);
                                answer = sc.nextInt();
                                out.writeObject(String.valueOf(answer));
                                out.flush();

                                if (answer == 1) {
                                    message = (String) in.readObject();
                                    System.out.println(message);
                                    hashtag = sc.next();

                                    if (!channelName.getPublishedHashTags().contains(hashtag)) {
                                        channelName.getPublishedHashTags().add(hashtag);
                                    }

                                    channelName.getPublishedHashtagVideosMap().putIfAbsent(hashtag, new ArrayList<>());
                                    channelName.getPublishedHashtagVideosMap().get(hashtag).add(vName);

                                    out.writeObject(hashtag);
                                    out.flush();

                                    b = hashTopic(hashtag);

                                    out.writeObject(b);
                                    out.flush();
                                }

                            } while (answer == 1); //όσο η απάντηση είναι 1 προσθέτουμε hashtags

                        }

                    } catch (ClassNotFoundException exc) {
                        exc.printStackTrace();
                    } catch (IOException exc) {
                        exc.printStackTrace();
                    }

                } while (initialAnswer == 1); //όσο η αρχική απάντηση είναι 1, κάνουμε upload νεα video

                if (initialAnswer == 2) { //αν η αρχική απάντηση είναι 2,
                    publisherMode= false; //άλλαξε σε consumer mode
                    notifyAll(); //και κάνε notify το thread που περιμένει
                }

                if (initialAnswer == 3) { //αν η αρχική απάντηση είναι 3,
                    running = false; //το AppNode σταματάει να τρέχει
                    disconnect(); //και γίνεται disconnect
                }
            }
        }
    }

    private void consumer() throws InterruptedException{

        String message;
        Topic topic;
        CopyOnWriteArrayList<Topic> topicsList;
        int initialAnswer=0;

        synchronized (this) {

            while (running) {

                while(publisherMode){
                    wait(); //όσο δεν είσαι σε publisher mode, περίμενε
                }

                do {

                    try {
/*
                        if(ifAlreadySubscribed){
                            out.writeUnshared(subscribedTo); //ενημέρωσε το topic που έχεις κάνει εγγραφή
                            out.flush();

                            BrokerInfo b = (BrokerInfo) in.readObject(); //λάβε τον υπέυθυνο Broker

                            ArrayList<String> vNames = (ArrayList<String>) in.readObject(); //λάβε τη λίστα με τα video

                            for(String vName: vNames){ //για κάθε video της λίστας
                                if(videosOfThisTopic.containsKey(subscribedTo)) {
                                    if (!videosOfThisTopic.get(subscribedTo).contains(vName)) {
                                        pull(vName, b); //κάνε το pull αν δεν το έχεις
                                        videosOfThisTopic.get(subscribedTo).add(vName);
                                    }
                                }
                            }
                        }*/

                        message = (String) in.readObject();
                        System.out.println(message);

                        initialAnswer = sc.nextInt(); //αρχική απάντηση για το αν θέλω να κάνω subscribe, ν'αλλάξω σε publisher ή να κλείσω

                        out.writeObject(String.valueOf(initialAnswer));
                        out.flush();

                        if (initialAnswer == 1) { //αν θέλω να κάνω subscribe

                            message = (String) in.readObject();
                            System.out.println(message);

                            topicsList = (CopyOnWriteArrayList<Topic>) in.readObject(); //λίστα με τα υπάρχοντα topics
                            CopyOnWriteArrayList<Topic> temp = new CopyOnWriteArrayList<>(); //προσωρινή λίστα για να γίνει φιλτράρισμα
                                                                                            //και να μην παρουσιαστεί το channel name μου
                            for (int i = 0; i < topicsList.size(); i++) {
                                if (!topicsList.get(i).getName().equals(channelName.getChannelName())) {
                                    temp.add(topicsList.get(i));
                                }
                            }

                            for (int i = 0; i < temp.size(); i++) {
                                System.out.println((i + 1) + ". Topic Name: " + temp.get(i).getName() + "\t Type: " + temp.get(i).getType());
                            }

                            System.out.println("\nChoose a topic to subscribe to: ");
                            topic = temp.get(sc.nextInt() - 1); //επιλέγω topic

                            subscribedTo = topic.getName(); //και κάνω subscribe
                            ifAlreadySubscribed = true;

                            out.writeObject(topic);
                            out.flush();

                            BrokerInfo b = (BrokerInfo) in.readObject(); //Broker υπέυθυνος για το topic που επέλεξα
                            
                            ArrayList<String> vNames = (ArrayList<String>) in.readObject(); //λάβε τη λίστα με τα video

                            pull(topic, b); //pull στα video αυτού του topic

                        }
                    } catch (ClassNotFoundException exc) {
                        exc.printStackTrace();
                    } catch (IOException exc) {
                        exc.printStackTrace();
                    }


                } while (initialAnswer == 1);

                if (initialAnswer == 2) {
                    publisherMode = true;
                    notifyAll();
                }

                if (initialAnswer == 3) {
                    running = false;
                    disconnect();
                }
            }
        }
    }


    private void server() throws IOException, ClassNotFoundException{

    	serverSocket = new ServerSocket(16230); //περιμένω να λάβω αιτήματα για να κάνω push

        while(running) {

        		serverConnection = serverSocket.accept();
        		serverOut = new ObjectOutputStream(serverConnection.getOutputStream());
        		serverIn = new ObjectInputStream(serverConnection.getInputStream());

                int flag = serverIn.readInt(); //flag για το αν πρέπει να στείλω πολλά, ήδη υπάρχοντα, ή ένα νέο
                if (flag == 0) { //αν flag=1, κάνω push τουλάχιστον 1

                    Topic t = (Topic) serverIn.readObject(); //το topic που μου ζητείται

                    if (t.getType().equals("Channel")) { //αν είναι το κανάλι μου

                        serverOut.writeInt(channelName.getPublishedVideos().size()); //στέλνω το πλήθος των video που 'χω κάνει upload
                        serverOut.flush();

                        for (String vName : channelName.getPublishedVideos()) { //κάνω push κάθε video της λίστας
                            push(vName);
                        }

                    } else if (t.getType().equals("Hashtag")) { //αν το topic είναι ένα hashtag

                        ArrayList<String> videosWithThisHashtag = channelName.getPublishedHashtagVideosMap().get(t.getName());
                        serverOut.writeInt(videosWithThisHashtag.size()); //στέλνω το πλήθος των video που 'χω κάνει upload με αυτό το hashtag
                        serverOut.flush();

                        for (String vName : videosWithThisHashtag) { //κάνω push κάθε video της λίστας
                            push(vName);
                        }
                    }

                serverConnection.close(); //κλείνω τη σύνδεση
                serverOut.close();
                serverIn.close();

                } else if (flag == 1) { //αν flag=1, κάνω push μόνο ένα

                    String vName = (String) serverIn.readObject(); //τ'όνομα του video
                    push(vName);

                    serverConnection.close(); //κλείνω τη σύνδεση
                    serverOut.close();
                    serverIn.close();
                }

        }
    }

    
    private void push(String vName){

        try {
            ArrayList<VideoFile> videoChunks = generateChunks(vName); //τα generated chunks μπαίνουν σε μία λίστα από VideoFiles

            serverOut.writeInt(videoChunks.size()); //στείλε τον αριθμό των chunks
            serverOut.flush();

            serverOut.writeUTF(vName); //στείλε το όνομα του video
            serverOut.flush();

            for (VideoFile chunks : videoChunks) {
                serverOut.writeObject(chunks); //στείλε τα chunks ένα προς ένα
                serverOut.flush();
            }

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private ArrayList<VideoFile> generateChunks(String vName) throws IOException {

        int chunk = 512 * 1024; //μέγεθος του chunk se bytes

        FileInputStream inputVideo = new FileInputStream(filepath+"\\toProduce\\"+vName); //directory για τα video προς δημοσίευση
/*
        Metadata metadata = new Metadata();
        ParseContext pcontext = new ParseContext();
        BodyContentHandler handler = new BodyContentHandler();
        MP4Parser MP4Parser = new MP4Parser();
        try {
            MP4Parser.parse(inputVideo, handler, metadata,pcontext);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (TikaException e) {
            e.printStackTrace();
        }

        String[] metadataNames = metadata.names();

        String dateCreated = metadata.get("Creation-Date");
        String frameHeight = metadata.get("tiff:ImageLength");
        String frameWidth = metadata.get("tiff:ImageWidth");
        String length = metadata.get("xmpDM:duration");

        inputVideo.close();


        inputVideo = new FileInputStream(filepath+"\\toProduce\\"+vName);*/

        ArrayList<byte[]> chunkList = new ArrayList<>(); //λίστα των chunks

        int i = 0;
        while (inputVideo.available() != 0) {
            byte[] c = new byte[chunk];
            inputVideo.read(c, i*chunk ,chunk); //διάβαζει τόσα bytes και τα βάζει στη λίστα των chunks
            chunkList.add(c);

        }

        ArrayList<VideoFile> videoFileList = new ArrayList<>(); //λίστα των VideoFiles

        for (i = 0; i < chunkList.size(); i++) {
           // VideoFile videofile = new VideoFile((i + 1) + "_" + vName,dateCreated,length,null,frameWidth,frameHeight,chunkList.get(i)); //μετατρέπει τα chunks σε VideoFiles
            VideoFile videofile = new VideoFile((i + 1) + "_" + vName, chunkList.get(i));
            videoFileList.add(videofile); //αριθμώντας τα, ώστε όταν φτάσουν να γίνει sorting
        }

        inputVideo.close();
        return videoFileList;

    }

    
    private void pull(Topic topic, BrokerInfo b) throws IOException, ClassNotFoundException { //pull video που έχουν ήδη ανέβει	
    	
        Socket consumerConnection = new Socket(b.getIp(), b.getPort()+1); //νέα σύνδεση, ως consumer, στον Broker που
        ObjectOutputStream consumerOut = new ObjectOutputStream(consumerConnection.getOutputStream()); //είναι υπέθυνος για το topic που έχουμε
        ObjectInputStream consumerIn = new ObjectInputStream(consumerConnection.getInputStream()); //κάνει εγγραφή

        consumerOut.writeInt(0); //στέλνω flag=0 για να ενημερώσω ότι θέλω να λάβω υπάρχοντα video
        consumerOut.flush();

        consumerOut.writeObject(topic); //στέλνω το topic στο οποίο κάνω εγγραφή
        consumerOut.flush();

        consumerOut.writeObject(appNodeInfo); //στέλνω τα στοιχεία μου ως AppNode
        consumerOut.flush();

        int numOfPubs = consumerIn.readInt(); //το πλήθος των publishers που θα κάνουν push για το topic που ζήτησα

        for (int i = 0; i < numOfPubs; i++) {

            boolean sameAppNode = consumerIn.readBoolean();

            if (!sameAppNode) { //φιλτράρισμα  για να μη λάβω video που έχω ανεβάσει ο ίδιος

                int numOfVids = consumerIn.readInt(); //το πλήθος των video που θα λάβω από τον κάθε publisher

                for (int j = 0; j < numOfVids; j++) {

                    int numOfChunks = consumerIn.readInt(); //το πλήθος των chunks που θα λάβω για κάθε video
                    String vName = consumerIn.readUTF(); //το όνομα του κάθε video

                    videosOfThisTopic.putIfAbsent(topic.getName(), new ArrayList<>()); //μπαίνει το topic sto map
                    videosOfThisTopic.get(topic.getName()).add(vName); //και το video name στη λίστα του

                    ArrayList<VideoFile> chunksReceived = new ArrayList<>(); //

                    for (int k = 0; k < numOfChunks; k++) {
                        VideoFile videofile = (VideoFile) consumerIn.readObject(); //βάζω τα VideoFiles που παίρνω στη λίστα
                        chunksReceived.add(videofile);
                    }

                    chunksReceived.sort(new VideoFileSorter()); //κάνω τη λίστα sort βάσει της αρίθμησης που έχει γίνει
                    playData(vName, chunksReceived); //γράφω τα chunks στο directory
                }
            }
        }
    }

    private void pull(String vName, BrokerInfo b) throws IOException, ClassNotFoundException{ //pull video που ανέβηκαν μεταγενέστερα    	
        Socket consumerConnection = new Socket(b.getIp(), b.getPort()+1); //νέα σύνδεση, ως consumer, στον Broker που
        System.out.println(b.getPort()+1);
        ObjectOutputStream consumerOut = new ObjectOutputStream(consumerConnection.getOutputStream()); //είναι υπέθυνος για το topic που έχουμε
        ObjectInputStream consumerIn = new ObjectInputStream(consumerConnection.getInputStream()); //κάνει εγγραφή

        consumerOut.writeInt(1); //στέλνω flag=1 για να ενημερώσω ότι θέλω να λάβω νέο video
        consumerOut.flush();

        consumerOut.writeObject(vName); //στέλνω τ'όνομα του video που θέλω να λάβω
        consumerOut.flush();

        int numOfChunks = consumerIn.readInt(); //λαμβάνω τον αριθμό των chunks
        ArrayList<VideoFile> chunksReceived = new ArrayList<>();

        for (int k = 0; k < numOfChunks; k++) {
            VideoFile videofile = (VideoFile) consumerIn.readObject(); //βάζω τα VideoFiles που παίρνω στη λίστα
            chunksReceived.add(videofile);
        }

        chunksReceived.sort(new VideoFileSorter()); //κάνω τη λίστα sort βάσει της αρίθμησης που έχει γίνει
        playData(vName, chunksReceived); //γράφω τα chunks στο directory
    }

    private void playData(String vName, ArrayList<VideoFile> chunksReceived){

        try {
            FileOutputStream outputVideo = new FileOutputStream(filepath+"\\toConsume"+"\\"+vName ); //το directory που θα γραφτεί το video

            for(VideoFile v : chunksReceived){
                outputVideo.write(v.getVideoFileChunk()); //γράφω το video
            }

            outputVideo.close();
            System.out.println("\nVideo "+vName+" added to our file directory! ");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getChannelName(){
        return channelName.getChannelName();
    }

    public void disconnect(){

            System.out.println("\nConnection with Server was terminated\nGoodbye!!!");

            try{
                out.close(); //κλείνει η σύνδεση
                in.close();
                connection.close();
            }catch(IOException exc){
                exc.printStackTrace();
            }
    }

    public void updateNodes(){}
    public List<BrokerInfo> getBrokers(){return null;}

}
