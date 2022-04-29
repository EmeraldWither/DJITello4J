package org.emeraldcraft.djitello4j.net;

import org.emeraldcraft.djitello4j.utils.Logger;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.emeraldcraft.djitello4j.utils.Constants.SDK_HOST;
import static org.emeraldcraft.djitello4j.utils.Constants.SDK_PORT;

public class TelloSDKSocket {
    private boolean isOpen = false;
    private DatagramSocket SDKSocket;
    private InetAddress address;

    public TelloSDKSocket() {
        try {
            this.SDKSocket = new DatagramSocket(SDK_PORT);
            this.address = InetAddress.getByName(SDK_HOST);
            isOpen = true;
        } catch (SocketException | UnknownHostException e) {
            isOpen = false;
            Logger.error("Unable to open the socket for the drone.");
            e.printStackTrace();
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public DatagramSocket getSDKSocket() {
        return SDKSocket;
    }

    public InetAddress getAddress() {
        return address;
    }
    public void close(){
        SDKSocket.close();
    }
}
