package com.lee.dexdiff.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jianglee on 7/30/16.
 */
public final class Logger {
    /**
     * [LEVEL] THREAD : [TAG] MESSAGE
     */
    private static final String TEMPLATE = "[ %s ] %s(Thread-%s) : %s";

    private static final Map<Integer, String> HumanLevel = new HashMap<Integer, String>();

    static {
        HumanLevel.put(0, "DEBUG");
        HumanLevel.put(1, "INFO");
        HumanLevel.put(2, "WARN");
        HumanLevel.put(3, "ERROR");
    }

    private static int sLevel = 1;

    private static ILogger sLogger = new ILogger() {

        @Override
        public void println(String log) {
            System.out.println(log);
        }
    };

    public static void setLevel(int level) {
        if (!HumanLevel.containsKey(level)) {
            throw new IllegalArgumentException("level unsupported.");
        }
        sLevel = level;
    }

    public static void setLogger(ILogger logger) {
        if (null != logger) {
            sLogger = logger;
        }
    }

    public static void d(String tag, String msg) {
        if (sLevel <= 0) {
            log(0, tag, msg);
        }
    }

    public static void d(String tag, Throwable msg) {
        if (sLevel <= 0) {
            log(0, tag, stringfy(msg));
        }
    }

    public static void i(String tag, String msg) {
        if (sLevel <= 1) {
            log(1, tag, msg);
        }
    }

    public static void i(String tag, Throwable msg) {
        if (sLevel <= 1) {
            log(1, tag, stringfy(msg));
        }
    }

    public static void w(String tag, String msg) {
        if (sLevel <= 2) {
            log(2, tag, msg);
        }
    }

    public static void w(String tag, Throwable msg) {
        if (sLevel <= 2) {
            log(2, tag, stringfy(msg));
        }
    }

    public static void e(String tag, String msg) {
        if (sLevel <= 3) {
            log(3, tag, msg);
        }
    }

    public static void e(String tag, Throwable msg) {
        if (sLevel <= 3) {
            log(3, tag, stringfy(msg));
        }
    }

    private static String stringfy(Throwable e) {
        return "{ class = " + e.getClass().getName() + ", message = " + e.getMessage() + "}";
    }

    private static void log(int level, String tag, String msg) {
        sLogger.println(String.format(TEMPLATE, HumanLevel.get(level), tag, Thread.currentThread().getId(), msg));
    }

    public interface ILogger {
        void println(String log);
    }
}
