package shared;

import java.net.InetSocketAddress;

public interface Listener {
    void connected(InetSocketAddress address);

    void disconnected();

   void receivedContent(String content);
}
