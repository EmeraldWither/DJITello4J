package org.emeraldcraft.djitello4j.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Logger {
    private static final boolean IS_DEBUGGING = true;

    public static void info(String msg){
        System.out.println("[INFO " + getCurrentTime() + "]: " + msg);
    }
    public static void debug(String msg){
        if(!IS_DEBUGGING) return;
        System.out.println("[DEBUG " + getCurrentTime() + "]: " + msg);
    }
    public static void error(String msg){
        System.out.println("<!><!> [ERROR " + getCurrentTime() + "]: " + msg +  " <!><!>");
    }
    private static String getCurrentTime(){
        Date date = new Date();
        DateFormat formatter;
        formatter = new SimpleDateFormat("h:mm:ss a");

        formatter.setTimeZone(TimeZone.getDefault());
        String currentTime;
        currentTime = formatter.format(date);
        return currentTime;
    }
}
