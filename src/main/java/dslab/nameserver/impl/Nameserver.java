package dslab.nameserver.impl;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.ComponentFactory;
import dslab.nameserver.INameserver;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.entity.NameserverEntity;
import dslab.nameserver.exception.AlreadyRegisteredException;
import dslab.nameserver.exception.InvalidDomainException;
import dslab.util.Config;

public class Nameserver implements INameserver {

    private final NameserverEntity entity;
    private final Shell shell;
    private  final INameserverRemote remote;


    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public Nameserver(String componentId, Config config, InputStream in, PrintStream out) {
        // TODO
        shell = new Shell(in, out);

        entity = NameserverEntity.Builder.getInstance()
                .setComponentId(componentId)
                .setConfig(config)
                .build();

        remote = new NameserverRemote(this, entity);

        try {
            // is server root?
            if (!config.containsKey("domain")) {
                Registry registry = LocateRegistry.createRegistry(config.getInt("registry.port"));

                // bind remote to registry
                try {
                    registry.bind(config.getString("root_id"), remote);
                } catch (AlreadyBoundException e) {
                    System.err.println(e.getMessage());
                }

            }
            // zone
            else {
                // lookup root Nameserver and register remote
                Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
                try {
                    NameserverRemote root = (NameserverRemote) registry.lookup(config.getString("root_id"));
                    try {
                       root.registerNameserver(config.getString("domain"), remote);
                    } catch (AlreadyRegisteredException | InvalidDomainException e) {
                        System.err.println(e);
                    }
                } catch (NotBoundException e) {
                    System.err.println("No root server found! " + e);
                }
            }
        } catch (RemoteException e) {
            System.err.println(e);
        }

    }

    @Override
    public void run() {
        // TODO

        shell.run();
    }

    @Override
    public void nameservers() {
        // TODO
    }

    @Override
    public void addresses() {
        // TODO
    }

    @Override
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
