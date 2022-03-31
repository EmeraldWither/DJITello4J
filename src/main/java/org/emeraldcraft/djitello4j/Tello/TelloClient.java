package org.emeraldcraft.djitello4j.Tello;

import org.emeraldcraft.djitello4j.Log;

import java.io.IOException;
import java.net.*;

public class TelloClient {
    private long timeOfLastCommand = 0;
    private boolean hasConnected = false;
    private boolean isEnabled = true;
    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port = 8889;
    private byte[] buf;
    private Thread keepAliveThread;
    private final TelloStateReceiver receiver = new TelloStateReceiver();

    public TelloClient() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        address = InetAddress.getByName("192.168.10.1");
    }

    public void connect() throws IOException {
        //First check to see if we are receiving state packets
        if (hasConnected) return;

        receiver.start();
        if (receiver.isOnline()) {
            sendKeepAlivePacket();
            startKeepAliveThread();
            receiver.receiveAndParsePackets();
            hasConnected = true;

            Log.debug("Receiver is alive");
            System.out.println("Tello is already connected");
            return;
        }
        Log.debug("Receiver is dead. Trying to reconnect...");
        String response = sendPacket("command", "ok", 5_000);
        if (response != null) {
            System.out.println("Tello is connected");
            startKeepAliveThread();
            receiver.receiveAndParsePackets();
            hasConnected = true;
            return;
        }
        System.out.println("Failed to connect");
        isEnabled = false;

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
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        keepAliveThread.start();
    }

    public void sendKeepAlivePacket() throws IOException {
        if (System.currentTimeMillis() - this.timeOfLastCommand > 13_000) {
            String response = sendPacket("alive", null, 3_000);
            if (response == null) {
                System.out.println("Failed to send keep-alive packet");
                return;
            }
            Log.debug("Sent keep-alive packet");
        }
    }


    public void takeoff() throws IOException {
        sendKeepAlivePacket();
        String response = sendPacket("takeoff", "ok", 20_000);
        if (response != null) {
            System.out.println("Tello took off");
            return;
        }
        System.out.println("Failed to takeoff");
    }

    public void land() throws IOException {
        sendKeepAlivePacket();
        String response = sendPacket("land", "ok", 20_000);
        if (response != null) {
            System.out.println("Tello landed");
            return;
        }
        System.out.println("Failed to land");
    }

    public void moveForward(int amount) throws IOException {
        sendKeepAlivePacket();
        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        String response = sendPacket("forward " + amount, "ok", 5_000);
        if (response != null) {
            System.out.println("Tello moved forward " + amount);
            return;
        }
        System.out.println("Failed to move forward");
    }

    public void moveBackward(int amount) throws IOException {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        String response = sendPacket("back " + amount, "ok", 5_000);
        if (response != null) {
            System.out.println("Tello moved backward " + amount);
            return;
        }
        System.out.println("Failed to move backward");
    }

    public void moveRight(int amount) throws IOException {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        String response = sendPacket("right " + amount, "ok", 5_000);
        if (response != null) {
            System.out.println("Tello moved right " + amount);
            return;
        }
        System.out.println("Failed to move right");
    }

    public void moveLeft(int amount) throws IOException {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        String response = sendPacket("left " + amount, "ok", 5_000);
        if (response != null) {
            System.out.println("Tello moved left " + amount);
            return;
        }
        System.out.println("Failed to move left");
    }

    public int getBattery() {
        return this.receiver.getCurrentBattery();
    }

    /**
     * @return The response from the Tello.
     */
    private String sendPacket(String command, String expectedResponse, int timeout) throws IOException {
        if (!isEnabled) return null;

        socket.setSoTimeout(timeout);
        int attempts = 0;
        while (attempts < 5) {
            try {
                buf = command.getBytes();
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
                    Log.debug("Got response: \"" + received + "\" for command: \"" + command + "\"");
                    return received;
                }
                Log.error("Got unsuccessful packet: \"" + received + "\" for command \"" + command + "\"");
                attempts++;
            } catch (SocketTimeoutException e) {
                Log.error("Socket connection timed out. Command: \"" + command + "\" || expected response: \"" + expectedResponse + "\" || timeout: " + timeout + "ms || attempt: " + attempts + ", retrying...");
                attempts++;
            }
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
}
