package dslab.transfer.tcp.dmtp;

import dslab.entity.MailEntity;
import dslab.nameserver.INameserverRemote;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

public class DMTPClientThread implements Runnable {

    private final MailEntity mail;
    private final String server;
    private Socket socket;
    private final Log LOG = LogFactory.getLog(DMTPClientThread.class);
    private String serverIP;
    private final INameserverRemote nameserver;

    public DMTPClientThread(MailEntity mail,INameserverRemote nameserver, String server) {
        this.mail = mail;
        this.server = server;
        this.nameserver = nameserver;
        try {
            this.serverIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            this.serverIP = "127.0.0.1";
        }
    }

    private String[] getAd(String server) throws RemoteException {
        String[] domain = server.split("\\.");
        INameserverRemote currentNameserver = nameserver;
        for (int i = domain.length - 1; i > 0; i--) {
            currentNameserver = currentNameserver.getNameserver(domain[i]);
        }
        String[] location = currentNameserver.lookup(domain[0]).split(":");
        LOG.info(location[0] + ":" + location[1]);

        return location;
        //String[] location = config.getString(server).split(":");
    }

    @Override
    public void run() {
        try {
            String[] location = getAd(server);
            sendAsClient(mail, location[0], Integer.parseInt(location[1]));
        } catch (Exception e) {
            try {
                String[] location = getAd(mail.getFrom().split("@")[1]);
                sendAsClient(generateErrorMail(e.getMessage()), location[0], Integer.parseInt(location[1]));
            } catch (Exception f) {
                LOG.info("Failed to send error message");
            }
        }
    }

    private void sendAsClient(MailEntity mail, String host, int port) throws Exception {
        try {
            socket = new Socket(host, port);
            LOG.info("Client is up and connected to: " + host + " on port: " + port);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream())) {
                send("begin", reader, writer);
                send("from " + mail.getFrom(), reader, writer);
                send("to " + String.join(", ", mail.getTo()), reader, writer);
                send("subject " + mail.getSubject(), reader, writer);
                send("data " + mail.getData(), reader, writer);
                send("hash " + mail.getHash(), reader, writer);
                send("send", reader, writer);
                send("quit", reader, writer);
            }
            LOG.debug("Mail " + mail.getSubject() + " successfully sent to " + host + ":" + port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw e;
        } catch (SocketException e) {
            LOG.info("Stopping client because socket was closed");
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOG.error("Exception during closing of Socket: " + e.getMessage());
                }
            }
            LOG.debug("Socket, reader and writer of client closed.");
        }
    }

    private MailEntity generateErrorMail(String reason) {
        MailEntity errorMail = new MailEntity();
        Set<String> to = new HashSet<>();
        to.add(mail.getFrom());
        errorMail.setTo(to);
        errorMail.setFrom("mailer@[" + serverIP + "]");
        errorMail.setSubject("error");
        errorMail.setData("Couldn't send message. Reason: " + reason);
        errorMail.setHash(errorMail.hash());
        return errorMail;
    }

    private void send(String line, BufferedReader reader, PrintWriter writer) throws IOException {
        writer.println(line);
        writer.flush();

        String res = reader.readLine();
        if (!res.startsWith("ok")) throw new ProtocolException(res);
    }
}
