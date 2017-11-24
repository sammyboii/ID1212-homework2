package client;

import shared.Constants;
import shared.Listener;
import shared.Parser;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class Net implements Runnable {
    private final ByteBuffer serverResponse = ByteBuffer.allocateDirect(Constants.MAX_RESPONSE_LENGTH);
    private final Queue<ByteBuffer> commandQueue = new ArrayDeque<>();
    private Listener listener;
    private final Parser parser = new Parser();
    private InetSocketAddress serverAddress;
    private SocketChannel socketChannel;
    private Selector selector;
    private boolean connected = false;
    private volatile boolean shouldSend = false;

    boolean isConnected() {
        return this.connected;
    }

    void connect (String host, int port, Listener observer) {
        serverAddress = new InetSocketAddress(host, port);
        listener = observer;
        new Thread(this).start();
    }

    void disconnect () throws IOException, ClassNotFoundException {
        this.connected = false;
        sendCommand("quit");
    }

    @Override
    public void run() {
        try {
            initializeConnection();
            initializeSelector();
            while (connected || !commandQueue.isEmpty()) {
                if (shouldSend) {
                    socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    shouldSend = false;
                }
                selector.select();
                processKeys();
            }
        } catch (Exception e) {
            System.err.println("An error occured trying to communicate with the server.");
        }
        try {
            requestDisconnect();
        } catch (IOException ioex) {
            System.err.println("An error occured trying to disconnect from the server.");
        }
    }

     void sendCommand(String command) throws IOException, ClassNotFoundException {
        String content = Parser.setLengthHeader(command);
        synchronized (commandQueue) {
            commandQueue.add(ByteBuffer.wrap(content.getBytes()));
        }
        shouldSend = true;
        selector.wakeup();
    }

    private void send (SelectionKey key) throws IOException {
        ByteBuffer cmd;
        synchronized (commandQueue) {
            while ((cmd = commandQueue.peek()) != null) {
                socketChannel.write(cmd);
                if (cmd.hasRemaining()) {
                    return;
                }
                commandQueue.remove();
            }
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void receive (SelectionKey key) throws IOException {
        serverResponse.clear();
        int numReadBytes = socketChannel.read(serverResponse);
        if (numReadBytes == -1) {
            throw new IOException("Error getting server response");
        }
        String received = Parser.extractFromBuffer(serverResponse);
        parser.add(received);
        while (parser.hasNext()) {
            String content = parser.next();
            if (!Parser.recognizesType(content)) {
                throw new IOException("Error reading server response");
            }
            notifyResponseReceived(content);
        }
    }

    private void notifyConnectionDone(InetSocketAddress connectedAddress) {
        Executor pool = ForkJoinPool.commonPool();
        pool.execute(new Runnable() {
            @Override
            public void run() {
                listener.connected(connectedAddress);
            }
        });
    }

    private void notifyDisconnectionDone() {
        Executor pool = ForkJoinPool.commonPool();
        pool.execute(new Runnable() {
            @Override
            public void run() {
                listener.disconnected();
            }
        });
    }

    private void notifyResponseReceived (String content) {
        Executor pool = ForkJoinPool.commonPool();
        pool.execute(new Runnable() {
            @Override
            public void run() {
                listener.receivedContent(content);
            }
        });
    }

    private void initializeConnection () throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(serverAddress);
        connected = true;
    }

    private void initializeSelector () throws IOException {
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void processKeys () throws IOException {
        for (SelectionKey key : selector.selectedKeys()) {
            selector.selectedKeys().remove(key);
            if (!key.isValid()) {
                continue;
            }
            if (key.isConnectable()) {
                completeConnection(key);
            } else if (key.isReadable()) {
                receive(key);
            } else if(key.isWritable()) {
                send(key);
            }
        }
    }

    private void requestDisconnect () throws IOException {
        socketChannel.close();
        socketChannel.keyFor(selector).cancel();
        notifyDisconnectionDone();
    }

    private void completeConnection (SelectionKey key) throws IOException {
        socketChannel.finishConnect();
        key.interestOps(SelectionKey.OP_READ);
        try {
            notifyConnectionDone((InetSocketAddress) socketChannel.getRemoteAddress());
        } catch (IOException ioex) {
            notifyConnectionDone(serverAddress);
        }
    }
}
