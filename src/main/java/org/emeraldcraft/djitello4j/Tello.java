package org.emeraldcraft.djitello4j;

import org.emeraldcraft.djitello4j.components.TelloResponse;
import org.emeraldcraft.djitello4j.components.TelloCommand;
import org.emeraldcraft.djitello4j.net.TelloPacketManager;
import org.emeraldcraft.djitello4j.net.TelloSDKSocket;
import org.emeraldcraft.djitello4j.background.TelloStateReceiver;
import org.emeraldcraft.djitello4j.utils.Logger;

import static org.emeraldcraft.djitello4j.utils.Constants.DEFAULT_TIMEOUT;

public class Tello {
    private final TelloSDKSocket socket;
    private boolean enabled = false;
    private final TelloPacketManager packetManager;
    private final TelloStateReceiver stateReceiver;

    private Tello() {
        this.socket = new TelloSDKSocket();
        if (!socket.isOpen()) {
            Logger.error("Could not initialize Tello class");
        }
        packetManager = new TelloPacketManager(this);
        stateReceiver = new TelloStateReceiver();

    }

    public static Tello createDrone() {
        return new Tello();
    }

    public TelloSDKSocket getTelloSocket() {
        return this.socket;
    }

    public void connect() {
        if (enabled) return;
        stateReceiver.start();
        if (stateReceiver.isOnline()) {
            Logger.info("Tello is online via state receiver");
            enabled = true;
            return;
        }
        stateReceiver.receiveAndParsePackets();
        enabled = true;
        TelloResponse response = packetManager.sendPacket(new TelloCommand("command", "ok", 5000));
        enabled = response.wasSuccessful();
    }

    /**
     * Initiates auto takeoff
     */
    public void takeoff() {
        packetManager.sendPacket(new TelloCommand("takeoff", "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Initiates auto landing
     */
    public void land() {

        packetManager.sendPacket(new TelloCommand("land", "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone forward. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void forward(int distance) {
        if (distance < 20 || distance > 500) throw new IllegalArgumentException("Distance must be between 20 and 500");
        packetManager.sendPacket(new TelloCommand("forward " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone backward. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void backward(int distance) {
        if (distance < 20 || distance > 500) throw new IllegalArgumentException("Distance must be between 20 and 500");
        packetManager.sendPacket(new TelloCommand("back " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone left. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void left(int distance) {
        if (distance < 20 || distance > 500) throw new IllegalArgumentException("Distance must be between 20 and 500");
        packetManager.sendPacket(new TelloCommand("left " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone right. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void right(int distance) {
        if (distance < 20 || distance > 500) throw new IllegalArgumentException("Distance must be between 20 and 500");
        packetManager.sendPacket(new TelloCommand("right " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone up. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void up(int distance) {
        if (distance < 20 || distance > 500) throw new IllegalArgumentException("Distance must be between 20 and 500");
        packetManager.sendPacket(new TelloCommand("up " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone down. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void down(int distance) {
        if (distance < 20 || distance > 500) throw new IllegalArgumentException("Distance must be between 20 and 500");
        packetManager.sendPacket(new TelloCommand("down " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Makes the drone flip forwards
     */
    public void flipForward(){
        packetManager.sendPacket(new TelloCommand("flip f", "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Flips the drone backwards
     */
    public void flipBackward(){
        packetManager.sendPacket(new TelloCommand("flip b", "ok", DEFAULT_TIMEOUT));
    }
    /**
     * Flips the drone left
     */
    public void flipLeft(){
        packetManager.sendPacket(new TelloCommand("flip l", "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Flips the drone right
     */
    public void flipRight(){
        packetManager.sendPacket(new TelloCommand("flip r", "ok", DEFAULT_TIMEOUT));
    }

    public int getBattery() {
        return stateReceiver.getCurrentBattery();
    }

    /**
     * Sets the speed of the drone.
     * @param speed Speed between 0 and 100
     */
    public void setSpeed(int speed){
        if (speed < 10 || speed > 100) throw new IllegalArgumentException("Speed must be between 10 and 100");
        packetManager.sendPacket(new TelloCommand("speed " + speed, "ok", DEFAULT_TIMEOUT));
    }
    /**
     * Disconnects the drone
     */
    public void disconnect() {
        if (!enabled) return;
        socket.close();
        stateReceiver.stopRunning();

        Logger.info("Tello socket closed.");
    }

    /**
     * Sends a custom command to the drone. This may be useful if you need to use a custom timeout or if you want to use commands from a different SDK version
     * @param command The command that you want to send
     * @return The response from the drone. May be null if the drone is not connected or enabled.
     */
    public TelloResponse sendCustomCommand(TelloCommand command){
        return packetManager.sendPacket(command);
    }
    public boolean isEnabled() {
        return enabled;
    }
}
