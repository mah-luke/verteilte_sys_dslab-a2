package dslab.mailbox.tcp.dmtp;

import dslab.mailbox.persistence.MailStorage;
import dslab.protocol.dmtp.DMTPProtocol;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;

public class MailboxDMTPThread implements Runnable {

    private final Socket socket;
    private final Config config;
    private final MailStorage storage;
    private final Config userConfig;
    private final Log LOG = LogFactory.getLog(MailboxDMTPThread.class);

    public MailboxDMTPThread(Socket socket, Config config, MailStorage storage) {
        this.socket = socket;
        this.storage = storage;
        this. config = config;
        userConfig = new Config(config.getString("users.config"));
    }

    public void run() {
        try {
            DMTPProtocol protocol = new DMTPProtocol(
                    new MailboxDMTPParser(config.getString("domain"), userConfig.listKeys()),
                    socket);

            while (!socket.isClosed()) {
                storage.save(protocol.process());
            }

        } catch (SocketException e) {
            LOG.info("SocketException while handling socket: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SecurityException e) {
            LOG.warn(e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOG.error("Exception during closing of Socket: " + e.getMessage());
                }
            }
        }
    }
}
