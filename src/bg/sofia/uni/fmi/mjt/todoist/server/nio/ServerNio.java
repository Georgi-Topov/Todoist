package bg.sofia.uni.fmi.mjt.todoist.server.nio;

import bg.sofia.uni.fmi.mjt.todoist.client.command.CommandCreator;
import bg.sofia.uni.fmi.mjt.todoist.client.command.CommandExecutor;
import bg.sofia.uni.fmi.mjt.todoist.client.command.exception.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.todoist.client.command.exception.UserNotAuthenticatedException;
import bg.sofia.uni.fmi.mjt.todoist.server.logger.Logger;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.TaskRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.CollaborationAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.CollaborationDoesNotExistException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskOperationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.CollaborationFileRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.TaskFileRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.FailedAuthorizationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.IllegalCollaborationNameException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.IllegalTaskAssignmentException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.UserAlreadyInCollaborationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.UserNotInCollaborationException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.UserLoginRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.IllegalUsernameException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserAlreadyLoggedInException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserDoesNotExistException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class ServerNio implements ServerNioAPI {

    private static final int BUFFER_SIZE = 2048;
    private static final String HOST = "localhost";
    private static final int PORT = 7777;

    private final int port;
    private final Logger logger;

    private boolean isServerWorking;
    private ByteBuffer buffer;
    private Selector selector;

    public ServerNio(Logger logger) {
        this.logger = logger;
        this.port = PORT;
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            selector = Selector.open();
            configureServerSocketChannel(serverSocketChannel, selector);
            this.buffer = ByteBuffer.allocate(BUFFER_SIZE);

            isServerWorking = true;
            while (isServerWorking) {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    try {
                        if (key.isReadable()) {
                            SocketChannel clientChannel = (SocketChannel) key.channel();

                            String clientInput = getClientInput(clientChannel);
                            System.out.println(clientInput);
                            if (clientInput == null) {
                                continue;
                            }

                            String output = handleRequest(key, clientInput);

                            writeClientOutput(clientChannel, output);

                        } else if (key.isAcceptable()) {
                            accept(key);
                        }

                        keyIterator.remove();
                    } catch (IOException e) {
                        key.channel().close();
                        keyIterator.remove();
                        System.out.println("Error occurred while processing client request: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to start server", e);
        }
    }

    @Override
    public void stop() {
        this.isServerWorking = false;
        if (selector.isOpen()) {
            selector.wakeup();
        }
    }

    private void configureServerSocketChannel(ServerSocketChannel channel, Selector selector) throws IOException {
        channel.bind(new InetSocketAddress(HOST, this.port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private String getClientInput(SocketChannel clientChannel) throws IOException {
        buffer.clear();

        int readBytes = clientChannel.read(buffer);
        if (readBytes < 0) {
            clientChannel.close();
            return null;
        }

        buffer.flip();

        byte[] clientInputBytes = new byte[buffer.remaining()];
        buffer.get(clientInputBytes);

        return new String(clientInputBytes, StandardCharsets.UTF_8);
    }

    private void writeClientOutput(SocketChannel clientChannel, String output) throws IOException {
        buffer.clear();
        buffer.put(output.getBytes());
        buffer.flip();

        clientChannel.write(buffer);
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = sockChannel.accept();

        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ,
                new CommandExecutor(new UserLoginRepository(), new TaskRepository(),
                        new TaskFileRepository(), new CollaborationFileRepository()));
    }

    private String handleRequest(SelectionKey key, String clientInput) {
        String output;

        try {
            output = ((CommandExecutor) key.attachment()).execute(CommandCreator.newCommand(clientInput));
        } catch (InvalidCommandException e) {
            logger.error(e);
            output = """
                    The format of the request is not valid!
                    To see the correct format type: "help".
                    If you still have an issue check the documentation for that command
                    """;
        } catch (UserNotAuthenticatedException e) {
            logger.error(e);
            output = """ 
                    Unsuccessful operation!
                    Login to be able to use other commands. To see the actual command, type "help"
                    """;
        } catch (CollaborationAlreadyExistsException | UserAlreadyLoggedInException | IllegalTaskOperationException |
                 FailedAuthorizationException | IllegalTaskDatesException | UserNotInCollaborationException |
                 UserAlreadyInCollaborationException | IllegalTaskAssignmentException | IllegalUsernameException |
                 UserDoesNotExistException | UserAlreadyExistsException | IllegalCollaborationNameException |
                 CollaborationDoesNotExistException e) {
            logger.error(e);
            output = e.getMessage();
        }

        return output;
    }

}