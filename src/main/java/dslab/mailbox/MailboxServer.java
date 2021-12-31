package dslab.mailbox;

import java.io.*;
import java.net.ServerSocket;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.mailbox.persistence.MailStorage;
import dslab.mailbox.tcp.dmap.MailboxDMAPListenerThread;
import dslab.mailbox.tcp.dmtp.MailboxDMTPListenerThread;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MailboxServer implements IMailboxServer, Runnable {

    ServerSocket serverSocket;
    ServerSocket dmapServerSocket;
    Shell shell;
    MailboxDMTPListenerThread listener;
    MailboxDMAPListenerThread dmapListener;
    Config config;
    InputStream in;
    PrintStream out;
    String componentId;
    MailStorage storage;
    private final Log LOG = LogFactory.getLog(MailboxServer.class);


    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;

        this.storage = new MailStorage(config.getString("domain"));
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(config.getInt("dmtp.tcp.port"));
            listener = new MailboxDMTPListenerThread(serverSocket,  config, storage);
            listener.start();
            LOG.info("Mailbox DMTP serverSocket is up: " + serverSocket.getLocalSocketAddress());

            dmapServerSocket = new ServerSocket(config.getInt("dmap.tcp.port"));
            dmapListener = new MailboxDMAPListenerThread(dmapServerSocket, config, storage);
            dmapListener.start();
            LOG.info("Mailbox DMAP serverSocket is up: " + dmapServerSocket.getLocalSocketAddress());
        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating server socket", e);
        }


        shell = new Shell(in, out);
        shell.register(this);
        shell.run();

        System.out.println("Finished shutdown");
    }

    @Override
    @Command
    public void shutdown() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.error("Error while closing server socket: " + e.getMessage());
            }

            try {
                dmapServerSocket.close();
            } catch (IOException e) {
                LOG.error("Error while closing server socket: " + e.getMessage());
            }
        }
        if (shell != null) {
            throw new StopShellException();
        }
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
