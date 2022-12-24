package org.emeraldcraft.djitello4j.background;

import org.emeraldcraft.djitello4j.Tello;
import org.emeraldcraft.djitello4j.components.TelloCommand;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TelloKeepAliveHandler {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Tello drone;

    public TelloKeepAliveHandler(Tello drone) {
        this.drone = drone;
    }

    public void start() {
        if(!drone.isEnabled()) {
            stop();
            return;
        }
        executor.scheduleAtFixedRate(
                () -> drone.sendCustomCommand(new TelloCommand("alive", "ok", 5000)),
                0, 15, TimeUnit.SECONDS);
    }
    public void stop() {
        executor.shutdown();
    }
}
