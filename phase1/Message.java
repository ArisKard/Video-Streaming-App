import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {//Object Carrier για να γίνεται επικοινωνία μεταξύ Brokers και AppNodes

    private AppNodeInfo app;
    private BrokerInfo b;
    private Topic topic;
    private int flag; //το flag καθορίζει το τι τύπου διεργασία πρέπει να γίνει με αυτό το Message
    private String vName;

    private ArrayList<Topic> topicsToDelete; //λίστα με τα topics που πρέπει να διαγραφούν κατά την αποχώρηση ενός AppNode
    private ArrayList<String> hashtagsForThisVideo; //λίστα με τα hashtags για ένα συγκεκριμένο video

    public Message(AppNodeInfo app, BrokerInfo b, Topic topic, int flag){ //τρεις διαφορετικοί constructors

        this.app = app;
        this.b = b;
        this.topic = topic;
        this.flag = flag;
    }

    public Message(ArrayList<Topic> topicsToDelete, AppNodeInfo app, int flag){

        this.app = app;
        this.topicsToDelete = topicsToDelete;
        this.flag = flag;
    }

    public Message(ArrayList<String> hashtagsForThisVideo, String vName, AppNodeInfo app, int flag){

        this.hashtagsForThisVideo = hashtagsForThisVideo;
        this.vName = vName;
        this.app = app;
        this.flag = flag;
    }

    public AppNodeInfo getAppNode(){
        return app;
    }

    public BrokerInfo getBroker(){
        return b;
    }

    public int getFlag(){
        return flag;
    }

    public ArrayList<Topic> getTopicsToDelete(){
        return topicsToDelete;
    }

    public Topic getTopic(){
        return topic;
    }

    public ArrayList<String> getHashtagsForThisVideo() {
        return hashtagsForThisVideo;
    }

    public String getvName(){
        return vName;
    }

}
