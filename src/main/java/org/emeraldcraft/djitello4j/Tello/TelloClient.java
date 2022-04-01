package org.emeraldcraft.djitello4j.Tello;

import org.emeraldcraft.djitello4j.Log;

import java.io.IOException;
import java.net.*;

/**
 * Represents the Tello Drone. This class is responsible for sending and receiving data from the Tello.
 * The battery and state information is retrieved from {@link TelloStateReceiver}
 * @see TelloStateReceiver
 * @author EmerqldWither
 */
public class TelloClient {
    private long timeOfLastCommand = 0;
    private boolean hasConnected = false;
    private boolean isEnabled = true;
    private DatagramSocket socket;
    private InetAddress address;
    private final int port = 8889;
    private Thread keepAliveThread;
    private TelloStateReceiver receiver;

    public TelloClient(){
        try{
            socket = new DatagramSocket(port);
            address = InetAddress.getByName("192.168.10.1");
            receiver = new TelloStateReceiver();
        }
        catch (SocketException | UnknownHostException e){
            if(e instanceof SocketException){
                Log.error("There was an error while creating the socket! (Is the Tello connected and is bypassing the firewall?)");
                e.printStackTrace();
            }
            else {
                Log.error("There was an error while finding the address (192.168.10.1)! (Is the Tello connected and is bypassing the firewall?)");
                e.printStackTrace();
            }
        }
    }

    public static class FlipDirection {
        public static final String FORWARD = "f";
        public static final String BACKWARD = "b";
        public static final String LEFT = "l";
        public static final String RIGHT = "r";
    }

    /**
     * Connect to the drone. This method will block until the connection is established.
     * @return True if the Tello has connected, or, is already connected. False if the Tello has failed to connect.
     */
    public boolean connect() {
        //First check to see if we are receiving state packets
        if (hasConnected) return true;

        receiver.start();
        if (receiver.isOnline()) {
            sendKeepAlivePacket();
            startKeepAliveThread();
            receiver.receiveAndParsePackets();
            hasConnected = true;

            Log.debug("The TelloStateReceiver is alive and receiving packets. Marked as running.");
            Log.debug("Tello is connected, battery is " + getBattery() + "%");
            return true;
        }
        Log.debug("Receiver is dead. Trying to reconnect...");
        String response = sendPacket("command", "ok", 5_000);
        if (response != null) {
            startKeepAliveThread();
            receiver.receiveAndParsePackets();
            hasConnected = true;
            Log.debug("Tello is connected via a new connection");
            Log.debug("Tello is connected, battery is " + getBattery() + "%");
            return true;
        }
        Log.error("Tello is not connected. Please check the Tello's connection and try again.\n" +
                "Ensure that the Tello is allowed to bypass Windows Firewall settings (this can usually be done by marking it as a private network)");
        isEnabled = false;
        return false;
    }

    private void startKeepAliveThread() {
        keepAliveThread = new Thread(() -> {
            if (isEnabled) {
                try {
                    //Check to see if 15 seconds have passed since the last command was sent
                    if (System.currentTimeMillis() - this.timeOfLastCommand > 13_000) {
                        sendKeepAlivePacket();
                    }
                    Thread.sleep(3_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        keepAliveThread.start();
    }

    public void sendKeepAlivePacket() {
        if (System.currentTimeMillis() - this.timeOfLastCommand > 13_000) {
            String response = sendPacket("alive", null, 3_000);
            if (response == null) {
                System.out.println("Failed to send keep-alive packet");
                return;
            }
            Log.debug("Sent keep-alive packet");
        }
    }


    public void takeoff() {
        sendKeepAlivePacket();
        String response = sendPacket("takeoff", "ok", 20_000);
        if (response != null) {
            System.out.println("Tello took off");
            return;
        }
        System.out.println("Failed to takeoff");
    }

    public void land() {
        sendKeepAlivePacket();
        String response = sendPacket("land", "ok", 20_000);
        if (response != null) {
            System.out.println("Tello landed");
            return;
        }
        System.out.println("Failed to land");
    }

    public void moveForward(int amount) {
        sendKeepAlivePacket();
        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("forward " + amount, "ok", calcSpeedTime(amount));
    }

    public void moveBackward(int amount) {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("back " + amount, "ok", calcSpeedTime(amount));
    }

    public void moveRight(int amount) {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("right " + amount, "ok", calcSpeedTime(amount));
    }

    public void moveLeft(int amount) {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("left " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Please use the {@link FlipDirection} class in order to know which direction to flip
     * Or you could just type it in manually
     *
     * @param direction The direction to flip in
     * @throws IOException If there is an error sending a packet
     */
    public void flip(String direction)  {
        sendKeepAlivePacket();
        sendPacket("flip " + direction, "ok", 20_000);
    }

    public int getBattery() {
        return this.receiver.getCurrentBattery();
    }

    /**
     * @return The response from the Tello.
     */
    private String sendPacket(String command, String expectedResponse, int timeout){
        if (!isEnabled) return null;
        try{
            socket.setSoTimeout(timeout);
            int attempts = 0;
            while (attempts < 5) {
                try {
                    byte[] buf = command.getBytes();
                    DatagramPacket packet
                            = new DatagramPacket(buf, buf.length, address, port);
                    socket.send(packet);
                    this.timeOfLastCommand = System.currentTimeMillis();

                    packet = new DatagramPacket(buf, buf.length, address, port);
                    socket.receive(packet);
                    String received = new String(
                            packet.getData(), 0, packet.getLength());

                    if (expectedResponse == null) return received;

                    if (expectedResponse.equalsIgnoreCase(received)) {
                        Log.debug("Got successful (expected) response: \"" + received + "\" for command: \"" + command + "\"");
                        return received;
                    }
                    Log.error("Got unsuccessful packet: \"" + received + "\" for command \"" + command + "\"");
                    attempts++;
                } catch (SocketTimeoutException e) {
                    Log.error("Socket connection timed out. Command: \"" + command + "\" || expected response: \"" + expectedResponse + "\" || timeout: " + timeout + "ms || attempt: " + attempts + ", retrying...");
                    attempts++;
                }
            }
            Log.error("Socket connection timed out after 5 attempts for command: \"" + command + "\" with an expected response \"" + expectedResponse + "\" with a timeout: " + timeout + "ms | GIVING UP!!!");
        }
        catch (IOException e){
            Log.error("IOException while sending packet: \"" + command + "\" with an expected response \"" + expectedResponse + "\" with a timeout: " + timeout + "ms | GIVING UP!!!");
        }
        return null;
    }

    public void disconnect() {
        if (keepAliveThread != null) {
            keepAliveThread.interrupt();
        }
        receiver.stopRunning();
        socket.disconnect();
        System.out.println("Tello disconnected. Goodbye!");
    }

    public void setSpeed(int speed)  {
        sendKeepAlivePacket();

        if (speed > 100) throw new IllegalArgumentException("Speed must be less than 100");
        if (speed < 10) throw new IllegalArgumentException("Speed must be greater than 10");

        String response = sendPacket("speed " + speed, "ok", 5_000);
        if (response != null) {
            System.out.println("Tello set speed to " + speed);
            return;
        }
        System.out.println("Failed to set speed");
    }
    private int calcSpeedTime(int speed) {
        return ((speed / 15) + 3) * 1000;
    }
}
