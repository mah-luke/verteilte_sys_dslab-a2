package dslab.transfer;

import java.io.*;
import java.net.ServerSocket;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.transfer.tcp.dmtp.TransferListenerThread;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransferServer implements ITransferServer, Runnable {

    private final Config config;
    private ServerSocket serverSocket;
    private TransferListenerThread listener;
    private final String componentId;
    private final InputStream in;
    private final PrintStream out;
    private Shell shell;
    private MessageForwardingListener forwarder;
    private final Log LOG = LogFactory.getLog(TransferServer.class);

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.componentId = componentId;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        LOG.info("TransferServer starting...");
        try {
            serverSocket = new ServerSocket(config.getInt("tcp.port"));

            // Create a forwarder that handles all outgoing messages.
            forwarder = new MessageForwardingListener(serverSocket.getInetAddress().getHostAddress(), config);

            // each TransferServer has its own forwarding thread.
            // If the forwarding becomes a bottleneck, the single thread can be replaced by a ThreadPool.
            forwarder.start();

            // Create a listener that creates new Sockets in seperate threads as needed.
            listener = new TransferListenerThread(serverSocket, forwarder);

            // each TransferServer has a single listener thread that accepts new connections.
            listener.start();

            LOG.info("Socket is up, forwarder and listener started");
        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating server socket", e);
        }

        LOG.info("Starting shell...");
        // start a shell to interact with the server while it is running.
        shell = new Shell(in, out);
        shell.register(this);
        shell.run();

        LOG.info("TransferServer shutdown complete");
    }

    @Override
    @Command
    public void shutdown() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.error("Exception while closing server socket: " + e.getMessage());
            }
        }
        // ASK: this ok? (else SHell in external class and calls server)
        if (forwarder != null && forwarder.isAlive()) forwarder.shutdown();
        if (shell != null){
            throw new StopShellException();
        }
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

}
