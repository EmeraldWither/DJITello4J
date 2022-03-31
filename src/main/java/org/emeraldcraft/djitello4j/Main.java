package org.emeraldcraft.djitello4j;

import org.emeraldcraft.djitello4j.Tello.TelloClient;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        TelloClient drone = new TelloClient();
        Scanner scanner = new Scanner(System.in);
        drone.connect();
        //ask user if they want to fly
        System.out.println("The battery is: " + drone.getBattery() + "%\nDo you want to fly? (y/n)");
        String answer = scanner.nextLine();
        if (answer.equals("y")) {
            drone.takeoff();
            //drone.moveForward(200);
            //drone.moveBackward(200);
            drone.land();
        }
        if (answer.equals("n")) {
            System.out.println("Goodbye!");
        }
        drone.disconnect();
    }
}
