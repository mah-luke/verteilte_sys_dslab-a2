package dslab.mailbox;

import java.io.*;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.entity.MailEntity;
import dslab.mailbox.persistence.MailStorage;
import dslab.mailbox.tcp.dmap.MailboxDMAPListenerThread;
import dslab.mailbox.tcp.dmtp.MailboxDMTPListenerThread;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.exception.AlreadyRegisteredException;
import dslab.nameserver.exception.InvalidDomainException;
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
    private Registry registry;
    private INameserverRemote nameserver;
    private final static Log LOG = LogFactory.getLog(MailboxServer.class);


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
            // DMTP server
            serverSocket = new ServerSocket(config.getInt("dmtp.tcp.port"));
            listener = new MailboxDMTPListenerThread(serverSocket,  config, storage);
            listener.start();
            LOG.info("Mailbox DMTP serverSocket is up: " + serverSocket.getLocalSocketAddress());

            // DMAP server
            dmapServerSocket = new ServerSocket(config.getInt("dmap.tcp.port"));
            dmapListener = new MailboxDMAPListenerThread(dmapServerSocket, config, storage);
            dmapListener.start();
            LOG.info("Mailbox DMAP serverSocket is up: " + dmapServerSocket.getLocalSocketAddress());
        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating server socket", e);
        }

        try {
            registry = LocateRegistry.getRegistry(
                    config.getString("registry.host"),
                    config.getInt("registry.port")
            );

            nameserver = (INameserverRemote) registry.lookup(config.getString("root_id"));

            LOG.info("Nameserver found");

            nameserver.registerMailboxServer(config.getString("domain"),
                    serverSocket.getLocalSocketAddress() + ":" + serverSocket.getLocalPort());

            LOG.info("Registration on Nameserver successful");
        } catch (RemoteException e) {
            LOG.warn("Commmunication with registry not possible", e);
        } catch (NotBoundException e) {
            LOG.warn("Nameserver not found", e);
        } catch (AlreadyRegisteredException | InvalidDomainException e) {
            LOG.warn("Registration on Nameserver not possible", e);
        }

        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + " >");

        LOG.info("Startup complete. Starting shell...");
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
