import org.emeraldcraft.djitello4j.Tello;

import java.util.Scanner;

public class TelloExample {
    public static void main(String[] args) {
        Tello tello = Tello.createDrone();
        Scanner scanner = new Scanner(System.in);
        tello.connect();
        System.out.println("battery is " + tello.getBattery());
        if(scanner.nextLine().equalsIgnoreCase("y")) {
            tello.takeoff();
            tello.setSpeed(40);
            tello.flipForward();
            tello.flipBackward();
            tello.backward(50);
            tello.land();
        }
        System.out.println("goodbye");
        tello.disconnect();
    }
}
