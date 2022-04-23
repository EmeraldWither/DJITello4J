package org.emeraldcraft.djitello4j.tello;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;

import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.javacv.OpenCVFrameConverter.ToOrgOpenCvCoreMat;
import static org.emeraldcraft.djitello4j.tello.TelloClient.Log.*;

/**
 * Represents the Tello Drone. This class is responsible for sending and receiving data from the Tello.
 * The battery and state information is retrieved from {@link TelloStateReceiver}
 *
 * @author EmerqldWither
 * @see TelloStateReceiver
 */
public class TelloClient {
    static {
        Loader.load(opencv_java.class);
    }

    private long timeOfLastCommand = 0;
    private boolean canRestart = true;
    private boolean hasConnected = false;
    private boolean isEnabled = true;
    private DatagramSocket socket;
    private InetAddress address;
    private final int port = 8889;
    private ScheduledExecutorService keepAliveExecutor;
    private TelloStateReceiver receiver;
    private TelloCameraStream stream;

    private boolean isStreamOn = false;

    public TelloClient() {
        try {
            socket = new DatagramSocket(port);
            address = InetAddress.getByName("192.168.10.1");
            receiver = new TelloStateReceiver();
        } catch (SocketException | UnknownHostException e) {
            if (e instanceof SocketException) {
                Log.error("There was an error while creating the socket! (Is the program already running?)");
                e.printStackTrace();
            } else {
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
     *
     * @return True if the Tello has connected, or, is already connected. False if the Tello has failed to connect.
     */
    public boolean connect() {
        //First check to see if we are receiving state packets
        if (hasConnected) return true;
        if (!canRestart) return false;
        receiver.start();
        if (receiver.isOnline()) {
            startKeepAliveThread();
            receiver.receiveAndParsePackets();
            hasConnected = true;

            debug("The TelloStateReceiver is alive and receiving packets. Marked as running.");
            debug("Tello is connected, battery is " + getBattery() + "%");
            return true;
        }
        debug("Receiver is dead. Trying to reconnect...");
        String response = sendPacket("command", "ok", 5_000);
        if (response != null) {
            startKeepAliveThread();
            receiver.receiveAndParsePackets();
            hasConnected = true;
            debug("Tello is connected via a new connection");
            debug("Tello is connected, battery is " + getBattery() + "%");
            return true;
        }
        Log.error("Tello is not connected. Please check the Tello's connection and try again.\n" +
                "Ensure that the Tello is allowed to bypass Windows Firewall settings (this can usually be done by marking it as a private network)");
        isEnabled = false;
        return false;
    }

    private void startKeepAliveThread() {
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor();
        keepAliveExecutor.scheduleAtFixedRate(this::sendKeepAlivePacket, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Sends a keep alive packet to keep the drone connected even if no commands are sent.
     * The keep alive packet is sent every 10 seconds automatically, so you shouldn't usually be calling this method.
     */
    public void sendKeepAlivePacket() {
        if (System.currentTimeMillis() - this.timeOfLastCommand > 13_000) {
            String response = sendPacket("alive", null, 3_000);
            if (response == null) {
                debug("Failed to send keep-alive packet");
                return;
            }
            debug("Sent keep-alive packet");
        }
    }

    private void startCameraStream() {
        this.stream = new TelloCameraStream(this);
        this.stream.start();
    }

    /**
     * Enables the camera stream.
     */
    public void streamOn() {
        if (this.isStreamOn) return;
        sendKeepAlivePacket();
        if (sendPacket("streamon", "ok", 15_000) != null) {
            this.isStreamOn = true;
            startCameraStream();
        }
    }

    public boolean isStreamOn() {
        return isStreamOn;
    }

    /**
     * Disables the camera stream.
     */
    public void streamOff() {
        if (!this.isStreamOn) return;
        sendKeepAlivePacket();
        if (sendPacket("streamoff", "ok", 15_000) != null) {
            this.isStreamOn = false;
            this.stream.stopFetchingFrames();
        }
    }

    /**
     * This will return the current frame from the camera stream as an {@link Mat} object.
     * Please ensure that you call {@link #streamOn} before calling this method.
     *
     * You can use {@link HighGui#imshow(String, Mat)} to display the image.
     *
     * @return The frame from the camera stream.
     */
    public Mat getFrame() {
        return stream.getFrame();
    }


    /**
     * Initiates auto-takeoff.
     */
    public void takeoff() {
        sendKeepAlivePacket();
        String response = sendPacket("takeoff", "ok", 20_000);
        if (response != null) {
            info("Tello took off");
            return;
        }
        info("Failed to takeoff");
    }

    /**
     * Initiates auto-landing.
     */
    public void land() {
        sendKeepAlivePacket();
        String response = sendPacket("land", "ok", 20_000);
        if (response != null) {
            System.out.println("Tello landed");
            return;
        }
        System.out.println("Failed to land");
    }

    /**
     * Moves the drone forward X amount in CM.
     *
     * @param amount The amount of cm to move the drone forward from 20cm to 500cm.
     */
    public void moveForward(int amount) {
        sendKeepAlivePacket();
        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("forward " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Moves the drone backward X amount in CM.
     *
     * @param amount Move the drone backward X amount in CM from 20cm to 500cm.
     */
    public void moveBackward(int amount) {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("back " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Moves the drone right X amount in CM.
     *
     * @param amount The amount of cm to move the drone right from 20cm to 500cm.
     */
    public void moveRight(int amount) {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("right " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Moves the drone left X amount in CM.
     *
     * @param amount The amount of cm to move the drone left from 20cm to 500cm.
     */
    public void moveLeft(int amount) {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("left " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Moves the drone up X amount in CM.
     *
     * @param amount The amount of cm to move the drone up from 20cm to 500cm.
     */
    public void moveUp(int amount) {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("up " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Moves the drone down X amount in CM.
     *
     * @param amount The amount of cm to move the drone down from 20cm to 500cm.
     */
    public void moveDown(int amount) {
        sendKeepAlivePacket();

        if (amount > 500) throw new IllegalArgumentException("Amount must be less than 500");
        if (amount < 20) throw new IllegalArgumentException("Amount must be greater than 20");

        sendPacket("down " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Rotates the drone clockwise X amount in degrees.
     *
     * @param amount The amount of degrees to rotate the drone clockwise from 0 to 3600.
     */
    public void rotateCW(int amount) {
        sendKeepAlivePacket();

        if (amount > 3600) throw new IllegalArgumentException("Amount must be less than 3600");
        if (amount < 1) throw new IllegalArgumentException("Amount must be greater than 1");

        sendPacket("cw " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Rotates the drone counter-clockwise X amount in degrees.
     *
     * @param amount The amount of degrees to rotate the drone counter-clockwise from 0 to 3600.
     */
    public void rotateCWW(int amount) {
        sendKeepAlivePacket();

        if (amount > 3600) throw new IllegalArgumentException("Amount must be less than 3600");
        if (amount < 1) throw new IllegalArgumentException("Amount must be greater than 1");

        sendPacket("ccw " + amount, "ok", calcSpeedTime(amount));
    }

    /**
     * Moves the drone emulating a controller.
     *
     * @param lr  The left-right value of the drone.
     * @param fb  The forward-backward value of the drone.
     * @param ud  The up-down value of the drone.
     * @param yaw The yaw value of the drone.
     */
    public void sendRCControl(int lr, int fb, int ud, int yaw) {
        sendKeepAlivePacket();
        if (lr < -100 || lr > 100) throw new IllegalArgumentException("lr must be between -100 and 100");
        if (fb < -100 || fb > 100) throw new IllegalArgumentException("fb must be between -100 and 100");
        if (ud < -100 || ud > 100) throw new IllegalArgumentException("ud must be between -100 and 100");
        if (yaw < -100 || yaw > 100) throw new IllegalArgumentException("yaw must be between -100 and 100");

        sendPacket("rc " + lr + " " + fb + " " + ud + " " + yaw, "ok", -1);
    }

    /**
     * Please use the {@link FlipDirection} class in order to know which direction to flip
     * Or you could just type it in manually
     *
     * @param direction The direction to flip in
     */
    public void flip(String direction) {
        sendKeepAlivePacket();
        sendPacket("flip " + direction, "ok", 20_000);
    }

    /**
     * Emergency stops the drone. The drone will also disconnect and cannot be reconnected.
     * In order to reconnect you will need to restart the drone.
     */
    public void emergency() {
        sendKeepAlivePacket();
        sendPacket("emergency", null, -1);
        this.disconnect();
        this.canRestart = false;
    }

    /**
     * Set the speed of the drone from 10 to 100.
     *
     * @param speed The speed to set the drone to.
     */
    public void setSpeed(int speed) {
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

    /**
     * Retrieves the current battery percentage from @{link {@link TelloStateReceiver#getCurrentBattery()}}
     *
     * @return The batter in %.
     */
    public int getBattery() {
        return this.receiver.getCurrentBattery();
    }

    /**
     * @return The current TOF
     */
    public int getTOF() {
        return this.receiver.getTof();
    }

    /**
     * Internal method to send a packet to the drone.
     *
     * @return The response from the Tello.
     */
    private String sendPacket(String command, String expectedResponse, int timeout) {
        if (!isEnabled) return null;
        try {
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
                        debug("Got successful (expected) response: \"" + received + "\" for command: \"" + command + "\"");
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
        } catch (IOException e) {
            Log.error("IOException while sending packet: \"" + command + "\" with an expected response \"" + expectedResponse + "\" with a timeout: " + timeout + "ms | GIVING UP!!!");
        }
        return null;
    }

    /**
     * Disconnects the socket and stops the receiver.
     */
    public void disconnect() {
        if (keepAliveExecutor != null) {
            keepAliveExecutor.shutdownNow();
        }
        receiver.stopRunning();
        socket.disconnect();
        this.stream.stopFetchingFrames();
        HighGui.destroyAllWindows();
        System.out.println("Tello disconnected. Goodbye!");
    }

    private int calcSpeedTime(int amount) {
        return ((amount / 15) + 3) * 1000;
    }

    public static class Log {
        private static final boolean IS_DEBUGGING = true;

        public static void debug(String msg) {
            if (!IS_DEBUGGING) return;

            Date date = new Date();
            DateFormat formatter;
            formatter = new SimpleDateFormat("h:mm:ss a");

            formatter.setTimeZone(TimeZone.getDefault());
            String currentTime;
            currentTime = formatter.format(date);
            System.out.println("[DEBUG " + currentTime + "]: " + msg);
        }

        public static void error(String msg) {
            Date date = new Date();
            DateFormat formatter;
            formatter = new SimpleDateFormat("h:mm:ss a");

            formatter.setTimeZone(TimeZone.getDefault());
            String currentTime;
            currentTime = formatter.format(date);
            System.out.println("<!><!> [ERROR " + currentTime + "]: " + msg + " <!><!>");
        }

        public static void info(String msg) {
            Date date = new Date();
            DateFormat formatter;
            formatter = new SimpleDateFormat("h:mm:ss a");

            formatter.setTimeZone(TimeZone.getDefault());
            String currentTime;
            currentTime = formatter.format(date);
            System.out.println("[INFO " + currentTime + "]: " + msg);
        }
    }

    /**
     * This class is used to receive the state of the Tello drone via the UDP socket.
     * This information is passed to provide information about the battery, tof, temperature, and height.
     * This runs in the background on a separate thread, and the information is sent from the Tello drone automatically.
     * <p>
     * This is an internal class, and should not be used directly.
     */
    static final class TelloStateReceiver extends Thread {
        private DatagramSocket socket;
        private final byte[] buf = new byte[256];
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
                    //Receive 1st state packet
                    this.receiveAndParsePackets();
                    debug("Tello state receiver started");
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
                if (!running) return;
                error("Error receiving packet from Tello");
                e.printStackTrace();
            }
        }

        private void parseResponse(String response) {
            String[] data = response.split(";");
            for (String s : data) {
                if (s.isBlank()) continue;

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

        public boolean isOnline() {
            try {
                DatagramPacket packet
                        = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                return true;
            } catch (IOException exception) {
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

    static class TelloCameraStream extends Thread {
        private boolean isRunning = true;

        private Mat frame = new Mat();
        private final TelloClient client;
        ToOrgOpenCvCoreMat converter = new ToOrgOpenCvCoreMat();
        private FFmpegFrameGrabber frameGrabber;

        public TelloCameraStream(TelloClient client){
            this.client = client;

            try {
                //Create a new FFmpegFrameGrabber to grab the video stream from the Tello stream
                frameGrabber = new FFmpegFrameGrabber("udp://@0.0.0.0:11111");
                frameGrabber.start();

            } catch (FFmpegFrameGrabber.Exception e) {
                error("Error starting frame grabber");
                e.printStackTrace();
            }

            if (!frameGrabber.hasVideo()) {
                error("Failed to open camera");
                return;
            }
            info("Tello camera stream started");
        }

        public void run() {
            while (isRunning && client.isStreamOn) {
                fetchFrame();
            }
        }

        private void fetchFrame() {

            try {
                //Convert the frame to a Mat
                frame = converter.convert(frameGrabber.grab());
            } catch (FFmpegFrameGrabber.Exception e) {
                error("Error fetching frame from grabber");
                e.printStackTrace();
            }
        }

        /**
         * @return Returns the decoded frame as a string
         */
        public Mat getFrame() {
            return frame;
        }

        public void stopFetchingFrames() {
            isRunning = false;
            try {
                frameGrabber.stop();
            } catch (FFmpegFrameGrabber.Exception e) {
                error("Error stopping frame grabber");
            }
        }

        public boolean isRunning() {
            return isRunning;
        }
    }
}
