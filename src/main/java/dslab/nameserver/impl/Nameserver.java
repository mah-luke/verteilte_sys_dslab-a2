package dslab.nameserver.impl;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.nameserver.INameserver;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.entity.NameserverEntity;
import dslab.nameserver.exception.AlreadyRegisteredException;
import dslab.nameserver.exception.InvalidDomainException;
import dslab.util.Config;
import dslab.util.DomainResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Nameserver implements INameserver, Runnable {

    private final NameserverEntity entity;
    private final Shell shell;
    private INameserverRemote remote;
    private INameserverRemote exported;
    private INameserverRemote root;
    private final Log LOG = LogFactory.getLog(this.getClass().getName());

    private Registry registry;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public Nameserver(String componentId, Config config, InputStream in, PrintStream out) {
        entity = NameserverEntity.Builder.getInstance()
                .setComponentId(componentId)
                .setConfig(config)
                .build();

        shell = new Shell(in, out);
    }

    @Override
    public void run() {

        Config config = entity.getConfig();

        // root
        if (!config.containsKey("domain")) {

            // bind remote to registry
            try {
                registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
                remote = new NameserverRemote(entity);
                exported = (INameserverRemote) UnicastRemoteObject.exportObject(remote, 0);
                root = exported;

                registry.bind(config.getString("root_id"), exported);
            } catch (RemoteException e) {
                throw new RuntimeException("Error while starting server.", e);
            } catch(AlreadyBoundException e) {
                LOG.error("Error while binding remote object to registry", e);
            }
        }

        // zone
        else {
            // lookup root Nameserver and register remote
            try {
                registry = LocateRegistry.getRegistry(
                        config.getString("registry.host"),
                        config.getInt("registry.port")
                );

                root = (INameserverRemote) registry.lookup(config.getString("root_id"));

                remote = new NameserverRemote(entity);
                exported = (INameserverRemote) UnicastRemoteObject.exportObject(
                        remote, 0);

                try {
                    root.registerNameserver(config.getString("domain"), exported);
                } catch (AlreadyRegisteredException | InvalidDomainException e) {
                    LOG.error(e);
                }

            } catch (RemoteException e) {
                LOG.error("Error while communicating with the server", e);
            } catch (NotBoundException e) {
                LOG.error("Server root not found in registry ", e);
            }
        }



        LOG.info("Starting shell...");

        shell.register(this);
        shell.setPrompt(entity.getComponentId() + " >");
        shell.run();

        LOG.info("Shell stopped");
    }

    @Override
    @Command
    public void nameservers() {
        StringBuilder msg = new StringBuilder();

        // TODO: synchronization for keyset iterator
        int cnt = 0;
        for (String key : entity.getZones().keySet()) {
            msg.append(++cnt).append(". ").append(key).append("\n");
        }

        print(msg.toString());
    }

    @Override
    @Command
    public void addresses() {
        StringBuilder msg = new StringBuilder();

        Hashtable<String, String> mailboxes;
        synchronized (mailboxes = entity.getMailservers()) {
            int cnt = 0;
            for (String key : mailboxes.keySet()) {
                msg.append(++cnt).append(". ").append(key).append(" ").append(mailboxes.get(key)).append("\n");
            }
        }

        print(msg.toString());

    }

    public void print(String msg) {
        shell.out().println(msg);
    }

    @Override
    @Command
    public void shutdown() {

        // unexport remote
        try {
            UnicastRemoteObject.unexportObject(remote, true);
        } catch (NoSuchObjectException e) {
            throw new RuntimeException(e);
        }

        // unbind root from registry
        if (!entity.getConfig().containsKey("domain")) {
            try {
                registry.unbind(entity.getConfig().getString("root_id"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                UnicastRemoteObject.unexportObject(registry, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        throw new StopShellException();

    }

    @Command
    public void create() {
        try {
            exported.registerMailboxServer("test.planet", "1234:123");
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    // TODO: remove me
    @Command
    public void find() {

        try {
            DomainResolver.resolve("test.planet", remote);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        INameserver component = ComponentFactory.createNameserver(args[0], System.in, System.out);
        component.run();
    }
}
