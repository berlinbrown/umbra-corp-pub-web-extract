package com.umbra.social.server;

import org.eclipse.jetty.server.Server;

public class JettyServerMainDriver {

    public static void main(String[] args) throws Exception {
        // Create a Jetty server on port 8080
        final Server server = new Server(8080);

        // Start the server and join the thread
        server.start();
        server.join();
    }
}
