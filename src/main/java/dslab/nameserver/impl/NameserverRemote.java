package dslab.nameserver.impl;

import dslab.nameserver.INameserverRemote;
import dslab.nameserver.entity.NameserverEntity;
import dslab.nameserver.exception.AlreadyRegisteredException;
import dslab.nameserver.exception.InvalidDomainException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

public class NameserverRemote implements INameserverRemote {

    private final NameserverEntity entity;
    private final static Log LOG = LogFactory.getLog(NameserverRemote.class);

    public NameserverRemote(NameserverEntity entity) {
        this.entity = entity;
    }


    //todo fix inverse
    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        String domainThis = entity.getConfig().containsKey("domain") ?
                entity.getConfig().getString("domain") : "";

        // is registering of domain allowed? (domainThis is null for root)
        if (domain.endsWith(domainThis)) {
            String[] subdomainSplit = (domainThis.length() > 0? domain.substring(0, domainThis.length() - 1) : domain)
                    .split("\\.");
            String next = subdomainSplit[subdomainSplit.length-1];

            // step into recursion
            if (subdomainSplit.length > 1) {
                if (!entity.getZones().containsKey(next))
                    throw new InvalidDomainException("Zone: " + next + " not registered for Nameserver: " + domainThis);
                getNameserver(next).registerNameserver(domain, nameserver);
            }

            // base case
            else {
                if (entity.getZones().containsKey(next)) {
                    try {
                        // call a method of nameserver to check if it's still up
                        entity.getZones().get(next).getNameserver("");
                        throw new AlreadyRegisteredException("Zone: " + domain + " already registered");
                    } catch (RemoteException e) {
                        // if old server is down replace with new server
                        LOG.warn("Old server not available. Registering new server");
                    }
                }
                entity.getZones().put(next, nameserver);
            }

        } else throw new InvalidDomainException("Registering subdomain '" + domain +
                "' is not allowed for server with domain '" + domainThis + "'.");
    }

    @Override
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        String domainThis = entity.getConfig().containsKey("domain") ?
                entity.getConfig().getString("domain") : "";

        if (domain.endsWith(domainThis)) {
            String[] subdomainSplit = (domainThis.length() > 0? domain.substring(0, domainThis.length() - 1) : domain)
                    .split("\\.");
            String next = subdomainSplit[subdomainSplit.length-1];

            // step into recursion
            if (subdomainSplit.length > 1) {
                if (!entity.getZones().containsKey(next))
                    throw new InvalidDomainException("Zone: " + next + " not registered for Nameserver: " + domainThis);

                getNameserver(next).registerMailboxServer(domain, address);
            }

            // base case
            else {
                if (entity.getMailservers().containsKey(next))
                    throw new AlreadyRegisteredException("Mailserver: " + domain + " already registered");
                entity.getMailservers().put(next, address);
            }
        } else throw new InvalidDomainException("Registering subdomain '" + domain +
                "' is not allowed for server with domain '" + domainThis + "'.");
    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        LOG.info("getNameserver called for zone: " + zone);
        return entity.getZones().get(zone);
    }

    @Override
    public String lookup(String username) throws RemoteException {
        LOG.info("getNameserver called for : " + username);
        return entity.getMailservers().get(username);
    }
}
