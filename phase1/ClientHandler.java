import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler {

    private boolean publisherMode=true; //flag για το αν βρισκόμαστε σε publisher ή consumer mode
    private boolean running = true; //flag για το αν έχει κάνει disconnect o AppNode

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket connection;

    private AppNodeInfo app; //τα στοιχεία του AppNode με τον οποίο υπάρχει η σύνδεση
    private Broker broker; //Object Broker ώστε να υπάρχει επικοινωνία με τις μεθόδους και τις δομές του Broker

    public ClientHandler(AppNodeInfo app, Socket connection, Broker broker, ObjectInputStream in, ObjectOutputStream out) {

        this.app = app;

        this.connection = connection;

        this.broker = broker;

        this.out = out;

        this.in = in;

    }

    public void publisher() throws InterruptedException{

        String message, initialAnswer = null; //κρατά την αρχική απάντηση στο αν θέλει να κάνει upload, να αλλάξει σε consumer
                                            //ή να κάνει disconnect

        synchronized (this) {

            while (running) {

                while (!publisherMode) {
                    System.out.println("\nChannel "+app.getChannelName()+" Switching to Consumer mode ");
                    wait(); //όσο δεν είναι σε publisher mode, περιμένει
                }

                try {

                    out.writeObject("\n1. Upload a video \n2. Switch to Consumer \n3. Disconnect ");
                    out.flush();
                    initialAnswer = (String) in.readObject(); //πάρε την αρχική απάντηση

                    if (initialAnswer.equals("1")) { //θέλει να κάνει upload

                        out.writeObject(" \nAvailable Videos: ");
                        out.flush();

                        System.out.println("\nChannel "+ app.getChannelName() + " is uploading a video...");

                        ArrayList<String> hashtagsForThisVideo = new ArrayList<>(); //λίστα για τα hashtags αυτού του video
                        String hashtag = (String) in.readObject(); //παίρνει το hashtag
                        String vName = (String) in.readObject(); //και το video name

                        broker.getTopicVideoMap().putIfAbsent(app.getChannelName(), new ArrayList<>()); //και αν δεν υπάρχουν,
                        broker.getTopicVideoMap().get(app.getChannelName()).add(vName); //ενημερώνει τις κατάλληλες δομές

                        broker.getTopicVideoMap().putIfAbsent(hashtag, new ArrayList<>());
                        broker.getTopicVideoMap().get(hashtag).add(vName);

                        broker.getVideoFromMap().put(vName, app);

                        BrokerInfo b = (BrokerInfo) in.readObject();

                        Message m = new Message(app, b, new Topic(hashtag,"Hashtag"), 1); //νέο Message
                        broker.addHashTag(m); //για την ενημέρωση των υπόλοιπων Brokers

                        hashtagsForThisVideo.add(app.getChannelName()); //βάζει το channel name
                        hashtagsForThisVideo.add(hashtag); //και το hashtag στη λίστα των συνδεόμενων topics με αυτό το βίντεο

                        do { //για την προσθήκη κι άλλων hashtag
                            out.writeObject("\n1. Add more hashtags \n2. No more hashtags ");
                            out.flush();
                            message = (String) in.readObject();

                            if(message.equals("1")){
                                out.writeObject("\nType hashtag: ");
                                out.flush();
                                hashtag = (String) in.readObject();

                                broker.getTopicVideoMap().putIfAbsent(hashtag, new ArrayList<>());
                                broker.getTopicVideoMap().get(hashtag).add(vName);

                                b = (BrokerInfo) in.readObject();

                                m = new Message(app, b, new Topic(hashtag,"Hashtag"), 1);
                                broker.addHashTag(m);

                                hashtagsForThisVideo.add(hashtag);
                            }

                            Message mess = new Message(hashtagsForThisVideo, vName, app, 4); //νεό Message με flag=4 για να ενημερωθεί το map
                            broker.notifyBrokersOnChanges(mess); //για το ποια video σχετίζονται με αυτά το hashtag

                        }while(message.equals("1"));

                        System.out.println("\nVideo uploaded ");

                    }

                    if (initialAnswer.equals("2")) { //αν η αρχική απάντηση ήταν 2,
                        publisherMode = false; //γυρνά σε consumer mode
                        notifyAll(); //και ενημερώνει το άλλο thread
                    }

                } catch (IOException exc) {
                    exc.printStackTrace();
                } catch (ClassNotFoundException exc) {
                    exc.printStackTrace();
                }


                if(initialAnswer.equals("3")){ //αν η αρχική απάντηση ήταν 2,

                    broker.updateNodes(app); //γίνεται update των δομών για τη διαγραφή του συγκεκριμένου AppNode
                    Message m = new Message(app, null, null, 3); //νεο Message με flag=3 για να γίνει το update
                    broker.notifyBrokersOnChanges(m); //και στους υπόλοιπους Brokers
                    System.out.println("Channel "+app.getChannelName()+" Disconnected ");
                    running=false;

                    try {
                        out.close(); //η σύνδεση κλείνει
                        in.close();
                        connection.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }

                }
            }
        }
    }


    public void consumer() throws InterruptedException {

        String answer;

        synchronized (this){

            while (running) {

                while (publisherMode) {
                    System.out.println("Channel "+app.getChannelName()+" Switching to Publisher Mode");
                    wait(); //όσο δεν είναι σε publisher mode, περιμένει
                }

                try {

                    boolean ifAlreadySubscribed = in.readBoolean(); //ενημερώνεται αν υπάρχει ήδη subscription, έτσι ώστε να τσεκάρει
                                                                  //αν έχουν ανέβει νέα βίντεο που δεν έχει λάβει
                    if(ifAlreadySubscribed){
                        String subscribedTo = (String)in.readObject(); //ενημερώνεται για το topic στο οποίο έχει κάνει subscribe

                        ArrayList<String> vNames = broker.getTopicVideoMap().get(subscribedTo); //λαμβάνει τη λίστα με τα video με αυτό το topic

                        BrokerInfo b = broker.getTopicsMap().get(subscribedTo); //λαμβάνει τον Broker που είναι υπεύθυνος γι'αυτό το topic

                        out.writeObject(b);
                        out.flush();

                        out.writeUnshared(vNames); //στέλνει τη λίστα με τα video
                        out.flush();
                    }



                    out.writeObject("\n1. Subscribe to a Topic \n2. Switch to Publisher \n3. Disconnect");
                    out.flush();
                    answer = (String) in.readObject();

                    if (answer.equals("1")) {
                        out.writeObject("\nAvailable Topics: ");
                        out.flush();

                        out.writeUnshared(broker.getTopicsList());
                        out.flush();

                        Topic topic = (Topic) in.readObject(); //λάβε το topic που θα γίνει το subscription
                        BrokerInfo b = broker.getTopicsMap().get(topic.getName()); //και τον broker που είναι υπέυθυνος

                        System.out.println("\nBroker Responsible for topic "+topic.getName() + ": "+b.getBrokerName());

                        out.writeObject(b);
                        out.flush();
                    }

                    if (answer.equals("2")) { //αν η αρχική απάντηση ήταν 2,
                        publisherMode = true; //γυρνά σε publisher mode
                        notifyAll(); //και γίνεται notify στα threads που περιμένουν
                    }

                    if(answer.equals("3")){ //αν η αρχική απάντηση ήταν 3,

                        broker.updateNodes(app);    //γίνεται update των δομών για τη διαγραφή του συγκεκριμένου AppNode
                        Message m = new Message(app, null, null, 3); //νεο Message με flag=3 για να γίνει το update
                        broker.notifyBrokersOnChanges(m); //και στους υπόλοιπους Brokers
                        System.out.println("Channel "+app.getChannelName()+" Disconnected ");
                        running=false;


                        out.close(); //η σύνδεση κλείνει
                        in.close();
                        connection.close();
                    }

                } catch (IOException exc) {
                    exc.printStackTrace();
                } catch (ClassNotFoundException exc) {
                    exc.printStackTrace();
                }

            }
        }
    }

    public void server() throws IOException{

        try {

            int flag = in.readInt(); //παίρνει flag για το αν ζητά ήδη υπάρχοντα ή νέο video
            if(flag==0) {  //αν flag=1, θέλει ήδη υπάρχοντα video

                Topic topic = (Topic) in.readObject(); //το topic που θα έγινε
                ArrayList<AppNodeInfo> appNodeInfoList = broker.getPublishedFromMap().get(topic.getName()); //η λίστα των publishers που πρέπει να γίνει σύνδεση
                                                                                                           //ώστε να κάνουν push τα video
                AppNodeInfo appRequesting = (AppNodeInfo) in.readObject(); //ο AppNode που ζητά τα video

                out.writeInt(appNodeInfoList.size()); //το πλήθος των publishers
                out.flush();

                for (AppNodeInfo app : appNodeInfoList) { //για κάθε publisher

                    boolean sameAppNode = app.equals(appRequesting);
                    out.writeBoolean(sameAppNode);
                    out.flush();

                    if (!sameAppNode) { //φιλτράρισμα  για να μη λάβει ο consumer video που έχει ανεβάσει ο ίδιος

                        Socket connectionToPub = new Socket(InetAddress.getLocalHost(), app.getPort()); //νέα σύνδεση με τον AppNode
                        ObjectOutputStream outToPub = new ObjectOutputStream(connectionToPub.getOutputStream()); //που θα κάνει push
                        ObjectInputStream inFromPub = new ObjectInputStream(connectionToPub.getInputStream());

                        outToPub.writeInt(0); //flag για το πόσα video ζητά ο AppNode
                        outToPub.flush();

                        outToPub.writeObject(topic);  //το topic που πρέπει να γίνει push
                        outToPub.flush();

                        int numOfVids = inFromPub.readInt(); //το πλήθος των video που θα γίνουν push από αυτόν τον publisher

                        System.out.println("Number of videos: " + numOfVids);

                        out.writeInt(numOfVids); //στέλνει το πλήθος των video στον consumer
                        out.flush();

                        for (int i = 0; i < numOfVids; i++) {

                            int numOfChunks = inFromPub.readInt(); //το πλήθος των chunks που θα γίνουν push γι'αυτό το video

                            out.writeInt(numOfChunks); //στέλνει τον αριθμό των chunks για αυτό το video στον consumer
                            out.flush();

                            String vName = inFromPub.readUTF(); //το όνομα του video που γίνεται push

                            out.writeUTF(vName); //στέλνει το όνομα του video στον consumer
                            out.flush();

                            for (int j = 0; j < numOfChunks; j++) { //λαμβάνει κι αμέσως στέλνει τα chunks στον consumer

                                out.writeObject(inFromPub.readObject());
                                out.flush();
                            }
                        }

                        connectionToPub.close(); //κλείνει η σύνδεση με τον publisher
                        outToPub.close();
                        inFromPub.close();
                    }
                }
            }

            else if(flag==1){ //αν flag=1, θέλει νέο video

                String vName = (String) in.readObject(); //λαμβάνει το όνομα του video
                AppNodeInfo appPushing = broker.getVideoFromMap().get(vName); //o AppNode που θα κάνει push το video
                System.out.println(vName);
System.out.println(broker.getVideoFromMap().values());
                Socket connectionToPub = new Socket(InetAddress.getLocalHost(), appPushing.getPort()); //νέα σύνδεση με τον AppNode
                ObjectOutputStream outToPub = new ObjectOutputStream(connectionToPub.getOutputStream());//που θα κάνει push
                ObjectInputStream inFromPub = new ObjectInputStream(connectionToPub.getInputStream());

                outToPub.writeInt(1); //flag για το πόσα video ζητά ο AppNode
                outToPub.flush();

                outToPub.writeObject(vName); //το όνομα του video που πρέπει να γίνει push
                outToPub.flush();

                int numOfChunks = inFromPub.readInt(); //o αριθμός των chunks

                out.writeInt(numOfChunks); //στέλνει τον αριθμό των chunks στον consumer
                out.flush();

                String video = inFromPub.readUTF(); //αχρείαστο, κατανάλωση ροής απ'το push

                for (int j = 0; j < numOfChunks; j++) { //λαμβάνει κι αμέσως στέλνει τα chunks στον consumer

                    out.writeObject(inFromPub.readObject());
                    out.flush();
                }

//                connectionToPub.close(); //κλείνει η σύνδεση με τον publisher
//                outToPub.close();
//                inFromPub.close();
            }

        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }

    }

}
