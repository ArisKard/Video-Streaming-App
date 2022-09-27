import java.util.Scanner;

public class AppNodeImpl {

    public static void main (String[] args){

        Scanner sc = new Scanner(System.in);
        System.out.println("Initiating AppNode number:\t"); //δώσε τον αριθμό του AppNode που εκκινείς, στην περίπτωση μας 1 ή 2, ώστε να είναι σωστά τα directories για τα video
        new AppNode(sc.nextInt(),args[0], args[1]);

    }
}
