package org.emeraldcraft.djitello4j.components;

import org.emeraldcraft.djitello4j.utils.Logger;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.emeraldcraft.djitello4j.utils.Constants.SDK_HOST;
import static org.emeraldcraft.djitello4j.utils.Constants.SDK_PORT;

/**
 * @param command The command to send to the Tello.
 * @param response The response to the command. Can be null.
 * @param timeout The timeout in milliseconds. -1 for no timeout (not suggested)
 */
public record TelloCommand(String command, String response, int timeout){
    /**
     * @return The datagram packet which is sent to the Tello.
     */
    public DatagramPacket getDatagramPacket(){
        try {
            return new DatagramPacket(command.getBytes(), command.length(), InetAddress.getByName(SDK_HOST), SDK_PORT);
        } catch (UnknownHostException e) {
            Logger.error("Could not find the SDK Host at : " + SDK_HOST + " using InetAddress#getByName");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "TelloCommand {" +
                "command='" + command + '\'' +
                ", expected response='" + response + '\'' +
                ", timeout=" + timeout +
                '}';
    }
    public record Response(TelloCommand command, boolean wasSuccessful, Exception exception) {}
}
