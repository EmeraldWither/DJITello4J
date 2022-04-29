package org.emeraldcraft.djitello4j.net;

import org.emeraldcraft.djitello4j.utils.Logger;

import java.net.DatagramSocket;
import java.net.SocketException;

public class TelloSocket {
    private boolean isOpen;
    private DatagramSocket socket;

    public TelloSocket() {
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            isOpen = false;
            Logger.error("Unable to open the socket for the drone. ");
            e.printStackTrace();
        }
    }
}
