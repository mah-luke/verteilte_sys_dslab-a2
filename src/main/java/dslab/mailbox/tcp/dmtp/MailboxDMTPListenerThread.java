package dslab.mailbox.tcp.dmtp;

import dslab.mailbox.persistence.MailStorage;
import dslab.mailbox.tcp.dmap.MailboxDMAPListenerThread;
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

public class MailboxDMTPListenerThread extends Thread{

    private final ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Config config;
    private final MailStorage storage;
    private final Log LOG = LogFactory.getLog(MailboxDMAPListenerThread.class);

    public MailboxDMTPListenerThread(ServerSocket serverSocket, Config config, MailStorage storage) {
        this.serverSocket = serverSocket;
        this.storage = storage;
        this.config = config;
    }

    @Override
    public void run() {

        try {
            while (!serverSocket.isClosed()) {
                LOG.debug("Listening for new connections...");
                Socket socket = serverSocket.accept();
                LOG.info("Accepted connection on socket: " + socket);
                executor.execute(new MailboxDMTPThread(socket, config, storage));
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
