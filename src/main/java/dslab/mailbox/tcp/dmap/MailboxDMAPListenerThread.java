package dslab.mailbox.tcp.dmap;

import dslab.mailbox.persistence.MailStorage;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailboxDMAPListenerThread extends Thread {

    private final ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Log LOG = LogFactory.getLog(MailboxDMAPListenerThread.class);
    private final MailStorage storage;
    private final Config config;

    public MailboxDMAPListenerThread(ServerSocket serverSocket, Config config, MailStorage storage) {
        this.config = config;
        this.storage = storage;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                LOG.info("Accepted socket: " + socket);
                executor.execute(new MailboxDMAPThread(socket, config, storage));
            }
        } catch (SocketException e) {
            LOG.info("Stopping listener because Server Socket was closed.");
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }
}
