package dslab.transfer.tcp.dmtp;

import dslab.protocol.dmtp.DMTPProtocol;
import dslab.protocol.dmtp.DefaultDMTPParser;
import dslab.transfer.MessageForwardingListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;

public class DMTPThread implements Runnable {

    private final Socket socket;
    private final MessageForwardingListener forwarder;
    private static final Log LOG = LogFactory.getLog(DMTPThread.class);

    public DMTPThread(Socket socket, MessageForwardingListener forwarder) {
        this.socket = socket;
        this.forwarder = forwarder;
    }

    public void run() {
        try {
            DMTPProtocol protocol = new DMTPProtocol(new DefaultDMTPParser(), socket);

            while (!socket.isClosed()) {
                forwarder.put(protocol.process());
                LOG.info("Added message to forwarding queue");
            }
        } catch (SocketException e) {
            LOG.warn("SocketException while handling socket: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            LOG.warn("InterruptException during putting: " + e.getMessage());
        } catch (SecurityException e) {
            LOG.warn(e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
