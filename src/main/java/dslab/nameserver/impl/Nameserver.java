package dslab.nameserver.impl;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.monitoring.MonitoringServer;
import dslab.nameserver.INameserver;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.entity.NameserverEntity;
import dslab.nameserver.exception.AlreadyRegisteredException;
import dslab.nameserver.exception.InvalidDomainException;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Nameserver implements INameserver {

    private final NameserverEntity entity;
    private final Shell shell;
    private  final INameserverRemote remote;
    private final Log LOG = LogFactory.getLog(this.getClass().getName());

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public Nameserver(String componentId, Config config, InputStream in, PrintStream out) throws Exception {
        // TODO
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + " >");

        entity = NameserverEntity.Builder.getInstance()
                .setComponentId(componentId)
                .setConfig(config)
                .build();

        remote = new NameserverRemote(this, entity);
    }

    @Override
    public void run() {
        // TODO

        Config config = entity.getConfig();

        try {
        // is server root?
            if (!config.containsKey("domain")) {
                Registry registry = LocateRegistry.createRegistry(config.getInt("registry.port"));

                // bind remote to registry
                try {
                    INameserverRemote exported = (INameserverRemote) UnicastRemoteObject.exportObject(remote, 0);
                    registry.bind(config.getString("root_id"), remote);
                } catch (AlreadyBoundException e) {
                    throw new RuntimeException(e);
                }
            }
            // zone
            else {
                // lookup root Nameserver and register remote
                Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
                try {
                    String rootId = config.getString("root_id");
                    LOG.info(Arrays.toString(registry.list()));
                    Remote r = registry.lookup(rootId);
                    INameserverRemote root = (INameserverRemote) r;
                    try {
                        root.registerNameserver(config.getString("domain"), remote);
                    } catch (AlreadyRegisteredException | InvalidDomainException e) {
                        LOG.error(e);
                    }
                } catch (NotBoundException e) {
                    LOG.error("No root server found! " + e);
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }




        LOG.info("Starting shell...");

        shell.run();

        LOG.info("Shell stopped");
    }

    @Override
    @Command
    public void nameservers() {
        // TODO
    }

    @Override
    @Command
    public void addresses() {
        // TODO
    }

    @Override
    @Command
    public void shutdown() {
        // TODO

        try {
            UnicastRemoteObject.unexportObject(remote, true);
        } catch (NoSuchObjectException e) {
            System.err.println(e);
        }
    }

    public static void main(String[] args) throws Exception {
        INameserver component = ComponentFactory.createNameserver(args[0], System.in, System.out);
        component.run();
    }
}
