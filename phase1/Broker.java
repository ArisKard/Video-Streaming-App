import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Broker extends Thread implements Node {

    private ArrayList<AppNodeInfo> registeredUsers = new ArrayList<>();
    private HashMap<String, ArrayList<AppNodeInfo>> subscribers = new HashMap<>();//de tha xreiastei
    private CopyOnWriteArrayList<Topic> topicsList = new CopyOnWriteArrayList <>(); //λίστα με τα υπάρχοντα topics
    private HashMap<String, BrokerInfo> topicMap = new HashMap<>(); //map με key ένα topic και value ένα BrokerInfo ώστε να ξέρουμε τους υπεύθυνους Brokers

    private HashMap<String, ArrayList<String>> topicVideoMap = new HashMap<>();//map με key ένα topic και value μία λίστα από video names

    private ConcurrentHashMap<String, ArrayList<AppNodeInfo>> publishedFromMap = new ConcurrentHashMap <>();//map με key ένα topic και value μία λίστα με AppNodeInfo για να ξέρουμε ποιος AppNode ανέβασε τι
    private ConcurrentHashMap<String, AppNodeInfo> videoFromMap = new ConcurrentHashMap <>();//map με key ένα video name και value ένα AppNodeInfo για να ξέρουμε ποιος AppNode ανέβασε το εκάστοτε video

    private HashMap<String, Integer> hashTagNumberMap = new HashMap<>();//map με key ένα hashtag και value τον αριθμό των video που έχουν ανεβεί με το συγκεκριμένο hashtag
    private HashMap<String, Integer> sameHashtagFromSamePublisher = new HashMap<>();//map με key ένα hashtag και value τον αριθμό των video που έχουν ανεβεί με το συγκεκριμένο hashtag από τον ΙΔΙΟ AppNode

    private int port; //το port
    private String ip; //η ip
    private String brokerName; //το όνομα του Broker
    private String filepath = System.getProperty("user.dir")+"\\src\\main\\java"; //το αρχικό directory
    private BrokerInfo brokerInfo; //κρατάει τις απαραίτητες πληροφορίες για τον συγκεκριμένο Broker

    private ServerSocket serverSocket = null;
    private Socket connection = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;


    public Broker(int i){

        readBrokersFromFile(filepath+"\\BrokerFile"); //διάβασε τα names και τα ports των Brokers απ'το BrokerFile
        init(i);
    }

    private void readBrokersFromFile(String filename){
        File file = new File(filename);
        try {
            Scanner sc = new Scanner(file);

            while(sc.hasNext()){
                brokers.add(new BrokerInfo(sc.next(),sc.next(), Integer.parseInt(sc.next()))); //φτιάξε τα BrokerInfo αντικείμενα για τους Brokers
            }

        }catch (FileNotFoundException f){
            f.printStackTrace();
        }

    }

    public void init(int i) {
        port = this.brokers.get(i).getPort(); //πάρε το port
        ip = this.brokers.get(i).getIp(); //πάρε το ip
        brokerName = this.brokers.get(i).getBrokerName(); //πάρε το όνομα
        brokerInfo = new BrokerInfo(brokerName,ip, port); //φτιάξε το BrokerInfo

        connect(); //ξεκίνα να ακούς για connections
    }

    @Override
    public void connect(){

        Thread t1,t2,t3; //τα threads

        System.out.println("\n" + getBrokerName() + " waiting for connections...\n");

        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                try {

                    connection = serverSocket.accept(); //δέχεται αιτήματα για connections
                    out = new ObjectOutputStream(connection.getOutputStream());
                    in = new ObjectInputStream(connection.getInputStream());

                    int flag = in.readInt(); //το flag καθορίζει το τι τύπου connection έχουμε
                    if(flag==0) { //αν είναι 0, έχουμε σύνδεση μ'ένα AppNode για publish / consume

                        AppNodeInfo app = null;

                        try {

                            app = (AppNodeInfo) in.readObject(); //παίρνει τα στοιχεία του AppNode
                            System.out.println("\nChannel named " + app.getChannelName()+" connected... ");

                            acceptConnection(app); //καταχωρεί τον AppNode

                        }catch(IOException exc){
                            exc.printStackTrace();
                        }catch(ClassNotFoundException exc){
                            exc.printStackTrace();
                        }

                        ClientHandler clientHandler = new ClientHandler(app, connection, this, in, out); //δημιουργεί ένα Handler
                                                                        // που θα χειρίζεται τις ανάγκες της επικοινωνίας Broker-AppNode


                        t1 = new Thread(new Runnable(){
                            public void run(){
                                try{
                                    clientHandler.publisher(); //αυτό το thread θα τρέχει την επικοινωνία με την
                                } catch(InterruptedException e){ //publisher πλευρά του AppNode
                                    e.printStackTrace();
                                }
                            }
                        });

                        t2 = new Thread(new Runnable(){
                            public void run(){
                                try{
                                    clientHandler.consumer(); //αυτό το thread θα τρέχει την επικοινωνία με την
                                } catch(InterruptedException e){ //consumer πλευρά του AppNode
                                    e.printStackTrace();
                                }
                            }
                        });

                        t1.start(); //εκκίνηση των threads
                        t2.start();


                    } else if (flag==1) { //αν είναι 1, έχουμε σύνδεση μ'έναν άλλον Broker για ενημέρωση

                        try {

                            System.out.println(in.readUTF());

                            Message m = (Message) in.readObject(); //παίρνει ένα μήνυμα ώστε να κάνει τα updates
                            updateStructures(m); //κάνει τα updates

                            System.out.println(in.readUTF());

                        } catch (ClassNotFoundException classNotFoundException) {
                            classNotFoundException.printStackTrace();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }

                    }else if (flag==2){ //αν είναι 2, έχουμε σύνδεση μ'εναν AppNode / consumer για να λάβει video(s)

                        ClientHandler clientHandler = new ClientHandler(null, connection,this, in, out);
                        t3 = new Thread(new Runnable(){
                            public void run(){
                                try{
                                    clientHandler.server(); //νεό thread για τη server πλευρά του AppNode που θα κάνει push
                                } catch(IOException e){
                                    e.printStackTrace();
                                }
                            }
                        });

                        t3.start();


                    }else if (flag==3){ //αν είναι 3, έχουμε σύνδεση μ'έναν άλλον Broker για την ενημέρωση
                                        //συγκεκριμένων δομών
                        try {

                            Message m = (Message) in.readObject();

                            updateHashtagStructures(m);

                        }catch(ClassNotFoundException e){
                            e.printStackTrace();
                        }

                    } else if (flag==4){

                        System.out.println("flag is " + flag);

                        out.writeObject(brokers);
                        out.flush();

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void acceptConnection(AppNodeInfo app){

        Topic t = new Topic(app.getChannelName(), "Channel"); //φτιάχνει ένα topic με το όνομα του καναλιού του AppNode

        registeredUsers.add(app); //προσθέτει τον AppNode στη λίστα
        topicsList.add(t); //προσθέτει το topic στη λίστα
        topicMap.put(app.getChannelName(), brokerInfo); //προσθέτει το ποιος Broker είναι υπέυθυνος για τον AppNode στο χάρτη

        publishedFromMap.put(app.getChannelName(), new ArrayList<>()); //προσθέτει στο map το ποιος AppNode είναι υπέθυνος  γι αυτό το topic
        publishedFromMap.get(app.getChannelName()).add(app); //προφανώς η λίστα θα έχει μόνο τον εαυτό του

        hashTagNumberMap.put(app.getChannelName(), 1); //προσθέτει στα 2 maps το πόσα κανάλια υπάρχουν με αυτό το όνομα
        sameHashtagFromSamePublisher.putIfAbsent(app.getChannelName(), 1); //προφανώς θα μείνει 1

        Message m = new Message(app, brokerInfo, t, 0); //φτιάχνει ένα Message για να ενημερώσει τους υπόλοιπους Brokers για τη νέα εγγραφή
        notifyBrokersOnChanges(m); //ενημερώνει τους υπόλοιπους Brokers
    }

    public void notifyBrokersOnChanges(Message m){

        for (BrokerInfo b: brokers){

            if (!getBrokerName().equals(b.getBrokerName())) { //κάνει μία νέα σύνδεση για κάθε Broker πλην του εαυτού του

                try{

                    Socket connectionNotifier;
                    ObjectOutputStream outNotifier;
                    ObjectInputStream inNotifier;

                    connectionNotifier = new Socket(InetAddress.getLocalHost(), b.getPort());
                    outNotifier = new ObjectOutputStream(connectionNotifier.getOutputStream());
                    inNotifier = new ObjectInputStream(connectionNotifier.getInputStream());

                    outNotifier.writeInt(1); //στέλνει flag=1 για να του πει ότι είναι Broker και θέλει να ενημερώσει τις δομές
                    outNotifier.flush();

                    outNotifier.writeUTF("\n- Α Broker connected to make some updates...");
                    outNotifier.flush();

                    outNotifier.writeObject(m);
                    outNotifier.flush();

                    outNotifier.writeUTF("\n- Updates were made, connection is being terminated");
                    outNotifier.flush();

                    inNotifier.close(); //κλείνει τη σύνδεση
                    outNotifier.close();
                    connectionNotifier.close();

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }
        }

    }

    private void updateStructures(Message m) {

        if (m.getFlag()==0){ //αν flag=0 έχουμε νέο AppNode

            registeredUsers.add(m.getAppNode()); //προσθέτει το νεό AppNode
            topicsList.add(m.getTopic()); //προσθέτει το νεό topic
            topicMap.put(m.getTopic().getName(), m.getBroker()); //προσθέτει το νέο topic και τον υπέυθυνο Broker του στο map
            System.out.println("\n" + m.getBroker().getBrokerName() + " is responsible for channel " + m.getTopic().getName());

        } else if(m.getFlag()==1) { //αν flag=1 έχουμε νέο hashtag

            topicsList.add(m.getTopic());
            topicMap.put(m.getTopic().getName(), m.getBroker());
            System.out.println("\n" + m.getBroker().getBrokerName() + " is responsible for hashtag " + m.getTopic().getName());

        }else if(m.getFlag()==2){ //αν flag=2 έχουμε διαγραφή των topics από τη λίστα και τα maps

            for(Topic topic: m.getTopicsToDelete()){ //γίνεται η διαγραφή με βάση της λίστας των topics προς διαγραφή
                topicsList.remove(topic);
                topicMap.remove(topic.getName());
                topicVideoMap.remove(topic.getName());
            }

        }else if(m.getFlag()==3){ //αν flag=3 ενημερώνονται κατά τη διαγραφή ενός AppNode οι συγκεκριμένες δομές

            updateNodes(m.getAppNode());

        }else if(m.getFlag() == 4){ //αν flag=4 ενημερώνεται το map των hashtags-videos

            for(String hashtag: m.getHashtagsForThisVideo()){ //βάζει όλα τα hashtags ως κλειδιά
                topicVideoMap.putIfAbsent(hashtag, new ArrayList<>()); //και προσθέτει στη λίστα τους
                topicVideoMap.get(hashtag).add(m.getvName()); //αυτό το video name
                videoFromMap.put(m.getvName(), m.getAppNode()); //προσθετει στο map το νέο video name και τον publisher του
            }
        }
    }

    public void addHashTag(Message m){

        if(!topicsList.contains(m.getTopic())){ //αν είναι νέο hashtag, ενημέρωσε
            topicsList.add(m.getTopic()); //τη λίστα
            topicMap.put(m.getTopic().getName(), m.getBroker()); //και το map
            notifyBrokersOnChanges(m); //και τους υπόλοιπους brokers
        }

        if(m.getBroker().getBrokerName().equals(brokerInfo.getBrokerName())){ //αν ο υπέυθυνος Broker είναι ο ίδιος

                updateHashtagStructures(m); //ενημέρωσε τις εσωτερικές δομές που δε χρειάζεται να ενημερωθούν όλοι οι Brokers

        }else{

            try { //αλλιώς κάνε μία νέα σύνδεση με τον μοναδικό Broker που χρειάζεται να ενημερωθεί για τις αλλαγές αυτές

                Socket connTemp = new Socket(InetAddress.getLocalHost(), m.getBroker().getPort());
                ObjectOutputStream outTemp = new ObjectOutputStream(connTemp.getOutputStream());

                outTemp.writeInt(3); //flag=3 για να δηλωθεί πως συνδέεται ένας Broker για την ενημέρωση συγκεκριμένων δομών
                outTemp.flush();

                outTemp.writeObject(m);
                outTemp.flush();

                connTemp.close();
                outTemp.close();

            }catch(UnknownHostException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }
        }

    }

    public void updateHashtagStructures(Message m){

        publishedFromMap.putIfAbsent(m.getTopic().getName(), new ArrayList<>()); //αν το hashtag είναι καινούργιο, μπαίνει στο map με μία νέα λίστα

        sameHashtagFromSamePublisher.putIfAbsent(m.getTopic().getName(), 1); //αν το hashtag είναι καινούργιο, μπαίνει στο map με τον αριθμό 1

        if(!publishedFromMap.get(m.getTopic().getName()).contains(m.getAppNode())){ //αν δεν περιέχει ήδη τον συγκεκριμένο AppNode, σημαίνει ότι ανέβηκε ένα βίντεο με το συγκεκριμένο hashtag από άλλον AppNode
            publishedFromMap.get(m.getTopic().getName()).add(m.getAppNode()); //αν υπάρχει ήδη το συγκεκριμένο AppNode, δεν το ξαναπροσθέτουμε

        }else{
            sameHashtagFromSamePublisher.replace(m.getTopic().getName(), sameHashtagFromSamePublisher.get(m.getTopic().getName()) +1); //ανεβάζουμε όμως τον αριθμό των video με το συγκεκριμένο hashtag
                                                                                                                                        //απ'τον ίδιο AppNode κατά 1
        }

        hashTagNumberMap.putIfAbsent(m.getTopic().getName(), 0); //αν το hashtag είναι καινούργιο, βάλε το με τον αριθμό 0
        hashTagNumberMap.replace(m.getTopic().getName(), hashTagNumberMap.get(m.getTopic().getName())+1); //αλλιώς αύξησε τον αριθμό του συγκεκριμένου hashtag κατά 1

    }

    public synchronized void updateNodes(AppNodeInfo app) {

        registeredUsers.remove(app); //διαγράφεται ο AppNode από τη λίστα

        videoFromMap.values().remove(app); //διαγράφονται όλα τα video του από το map

        for (String key : publishedFromMap.keySet()) { //για όλα τα topics

            if(publishedFromMap.get(key).remove(app)){ //αν είχε συμμετάσχει ο AppNode ως publisher σ'αυτά, διαγράφεται
                hashTagNumberMap.replace(key, hashTagNumberMap.get(key)-sameHashtagFromSamePublisher.get(key)); //και μειώνεται ο αριθμός του κάθε topic
            }                                                           //κατά το πόσα video είχε ανεβάσει ο AppNode με αυτό το topic

            if (publishedFromMap.get(key).isEmpty()) { //αν δεν υπάρχει άλλο video με αυτό το topic
                publishedFromMap.remove(key); //διαγράφεται από το map
            }

        }

        ArrayList<Topic> topicsToDelete = new ArrayList<>(); //λίστα με topics προς διαγραφή

        for(String key: hashTagNumberMap.keySet()){ //για κάθε hashtag
            if (hashTagNumberMap.get(key)==0){ //αν ο αριθμός του έγινε 0
                Topic t = new Topic(key, null);
                topicsList.remove(t); //θα αποσυρθεί από τη λίστα με τα topics
                topicsToDelete.add(t); //και θα προστεθεί στη λίστα προς διαγραφή για να επικοινωνηθεί με τους υπόλοιπους Brokers
                topicMap.remove(key);
            }
        }

        if(!topicsToDelete.isEmpty()) { //αν η λίστα των topics προς διαγραφή δεν είναι άδεια
            Message m = new Message(topicsToDelete, app, 2); //νεό Message με flag=2 για να πραγματοποιηθούν οι αλλαγές
            notifyBrokersOnChanges(m); //σε όλους τους Brokers
        }

    }

    @Override
    public void disconnect() {
        try {

            in.close();
            out.close();
            connection.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getBrokerName() {
        return brokerName;
    }

    public List<BrokerInfo> getBrokers() {
        return brokers;
    }

    public CopyOnWriteArrayList <Topic> getTopicsList(){
        return topicsList;
    }

    public HashMap<String, BrokerInfo> getTopicsMap() {
        return topicMap;
    }

    public HashMap<String, ArrayList<String>> getTopicVideoMap(){
        return topicVideoMap;
    }

    public ConcurrentHashMap<String, AppNodeInfo> getVideoFromMap() {
        return videoFromMap;
    }

    public ConcurrentHashMap <String, ArrayList<AppNodeInfo>> getPublishedFromMap(){
        return publishedFromMap;
    }
}


