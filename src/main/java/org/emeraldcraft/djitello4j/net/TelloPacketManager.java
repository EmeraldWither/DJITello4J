package org.emeraldcraft.djitello4j.net;

import org.emeraldcraft.djitello4j.Tello;
import org.emeraldcraft.djitello4j.components.TelloCommand;
import org.emeraldcraft.djitello4j.components.TelloCommand.Response;
import org.emeraldcraft.djitello4j.utils.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class TelloPacketManager {
    private final Tello tello;
    private final DatagramSocket socket;
    private final TelloSDKSocket telloSDKSocket;

    private long lastPacketSent = 0;
    public TelloPacketManager(Tello tello) {
        this.tello = tello;
        telloSDKSocket = tello.getTelloSocket();
        socket = telloSDKSocket.getSDKSocket();

    }

    /**
     * Sends a TelloPacket to the drone, and awaits a response.
     * @return The response packet, or null if no response was received or expected.
     */
    public Response sendPacket(TelloCommand command){
        if (telloSDKSocket.isClosed()) return null;
        if(!tello.isEnabled()) return null;

        try{
            socket.setSoTimeout(command.timeout());
            int attempts = 0;
            while (attempts < 5) {
                try {
                    DatagramPacket packet = command.getDatagramPacket();
                    if(packet == null){
                        Logger.error("Datagram packet from command is null. Command: " + command);
                        throw new IllegalStateException("Datagram packet is null");
                    }
                    socket.send(command.getDatagramPacket());
                    this.lastPacketSent = System.currentTimeMillis();

                    socket.receive(packet);
                    String received = new String(
                            packet.getData(), 0, packet.getLength());

                    if (command.command() == null) return new Response(command, true, null);

                    if (command.response().equalsIgnoreCase(received)) {
                        Logger.debug("Got successful (expected) response: \"" + received + "\" for command: \"" + command + "\"");
                        return new Response(command, true, null);
                    }
                    Logger.error("Got unsuccessful packet: \"" + received + "\" for command \"" + command + "\"");
                    attempts++;
                } catch (SocketTimeoutException e) {
                    Logger.error("Socket connection timed out " + command + " || attempt: " + attempts + ", retrying...");
                    attempts++;
                }
            }
            Logger.error("Socket connection timed out" + command + " after 5 attempts.");
        }
        catch (IOException e){
            Logger.error("IOException while sending packet: \"" + command + "\" | GIVING UP!!!");
            return new Response(command, false, e);
        }
        return new Response(command, false, null);
    }


    public long getLastPacketSent() {
        return lastPacketSent;
    }
}
