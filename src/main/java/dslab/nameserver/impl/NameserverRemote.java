package dslab.nameserver.impl;

import dslab.nameserver.INameserverRemote;
import dslab.nameserver.entity.NameserverEntity;
import dslab.nameserver.exception.AlreadyRegisteredException;
import dslab.nameserver.exception.InvalidDomainException;

import java.rmi.RemoteException;

public class NameserverRemote implements INameserverRemote {

    private final NameserverEntity entity;

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
                entity.getZones().get(next).registerNameserver(domain, nameserver);
            }

            // base case
            else {
                if (entity.getZones().containsKey(next))
                    throw new AlreadyRegisteredException("Zone: " + domain + " already registered");
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
                entity.getZones().get(next).registerMailboxServer(domain, address);
            }

            // base case
            else {
                if (entity.getMailboxes().containsKey(next))
                    throw new AlreadyRegisteredException("Mailbox: " + domain + " already registered");
                entity.getMailboxes().put(next, address);
            }
        } else throw new InvalidDomainException("Registering subdomain '" + domain +
                "' is not allowed for server with domain '" + domainThis + "'.");
    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        return entity.getZones().get(zone);
    }

    @Override
    public String lookup(String username) throws RemoteException {
        return entity.getMailboxes().get(username);
    }
}
