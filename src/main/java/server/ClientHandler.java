package server;

import shared.Constants;
import shared.Parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ForkJoinPool;

class ClientHandler implements Runnable {
    private final Server server;
    private SelectionKey key;
    private final SocketChannel clientChannel;
    private final ByteBuffer clientCommand = ByteBuffer.allocateDirect(Constants.CONTENT_MAX_LENGTH);
    private final Parser parser = new Parser();
    private final Controller controller;
    private final Queue<Response> responseQueue = new ArrayDeque<>();

    ClientHandler (Server server, SocketChannel clientChannel) {
        this.server = server;
        this.clientChannel = clientChannel;
        this.controller = new Controller();
    }

    void setKey (SelectionKey key) {
        this.key = key;
    }

    @Override
    public void run () {
        while (parser.hasNext()) {
            try {
                String command = parser.next();
                passCommand(command);
            } catch (ErroneousInputException e) {
                addResponse(new Response(Parser.ContentType.INFO, e.getMessage()));
            }
        }
    }

    private void respond () {
        this.server.addToQueue(this.key);
    }

    void receiveCommand () throws IOException {
        clientCommand.clear();
        int numReadBytes = clientChannel.read(clientCommand);
        if (numReadBytes == -1) {
            throw new IOException("Client disconnected");
        }
        String content = Parser.extractFromBuffer(clientCommand);
        parser.add(content);
        ForkJoinPool.commonPool().execute(this);
    }

    void sendAll () throws IOException {
        Response response = null;
        synchronized (responseQueue) {
            while ((response = responseQueue.peek()) != null) {
                sendResponse(response);
                responseQueue.remove();
            }
        }
    }

    private void sendResponse (Response response) throws IOException {
        ByteBuffer data = this.server.createBuffer(response.getType(), response.getContent());
        clientChannel.write(data);
        if (data.hasRemaining()) {
            throw new IOException("Could not send response");
        }
    }

    void disconnectClient () throws IOException {
        clientChannel.close();
    }

    private void passCommand(String command) throws ErroneousInputException {
        Response response = controller.parseCommand(command);
        addResponse(response);
    }

    private synchronized void addResponse(Response response) {
        responseQueue.add(response);
        respond();
    }

}
