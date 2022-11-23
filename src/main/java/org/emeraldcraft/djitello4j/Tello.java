package org.emeraldcraft.djitello4j;

import org.emeraldcraft.djitello4j.components.TelloCommand;
import org.emeraldcraft.djitello4j.components.TelloCommand.Response;
import org.emeraldcraft.djitello4j.net.TelloPacketManager;
import org.emeraldcraft.djitello4j.net.TelloSDKSocket;
import org.emeraldcraft.djitello4j.background.TelloStateReceiver;
import org.emeraldcraft.djitello4j.utils.Logger;
import org.jetbrains.annotations.Range;

import static org.emeraldcraft.djitello4j.utils.Constants.DEFAULT_TIMEOUT;
@SuppressWarnings("unused")
public class Tello {
    private final TelloSDKSocket socket;
    private boolean enabled = false;
    private final TelloPacketManager packetManager;
    private final TelloStateReceiver stateReceiver;

    private Tello() {
        this.socket = new TelloSDKSocket();
        if (socket.isClosed()) {
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
        Response response = packetManager.sendPacket(new TelloCommand("command", "ok", 5000));
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
    public void forward(@Range(from = 20, to = 500) int distance) {
        packetManager.sendPacket(new TelloCommand("forward " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone backward. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void backward(@Range(from = 20, to = 500) int distance) {
        packetManager.sendPacket(new TelloCommand("back " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone left. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void left(@Range(from = 20, to = 500) int distance) {
        packetManager.sendPacket(new TelloCommand("left " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone right. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void right(@Range(from = 20, to = 500) int distance) {
        packetManager.sendPacket(new TelloCommand("right " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone up. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void up(@Range(from = 20, to = 500) int distance) {
        packetManager.sendPacket(new TelloCommand("up " + distance, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Moves the drone down. Must be between 20 and 500
     * @param distance Distance in centimeters
     */
    public void down(@Range(from = 20, to = 500) int distance) {
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
    public void setSpeed(@Range(from = 10, to = 100) int speed){
        packetManager.sendPacket(new TelloCommand("speed " + speed, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Rotate the tello clockwise
     * @param degrees Degrees between 0 and 3600
     */
    public void cw(@Range(from = 1, to = 3600) int degrees){
        packetManager.sendPacket(new TelloCommand("cw " + degrees, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Rotate the tello counterclockwise
     * @param degrees Degrees between 0 and 3600
     */
    public void ccw(@Range(from = 1, to = 3600)int degrees){
        packetManager.sendPacket(new TelloCommand("ccw " + degrees, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Tello fly to x y z in speed (cm/s)
     *
     * @param x X position between 20 and 500
     * @param y Y position between 20 and 500
     * @param z Z position between 20 and 500
     * @param speed Speed between 10 and 60
     */
    public void goXYZSpeed(@Range(from = 20, to = 500)int x, @Range(from = 20, to = 500) int y, @Range(from = 20, to = 500)int z, @Range(from = 10, to = 60)int speed){
        packetManager.sendPacket(new TelloCommand("go " + x + " " + y + " " + z + " " + speed, "ok", DEFAULT_TIMEOUT));
    }

    /**
     * Tello fly a curve defined by the
     * current and two given coordinates
     * with speed (cm/s)
     * If the arc radius is not within
     * the range of 0.5-10 meters, it
     * will cause an error
     *
     * @param x1 X position between 20 and 500
     * @param y1 Y position between 20 and 500
     * @param z1 Z position between 20 and 500
     * @param x2 X position between 20 and 500
     * @param y2 Y position between 20 and 500
     * @param z2 Z position between 20 and 500
     * @param speed Speed between 10 and 60
     */
    public void curve(@Range(from = 20, to = 500)int x1, @Range(from = 20, to = 500)int y1, @Range(from = 20, to = 500)int z1, @Range(from = 20, to = 500)int x2, @Range(from = 20, to = 500)int y2, @Range(from = 20, to = 500)int z2, @Range(from = 10, to = 60)int speed){
        packetManager.sendPacket(new TelloCommand("curve " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + speed, "ok", DEFAULT_TIMEOUT));
    }
    public void rc(int lr, int fb, int ud, int yaw){
        packetManager.sendPacket(new TelloCommand("rc " + lr + " " + fb + " " + ud + " " + yaw, "ok", DEFAULT_TIMEOUT));
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
    public Response sendCustomCommand(TelloCommand command){
        return packetManager.sendPacket(command);
    }
    public boolean isEnabled() {
        return enabled;
    }
}
