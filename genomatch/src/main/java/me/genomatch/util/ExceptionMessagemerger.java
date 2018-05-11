package me.genomatch.util;

public class ExceptionMessagemerger {
    public static String mergeMessage(Exception e) {
        if(e == null) {
            return "No Exception";
        }
        StringBuilder builder = new StringBuilder();

        // Print our stack trace
        builder.append(e.getClass().getCanonicalName()).append(":").append(e.getMessage()).append("\n");
        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            builder.append("\tat ").append(traceElement).append("\n");
        }
        return builder.toString();
    }

}
