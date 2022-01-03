package dslab.mailbox.tcp.dmap;

import dslab.mailbox.persistence.MailStorage;
import dslab.entity.MailEntity;
import dslab.security.AESCipher;
import dslab.util.Config;
import dslab.util.Keys;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MailboxDMAPThread implements Runnable {

    private final Socket socket;
    private final Config config;
    private final MailStorage storage;
    private String loggedInUser;
    private final Config userConfig;
    private final Log LOG = LogFactory.getLog(MailboxDMAPListenerThread.class);
    private AESCipher aesCipher = null;
    private final String componentId;

    public MailboxDMAPThread(Socket socket, Config config, MailStorage storage, String componentId) {
        this.socket = socket;
        this.componentId = componentId;

        this.config = config;
        this.storage = storage;
        userConfig = new Config(config.getString("users.config"));
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

            writer.println("ok DMAP2.0");
            writer.flush();

            String request;
            while ((request = reader.readLine()) != null) {
                LOG.info("Client sent request (raw): " + request);

                if (aesCipher != null) {
                    request = aesCipher.decrypt(request);
                    LOG.info("Client sent request (decrypted): " + request);
                }

                String[] parts = request.strip().split("\\s");
                String command = parts[0];
                String response = "error";

                if (command.equals("startsecure")) {
                    try {
                        aesCipher = handshake(writer, reader);
                    } catch (Exception e) {
                        throw new SecurityException("Handshake failed", e);
                    }
                }
                else {
                    try {
                        if (command.equals("quit")) {
                            response = "ok bye";
                            loggedInUser = null;
                            writer.println(response);
                            writer.flush();
                            socket.close();
                        } else if (command.equals("login")) {
                            if (parts.length != 3) throw new ProtocolException("2 arguments expected");

                            if (userConfig.containsKey(parts[1])) {
                                if (userConfig.getString(parts[1]).equals(parts[2])) {
                                    loggedInUser = parts[1];
                                    response = "ok";
                                } else
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
                                        if (id > mails.size() || id < 1)
                                            throw new ProtocolException("unknown message id");
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
                                        if (id > mails.size() || id < 1)
                                            throw new ProtocolException("unknown message id");
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

                    LOG.info("Response (plain): " + response);

                    if (aesCipher != null) {
                        response = aesCipher.encrypt(response);
                        LOG.info("Response (encrypted): " + response);
                    }

                    writer.println(response);
                    writer.flush();
                }
            }

        } catch (SocketException e) {
             LOG.info("SocketException while handling socket: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        } catch (SecurityException e) {
            LOG.warn("Handshake failed, terminating connection.", e);
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

    private AESCipher handshake(PrintWriter writer, BufferedReader reader) throws
            IOException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {


        // send (plain) ok <component-id> -> find server's pub key
        writer.println("ok " + componentId);
        writer.flush();

        // get  (RSA) ok <client-challenge> <secret-key> <iv> -> server decrypts with private
        String resolvedChallenge = solveChallenge(reader.readLine());
        String[] split = resolvedChallenge.split("\\s");

        if (!split[0].equals("ok") || split.length != 4) throw new IllegalArgumentException("Illegal challenge request");

        aesCipher = new AESCipher(
                new SecretKeySpec(Base64.getDecoder().decode(split[2]), "AES"),
                Base64.getDecoder().decode(split[3]));

        // send (AES) ok <client-challenge> -> send decrypted challenge back
        String response = aesCipher.encrypt("ok " + split[1]);
        LOG.info("Sending solved challenge: " + response);
        writer.println(response);
        writer.flush();

        // get  (AES) ok -> client accepts handshakee
        String request = reader.readLine();
        LOG.info("Handshake accept (encrypted): " + request);
        request = aesCipher.decrypt(request);
        LOG.info("Handshake accept (decrypted): " + request);

        if (!request.equals("ok")) throw new SecurityException("Handshake accept contained wrong response");

        return aesCipher;
    }

    private String solveChallenge(String challenge) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, IOException {

        LOG.info("Solving challenge: " + challenge);

        PrivateKey privateKey = Keys.readPrivateKey(new File("keys/server/" + componentId + ".der"));

        Cipher decrypterRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decrypterRSA.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] bytes = decrypterRSA.doFinal(Base64.getDecoder().decode(challenge));
        String decrypted = new String(bytes, StandardCharsets.UTF_8);

        LOG.info("Decrypted challenge: " + decrypted);
        return decrypted;
    }
}
