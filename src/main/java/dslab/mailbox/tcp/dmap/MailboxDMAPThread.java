package dslab.mailbox.tcp.dmap;

import dslab.mailbox.persistence.MailStorage;
import dslab.entity.MailEntity;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class MailboxDMAPThread implements Runnable {

    private final Socket socket;
    private final Config config;
    private final MailStorage storage;
    private String loggedInUser;
    private final Config userConfig;
    private final Log LOG = LogFactory.getLog(MailboxDMAPListenerThread.class);

    public MailboxDMAPThread(Socket socket, Config config, MailStorage storage) {
        this.socket = socket;

        this.config = config;
        this.storage = storage;
        userConfig = new Config(config.getString("users.config"));
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

            writer.println("ok DMAP");
            writer.flush();

            String request;
            while ((request = reader.readLine()) != null) {
                LOG.info("Client sent request: " + request);

                String[] parts = request.strip().split("\\s");
                String command = parts[0];
                String response = "error";

                try {
                    if (command.equals("quit")){
                        response = "ok bye";
                        loggedInUser = null;
                        writer.println(response);
                        writer.flush();
                        socket.close();
                    }
                    if (command.equals("login")) {
                        if (parts.length != 3) throw new ProtocolException("2 arguments expected");

                        if (userConfig.containsKey(parts[1])) {
                            if (userConfig.getString(parts[1]).equals(parts[2])){
                                loggedInUser = parts[1];
                                response = "ok";
                            }
                            else
                                throw new ProtocolException("wrong password");
                        } else {
                            throw new ProtocolException("unknown user");
                        }
                    } else {

                        if (loggedInUser == null) throw new ProtocolException("not logged in");

                        List<MailEntity> mails;
                        switch (command) {
                            case "list":
                                if (parts.length != 1) throw new ProtocolException("0 arguments expected");
                                synchronized (mails = storage.retrieve(loggedInUser)) {
                                    List<String> entries = new ArrayList<>();
                                    for (int i = 0; i < mails.size(); i++) {
                                        MailEntity mail = mails.get(i);
                                        entries.add(String.format("%d %s %s", i + 1, mail.getFrom(), mail.getSubject()));
                                    }
                                    response = String.join("\n", entries);
                                }
                                break;
                            case "logout":
                                response = "ok";
                                loggedInUser = null;
                                break;
                            case "show":
                                if (parts.length != 2) throw new ProtocolException("1 argument expected");
                                try {
                                    int id = Integer.parseInt(parts[1]);
                                     mails = storage.retrieve(loggedInUser);
                                     if (id > mails.size() || id < 1) throw new ProtocolException("unknown message id");
                                     response = mails.get(id - 1).toString();
                                } catch (NumberFormatException e) {
                                    throw new ProtocolException("message id not a number");
                                }
                                break;
                            case "delete":
                                if (parts.length != 2) throw new ProtocolException("1 argument expected");
                                try {
                                    int id = Integer.parseInt(parts[1]);
                                    mails = storage.retrieve(loggedInUser);
                                    if (id > mails.size() || id < 1) throw new ProtocolException("unknown message id");
                                    mails.remove(id - 1);
                                    response = "ok";
                                } catch (NumberFormatException e) {
                                    throw new ProtocolException("message id not a number");
                                }
                                break;
                        }
                    }
                } catch (ProtocolException e) {
                    response = String.format("error %s", e.getMessage());
                }

                writer.println(response);
                writer.flush();
            }

        } catch (SocketException e) {
             LOG.info("SocketException while handling socket: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOG.error("Exception during closing of the Socket: " + e.getMessage());
                }
            }
        }
    }
}
