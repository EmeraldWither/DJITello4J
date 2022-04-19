import org.emeraldcraft.djitello4j.tello.TelloClient;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        TelloClient drone = new TelloClient();
        Scanner scanner = new Scanner(System.in);
        boolean isConnected = drone.connect();

        if (!isConnected) {
            drone.disconnect();
            return;
        }
        //ask user if they want to fly
        System.out.println("Do you want to fly? (y/n)");
        String answer = scanner.nextLine();
        if (answer.equals("y")) {
            drone.setSpeed(40);
            drone.takeoff();
            drone.moveForward(50);
            drone.moveBackward(50);
            drone.land();
        } else {
            System.out.println("Goodbye!");
        }
        drone.disconnect();
    }
}
