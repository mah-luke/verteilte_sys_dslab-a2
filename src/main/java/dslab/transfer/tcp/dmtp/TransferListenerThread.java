package dslab.transfer.tcp.dmtp;

import dslab.transfer.MessageForwardingListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferListenerThread extends Thread {
    private final ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final MessageForwardingListener forwarder;
    private final Log LOG = LogFactory.getLog(TransferListenerThread.class);

    public TransferListenerThread(ServerSocket serverSocket, MessageForwardingListener forwarder) {
        this.serverSocket = serverSocket;
        this.forwarder = forwarder;
    }

    public void run() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                LOG.info("Accepted connection on socket: " + socket);
                executor.execute(new DMTPThread(socket, forwarder));
            }
        } catch (SocketException e) {
            LOG.warn("Stopping listener because Server Socket was closed");
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }
}
