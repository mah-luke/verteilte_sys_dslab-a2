package dslab.transfer.tcp.dmtp;

import dslab.entity.MailEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class DMTPClientThread implements Runnable {

    private final MailEntity mail;
    private final String host;
    private final int port;
    private Socket socket;
    private final Log LOG = LogFactory.getLog(DMTPClientThread.class);

    public DMTPClientThread(MailEntity mail, String host, int port) {
        this.mail = mail;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {

        try {
            socket = new Socket(host, port);
            LOG.info("Client is up and connected to: " + host + " on port: " + port);

            try ( BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                  PrintWriter writer = new PrintWriter(socket.getOutputStream()))
            {
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
        } catch (SocketException e) {
            LOG.info("Stopping client because socket was closed");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    private void send(String line, BufferedReader reader, PrintWriter writer) throws IOException {
        writer.println(line);
        writer.flush();

        String res = reader.readLine();
        if (!res.startsWith("ok")) throw new ProtocolException(res);
    }
}
