package bg.sofia.uni.fmi.mjt.todoist.server;

import bg.sofia.uni.fmi.mjt.todoist.server.logger.Logger;
import bg.sofia.uni.fmi.mjt.todoist.server.logger.LoggerFileImplementation;
import bg.sofia.uni.fmi.mjt.todoist.server.logger.vo.LoggerLevel;
import bg.sofia.uni.fmi.mjt.todoist.server.nio.ServerNio;
import bg.sofia.uni.fmi.mjt.todoist.server.nio.ServerNioAPI;

public class ServerMain {

    private static final Logger LOGGER = new LoggerFileImplementation(LoggerLevel.ERROR);

    public static void main(String[] args) {
        runServer();
    }

    private static void runServer() {
        ServerNioAPI nioServer = new ServerNio(LOGGER);
        Thread server = new Thread(nioServer);
        server.start();
    }

}