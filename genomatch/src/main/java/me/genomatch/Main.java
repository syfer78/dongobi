package me.genomatch;

import me.genomatch.http.HttpServer;
import me.genomatch.util.LogFormatter;

import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) throws Exception {
        String logDir = System.getProperty("genomatch.log.home");
        if(logDir == null) {
            logDir = "%t";
        }
        FileHandler fh = new FileHandler(logDir + "/genomatch.log", true);
        fh.setFormatter(new LogFormatter());
        Logger.getLogger("").addHandler(fh);
        Logger.getLogger(Main.class.getCanonicalName()).info("Server Started");
        HttpServer server = new HttpServer(8080);
        server.run();
        Logger.getLogger(Main.class.getCanonicalName()).info("Server Stopped");
    }
}
