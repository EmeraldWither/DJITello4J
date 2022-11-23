package org.emeraldcraft.djitello4j.background;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter.ToOrgOpenCvCoreMat;
import org.emeraldcraft.djitello4j.Tello;
import org.emeraldcraft.djitello4j.utils.Logger;

public class TelloCameraStream extends Thread {
    private boolean isRunning = false;

    private org.opencv.core.Mat frame = new org.opencv.core.Mat();
    private final Tello drone;
    final ToOrgOpenCvCoreMat converter = new ToOrgOpenCvCoreMat();
    private FFmpegFrameGrabber frameGrabber;

    public TelloCameraStream(Tello drone){
        this.drone = drone;

        try {
            //Create a new FFmpegFrameGrabber to grab the video stream from the Tello stream
            frameGrabber = new FFmpegFrameGrabber("udp://@0.0.0.0:11111");
            frameGrabber.start();

        } catch (FFmpegFrameGrabber.Exception e) {
            Logger.error("Error starting frame grabber");
            e.printStackTrace();
        }

        if (!frameGrabber.hasVideo()) {
            Logger.error("Failed to open camera");
            return;
        }
        Logger.info("Tello camera stream started");
        isRunning = true;
    }

    public void run() {
        while (isRunning && drone.isStreamOn()) {
            fetchFrame();
        }
    }

    private void fetchFrame() {

        try {
            //Convert the frame to a Mat
            frame = converter.convert(frameGrabber.grab());
        } catch (FFmpegFrameGrabber.Exception e) {
            Logger.error("Error fetching frame from grabber");
            e.printStackTrace();
        }
    }

    /**
     * @return Returns the decoded frame as a string
     */
    public org.opencv.core.Mat getFrame() {
        if(frame.empty()) return null;
        return frame;
    }

    public void stopFetchingFrames() {
        isRunning = false;
        try {
            frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            Logger.error("Error stopping frame grabber");
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
