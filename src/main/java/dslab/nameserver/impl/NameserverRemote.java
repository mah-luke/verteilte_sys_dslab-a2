package dslab.nameserver.impl;

import dslab.nameserver.INameserver;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.entity.NameserverEntity;
import dslab.nameserver.exception.AlreadyRegisteredException;
import dslab.nameserver.exception.InvalidDomainException;
import dslab.util.Config;

import java.rmi.RemoteException;

public class NameserverRemote implements INameserverRemote {

    private final INameserver nameserver;
    private final NameserverEntity entity;

    public NameserverRemote(INameserver nameserver, NameserverEntity entity) {
        this.nameserver = nameserver;
        this.entity = entity;
    }


    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        String domainThis = entity.getConfig().getString("domain");

        // is registering of domain allowed? (domainThis is null for root)
        if (domainThis == null || domain.startsWith(domainThis)) {
            String subdomain = domainThis == null? domain : domain.substring(domainThis.length());
            String[] subdomainSplit = subdomain.split("\\.");

            if (subdomainSplit.length > 1) {
                if (entity.getZones().containsKey(subdomainSplit[0])) {
                    entity.getZones().get(subdomainSplit[0]).registerNameserver(domain, nameserver);
                } else throw new InvalidDomainException("Zone: " + subdomainSplit[0] + " not registered for Nameserver: " + domainThis);
            }
            else if (subdomainSplit.length == 1){
                if (!entity.getZones().containsKey(subdomainSplit[0])) {
                    entity.getZones().put(subdomainSplit[0], nameserver);
                } else throw new AlreadyRegisteredException("Domain: " + domain + " already registered");
            }
            else throw new InvalidDomainException("Domain invalid: " + domain);

        } else throw new InvalidDomainException("Registering subdomain '" + domain +
                "' is not allowed for server with domain '" + domainThis + "'.");
    }

    @Override
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        String domainThis = entity.getConfig().getString("domain");

        if (domainThis == null || domain.startsWith(domainThis)) {
            String subdomain = domainThis == null? domain : domain.substring(domainThis.length());
            String[] subdomainSplit = subdomain.split("\\.");

            if (subdomainSplit.length > 1) {
                if (!entity.getZones().containsKey(subdomainSplit[0])) {
                    entity.getZones().get(subdomainSplit[0]).registerMailboxServer(domain, address);
                } else throw new InvalidDomainException("Zone: " + subdomainSplit[0] + " not registered for Nameserver: " + domainThis);
            }
            else if (subdomainSplit.length == 1) {
                if (!entity.getMailboxes().containsKey(subdomainSplit[0])) {
                    entity.getMailboxes().put(subdomainSplit[0], address);
                } else throw new AlreadyRegisteredException("Mailbox: " + domain + " already registered");
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
        return null;
    }
}
