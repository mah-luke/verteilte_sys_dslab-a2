package dslab.nameserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public final class DomainResolver {

    private final static Log LOG = LogFactory.getLog(DomainResolver.class);

    public static String resolve(String domain, String host, int port, String registeredName) throws RemoteException, NotBoundException {
        return resolve(domain, (INameserverRemote) LocateRegistry.getRegistry(host, port).lookup(registeredName));
    }

    public static String resolve(String domain, INameserverRemote root) throws RemoteException {

        String[] split = domain.split("\\.");
        INameserverRemote cur = root;

        for (int i = split.length - 1; i > 0; i--) {
            cur = cur.getNameserver(split[i]);
        }

        String address = cur.lookup(split[0]);

        LOG.info("Address registered for domain '" + domain + "': " + address);

        return address;
    }
}
