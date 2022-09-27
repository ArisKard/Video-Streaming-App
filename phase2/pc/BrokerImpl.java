import java.util.Scanner;

public class BrokerImpl {

    public static void main(String[] args){

        Scanner sc = new Scanner(System.in);
        System.out.println("Initiating Broker number:\t"); //δώσε τον αριθμό του Broker που εκκινείς
        new Broker(sc.nextInt() - 1);

    }
}
