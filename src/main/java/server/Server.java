package server;

import shared.Constants;
import shared.Parser;
import shared.Parser.ContentType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.StringJoiner;

public class Server {
    private static final int LINGER_TIME = 5000;
    private final Queue<SelectionKey> willingToRespond = new ArrayDeque<>();
    private int portNo = 1337;
    private Selector selector;
    private ServerSocketChannel listeningSocketChannel;
    private volatile boolean shouldRespond = false;

    public static void main(String[] args) {
        new Server().serve();
    }

    ByteBuffer createBuffer (ContentType type, String content) {
        StringJoiner joiner = new StringJoiner(Constants.CONTENT_TYPE_DELIMITER);
        joiner.add(type.toString());
        joiner.add(content);
        String preparedContent = Parser.setLengthHeader(joiner.toString());
        return ByteBuffer.wrap(preparedContent.getBytes());
    }

    private synchronized SelectionKey nextClientKey () {
        return willingToRespond.poll();
    }

    private void serve () {
        try {
            initializeSelector();
            initializeListeningSocketChannel();
            System.out.println("Running on port " + portNo);
            while (true) {
                if (shouldRespond) {
                    SelectionKey key = nextClientKey();
                    if (key.isValid() && key.channel() instanceof SocketChannel) {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    shouldRespond = false;
                }
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        startHandler(key);
                    }
                    else if (key.isReadable()) {
                        receive(key);
                    }
                    else if (key.isWritable()) {
                        send(key);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("An error occurred trying to serve");
            e.printStackTrace();
        }
    }

    private void receive (SelectionKey key) throws IOException {
        ClientHandler handler = (ClientHandler) key.attachment();
        try {
            handler.receiveCommand();
        } catch (IOException ioex) {
            removeClient(key);
        }
    }

    private void send (SelectionKey key) throws IOException {
        ClientHandler handler = (ClientHandler) key.attachment();
        try {
            handler.sendAll();
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException ioex) {
            removeClient(key);
        }
    }

    private void removeClient(SelectionKey key) throws IOException {
        ClientHandler handler = (ClientHandler) key.attachment();
        handler.disconnectClient();
        key.cancel();
    }

    void addToQueue (SelectionKey key) {
        shouldRespond = true;
        synchronized (willingToRespond) {
            willingToRespond.add(key);
        }
        selector.wakeup();
    }

    private void initializeSelector () throws IOException {
        selector = Selector.open();
    }

    private void initializeListeningSocketChannel () throws IOException {
        listeningSocketChannel = ServerSocketChannel.open();
        listeningSocketChannel.configureBlocking(false);
        listeningSocketChannel.bind(new InetSocketAddress(portNo));
        listeningSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void startHandler (SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        ClientHandler handler = new ClientHandler(this, clientChannel);
        SelectionKey newKey = clientChannel.register(selector, SelectionKey.OP_READ, handler);
        handler.setKey(newKey);
        clientChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER_TIME);
    }
}
