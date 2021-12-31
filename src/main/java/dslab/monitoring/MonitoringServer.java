package dslab.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.util.AbstractMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.monitoring.persistence.MonitoringStorage;
import dslab.monitoring.udp.MonitoringThread;
import dslab.transfer.tcp.dmtp.DMTPClientThread;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MonitoringServer implements IMonitoringServer, Runnable {

    private final Config config;
    private final InputStream in;
    private final PrintStream out;
    private final String componentId;
    private DatagramSocket datagramSocket;
    private MonitoringThread listener;
    private Shell shell;
    private final MonitoringStorage storage;
    private final Log LOG = LogFactory.getLog(MonitoringServer.class);

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.componentId = componentId;
        this.in = in;
        this.out = out;

        this.storage = new MonitoringStorage();
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(config.getInt("udp.port"));
            listener =  new MonitoringThread(datagramSocket, storage);
            listener.start();
            LOG.info("Monitoring server is up: " + datagramSocket.getLocalSocketAddress());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot listen on UDP port.", e);
        }

        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + " >");
        shell.run();
        LOG.info("Finished shutdown");
    }

    @Override
    @Command
    public void addresses() {
        AbstractMap<String, Long> addresses;
        synchronized (addresses = storage.getAddresses()) {
            if (addresses.size() == 0) shell.out().println("No addresses logged");
            else for (String address : addresses.keySet()) shell.out().printf("%s %d\n", address, addresses.get(address));
        }
    }

    @Override
    @Command
    public void servers() {
        AbstractMap<String, Long> servers;
        synchronized (servers = storage.getServers()) {
            if (servers.size() == 0) shell.out().println("No servers logged");
            else for (String server : servers.keySet()) shell.out().printf("%s %d\n", server, servers.get(server));
        }
    }

    @Override
    @Command
    public void shutdown() {
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
        }

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }



}
