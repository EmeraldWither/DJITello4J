package org.emeraldcraft.djitello4j.Tello;

import org.emeraldcraft.djitello4j.Log;

import java.io.IOException;
import java.net.*;

public class TelloStateReceiver extends Thread {
    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private boolean running = false;

    private int currentBattery = 0;
    private int currentTemp = 0;
    private int height = 0;
    private int tof = 0;

    public TelloStateReceiver() throws SocketException {
        try {
            socket = new DatagramSocket(8890, InetAddress.getByName("0.0.0.0"));
            socket.setSoTimeout(2000);
            //Try checking to see if we are online and running
            if (isOnline()) {
                running = true;
                //Recieve 1st state packet
                this.receiveAndParsePackets();
                Log.debug("Tello state receiver started");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        while (running) {
            receiveAndParsePackets();
        }
    }
    public void receiveAndParsePackets() {
        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength());
            parseResponse(message);
        } catch (IOException e) {
            if(!running) return;
            Log.error("Error receiving packet from Tello");
            e.printStackTrace();
        }
    }
    private void parseResponse(String response){
        String[] data = response.split(";");
        for(String s : data){
            if(s.isBlank()) continue;

            String dataType = s.split(":")[0];
            String dataValue = s.split(":")[1];
            switch (dataType) {
                case "bat" -> this.currentBattery = Integer.parseInt(dataValue);
                case "temp" -> this.currentTemp = Integer.parseInt(dataValue);
                case "h" -> this.height = Integer.parseInt(dataValue);
                case "tof" -> this.tof = Integer.parseInt(dataValue);
            }
        }
    }
    public void stopRunning() {
        running = false;
    }
    public boolean isOnline(){
        try{
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            return true;
        }
        catch(IOException exception){
            return false;
        }
    }

    public int getCurrentBattery() {
        return currentBattery;
    }

    public int getCurrentTemp() {
        return currentTemp;
    }

    public int getHeight() {
        return height;
    }

    public int getTof() {
        return tof;
    }
}