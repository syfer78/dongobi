package me.genomatch.http;

import me.genomatch.http.handler.DispatchHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import java.util.logging.Logger;

public class HttpServer {
    private static final Logger logger = Logger.getLogger(HttpServer.class.getCanonicalName());
    private int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        Server server = new Server(port);
        Handler handler = new DispatchHandler();
        server.setHandler(handler);
        try {
            server.start();
        } catch (Exception e) {
            logger.severe("[" + this.getClass().getSimpleName() + "]" + e.getMessage());
            server.stop();
        }
        server.join();
    }
}
