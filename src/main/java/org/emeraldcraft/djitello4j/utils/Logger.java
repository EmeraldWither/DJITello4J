package org.emeraldcraft.djitello4j.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Logger {
    private static final boolean IS_DEBUGGING = true;

    public static void info(String msg){
        Date date = new Date();
        DateFormat formatter;
        formatter = new SimpleDateFormat("h:mm:ss a");

        formatter.setTimeZone(TimeZone.getDefault());
        String currentTime;
        currentTime = formatter.format(date);
        System.out.println("[INFO " + currentTime + "]: " + msg);
    }
    public static void debug(String msg){
        if(!IS_DEBUGGING) return;

        Date date = new Date();
        DateFormat formatter;
        formatter = new SimpleDateFormat("h:mm:ss a");

        formatter.setTimeZone(TimeZone.getDefault());
        String currentTime;
        currentTime = formatter.format(date);
        System.out.println("[DEBUG " + currentTime + "]: " + msg);
    }
    public static void error(String msg){
        Date date = new Date();
        DateFormat formatter;
        formatter = new SimpleDateFormat("h:mm:ss a");

        formatter.setTimeZone(TimeZone.getDefault());
        String currentTime;
        currentTime = formatter.format(date);
        System.out.println("<!><!> [ERROR " + currentTime + "]: " + msg +  " <!><!>");
    }
}
