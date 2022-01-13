package dslab.transfer;

import dslab.entity.MailEntity;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.exception.AlreadyRegisteredException;
import dslab.nameserver.exception.InvalidDomainException;
import dslab.transfer.tcp.dmtp.DMTPClientThread;
import dslab.transfer.udp.UDPClient;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.ProtocolException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.util.Set;

public class MessageForwardingThread implements Runnable {

    private final MailEntity mail;
    private final Config serverConfig;
    private INameserverRemote nameserver;
    //TODO: dont use config
    //private final Config config = new Config("domains");
    private final Log LOG = LogFactory.getLog(MessageForwardingThread.class);

    MessageForwardingThread(MailEntity mail, Config serverconfig, INameserverRemote nameserver) {
        this.mail = mail;
        this.serverConfig = serverconfig;
        this.nameserver = nameserver;
    }

    @Override
    public void run() {

        Set<String> recipients = mail.getTo();
        Set<String> servers = new LinkedHashSet<>();

        for (String recipient : recipients) {
            servers.add(recipient.split("@")[1]);
        }

        // all threads with run can optionally be run in a thread pool if needed
        for (String server : servers) {
            LOG.info("Start forwarding client for mail " + mail.getSubject() + " to server " + server + "...");
            try {
                new DMTPClientThread(mail, nameserver, server).run();
            } catch (MissingResourceException e) {
                LOG.warn("Server with address: " + server + " not registered");
            }
        }
        new UDPClient(mail, serverConfig).run();
    }
}
