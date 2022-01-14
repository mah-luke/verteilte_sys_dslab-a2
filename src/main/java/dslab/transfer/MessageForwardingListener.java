package dslab.transfer;

import dslab.entity.MailEntity;
import dslab.nameserver.INameserverRemote;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageForwardingListener extends Thread {

    private final BlockingQueue<MailEntity> mailQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Config serverConfig;
    private final Log LOG = LogFactory.getLog(MessageForwardingListener.class);

    // Flag for terminating the thread
    private boolean shutdown = false;

    // Flag whether the thread is currently blocking
    private boolean isBlocking = false;

    private INameserverRemote nameserver;

    MessageForwardingListener(String hostAddress, Config serverConfig, INameserverRemote nameserver) {
        this.serverConfig = serverConfig;
        this.nameserver = nameserver;
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                isBlocking = true;
                MailEntity mail = mailQueue.take();
                isBlocking = false;

                LOG.info("Mail " + mail.getSubject() + " taken from queue for forwarding");
                executor.execute(new MessageForwardingThread(mail, serverConfig,nameserver));
            }
        } catch (InterruptedException e) {
            LOG.info("Forwarder stopped");
        } finally {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }

    public void put(MailEntity mail) throws InterruptedException {
        mailQueue.put(mail);
    }

    public void shutdown() {
        // if queue is currently forwarding a mail let it finish and then stop the thread
        if (isBlocking) shutdown = true;

        // if queue is waiting for new mails (blocking) interrupt the thread directly
        else this.interrupt();
    }
}
