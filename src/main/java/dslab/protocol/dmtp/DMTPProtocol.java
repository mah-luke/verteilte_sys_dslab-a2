package dslab.protocol.dmtp;

import dslab.entity.MailEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;

public class DMTPProtocol {

    private final DMTPParser parser;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final Socket socket;
    private final Log LOG = LogFactory.getLog(DMTPProtocol.class);

    public DMTPProtocol(DMTPParser parser, Socket socket) throws IOException {
        this.parser = parser;
        this.socket = socket;

        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream());

        writer.println("ok DMTP2.0");
        writer.flush();
    }

    public MailEntity process() throws IOException {

        String request;
        String hash = null;
        MailEntity mailEntity = null;
        while ((request = reader.readLine()) != null) {

            LOG.info("Client sent request: " + request);

            String[] parts = request.strip().split("\\s", 2);
            String command = parts[0];

            try {
                if (command.equals("begin")) {
                    if (parts.length != 1) throw new ProtocolException("Command 'begin' expected no arguments");
                    if (mailEntity != null) throw new ProtocolException("Creation already started");

                    mailEntity = new MailEntity();
                    writer.println("ok");
                    writer.flush();
                } else if (command.equals("send")) {

                    if (parts.length != 1) throw new ProtocolException("Command 'send' expected no arguments");
                    if (mailEntity == null || !mailEntity.isComplete())
                        throw new ProtocolException("Creation not finished");

                    MailEntity ret = mailEntity;
                    mailEntity = null;
                    writer.println("ok");
                    writer.flush();

                    // ASK: Check for hash if hash was given during transmission
                    //  (given test requires possibility to send without hash)
                    //if (hash == null) LOG.warn("No hash was given for transmission (deprecated from DMTP1.0). " +
                    //        "Future versions may require the new DMTP2.0 format with a hash.");
                    //else {
                    //    //TODO: this is bad
                    //    //if (!ret.checkHash(hash)) throw new SecurityException("Hash not compatible with Mail");
                    //    //else LOG.info("Transmission successfully authenticated via hash.");
                    //}

                    return ret;
                } else if (command.equals("quit")) {
                    if (parts.length != 1) throw new ProtocolException("Command 'quit' expected no arguments");
                    writer.println("ok bye");
                    writer.flush();
                    socket.close();
                } else if (parts.length == 2) {

                    String content = parts[1];

                    if (mailEntity != null) {
                        String response = "ok";
                        switch (command) {
                            case "data":
                                mailEntity.setData(parser.data(content));
                                break;
                            case "subject":
                                mailEntity.setSubject(parser.subject(content));
                                break;
                            case "to":
                                Set<String> toSet = parser.to(content);
                                mailEntity.setTo(toSet);
                                response = "ok " + toSet.size();
                                break;
                            case "from":
                                mailEntity.setFrom(parser.from(content));
                                break;
                            case "hash":
                                hash = parser.hash(content);
                                mailEntity.setHash(hash);
                                break;
                        }

                        writer.println(response);
                        writer.flush();
                    } else throw new ProtocolException("Creation not started");
                } else
                    throw new ProtocolException("Command " + command + " has wrong argument length or is unknown");
            } catch (ProtocolException e) {
                writer.println(String.format("error %s", e.getMessage()));
                writer.flush();
            }
        }
        throw new SocketException("Client stopped connection");
    }
}
