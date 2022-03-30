package org.emeraldcraft.djitello4j.Tello;

import org.emeraldcraft.djitello4j.Log;

import java.io.IOException;
import java.net.*;

public class TelloClient {
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
        if(hasConnected) return;

        receiver.start();
        if (receiver.isOnline()) {
            Log.debug("Receiver is alive");
            startKeepAliveThread();
            receiver.receiveAndParsePackets();
            hasConnected = true;

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
                    sendKeepAlivePacket();
                    Thread.sleep(3_000);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendKeepAlivePacket() throws IOException {
        String response = sendPacket("alive", "ok", 3_000);
        if (response == null) {
            System.out.println("Failed to send keep-alive packet");
        }
    }


    public void takeoff() throws IOException {
        String response = sendPacket("takeoff", "ok", 20_000);
        if (response != null) {
            System.out.println("Tello took off");
            return;
        }
        System.out.println("Failed to takeoff");
    }

    public void land() throws IOException {
        String response = sendPacket("land", "ok", 20_000);
        if (response != null) {
            System.out.println("Tello landed");
            return;
        }
        System.out.println("Failed to land");
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
                packet = new DatagramPacket(buf, buf.length);
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
    }
}
