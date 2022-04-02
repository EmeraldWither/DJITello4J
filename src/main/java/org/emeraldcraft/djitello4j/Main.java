package org.emeraldcraft.djitello4j;

import org.emeraldcraft.djitello4j.Tello.TelloClient;
import org.emeraldcraft.djitello4j.Tello.TelloClient.FlipDirection;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        TelloClient drone = new TelloClient();
        Scanner scanner = new Scanner(System.in);
        boolean isConnected = drone.connect();

        if(!isConnected){
            drone.disconnect();
            return;
        }
        //ask user if they want to fly
        System.out.println("Do you want to fly? (y/n)");
        String answer = scanner.nextLine();
        if (answer.equals("y")) {
            drone.setSpeed(50);
            drone.takeoff();
            drone.moveForward(100);
            drone.flip(FlipDirection.FORWARD);
            drone.moveBackward(100);

            drone.land();
        }
        if (answer.equals("n")) {
            System.out.println("Goodbye!");
        }
        drone.disconnect();
    }
}
