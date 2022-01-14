package dslab.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ProtocolException;
import java.net.Socket;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.security.AESCipher;
import dslab.util.Config;
import dslab.util.Keys;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MessageClient implements IMessageClient, Runnable {

    private final Log LOG = LogFactory.getLog(MessageClient.class);
    final private Shell shell;
    final private Config config;
    private Socket socketDmap;
    private String email;

    private AESCipher aesCipher = null;
    private BufferedReader dmapReader;
    private PrintWriter dmapWriter;


    /**
     * Creates a new client instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public MessageClient(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {
        this.email = config.getString("transfer.email");
        try {
            //dmap connect start in the beginning and runs throughout the session
            socketDmap = new Socket(config.getString("mailbox.host"), config.getInt("mailbox.port"));
            this.dmapReader = new BufferedReader(new InputStreamReader(socketDmap.getInputStream()));
            this.dmapWriter = new PrintWriter(socketDmap.getOutputStream());

            String response = dmapReader.readLine();
            if (!response.startsWith("ok DMAP2.0")) throw new ProtocolException("Bad response");
            //shell.out().println(response);
            startsecure();
        } catch (IOException e) {
            cleanup();
            throw new UncheckedIOException("Error while creating socket", e);
        }
        shell.run();
    }

    public void startsecure() {
        try {
            //begin protocol
            dmapWriter.println("startsecure");
            dmapWriter.flush();

            String response = dmapReader.readLine();
            if (!response.startsWith("ok"))
                throw new ProtocolException(response);

            //LOG.info("Started handshake");

            String componentId = response.substring(3);
            //generate challenge
            SecureRandom random = new SecureRandom();
            byte[] challengeBytes = new byte[32];
            random.nextBytes(challengeBytes);
            String challenge = new String(Base64.getEncoder().encode(challengeBytes));

            //generate key
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(random);
            keyGenerator.init(256);
            SecretKey key = keyGenerator.generateKey();
            byte[] keyByte = key.getEncoded();
            String keyString = new String(Base64.getEncoder().encode(keyByte));

            //generate iv
            byte[] iv = new byte[16];
            random.nextBytes(iv);
            String ivString = new String(Base64.getEncoder().encode(iv));

            //create and send message encrypted with a rsa public key
            String challengeMessage = String.format("ok %s %s %s", challenge, keyString, ivString);

            PublicKey publicKey = Keys.readPublicKey(new File(String.format("keys/client/%s_pub.der", componentId)));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(challengeMessage.getBytes());
            String message = new String(Base64.getEncoder().encode(encryptedBytes));

            dmapWriter.println(message);
            dmapWriter.flush();

            String challengeAesEnc = dmapReader.readLine();

            aesCipher = new AESCipher(key, iv);
            String challengeResponse = aesCipher.decrypt(challengeAesEnc).split("\\s")[1];
            if (!challengeResponse.equals(challenge))
                throw new ProtocolException("Bad challenge response");

            //final ok message in aes
            dmapWriter.println(aesCipher.encrypt("ok"));
            dmapWriter.flush();

            LOG.info("Completed handshake");

            //login the user
            String res = sendDmap(String.format("login %s %s", config.getString("mailbox.user"), config.getString("mailbox.password")));
            if (!res.startsWith("ok")) throw new ProtocolException(res);

        } catch (ProtocolException e) {
            shell.out().println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            //if this is reached then there is something wrong with the code itself. As in wrong data is used for key gen etc.
            throw new IllegalArgumentException();
        }
    }

    private String sendDmap(String line) {
        try {
            dmapWriter.println(aesCipher.encrypt(line));
            dmapWriter.flush();

            return aesCipher.decrypt(dmapReader.readLine());
        } catch (IOException e) {
            return "Error while trying to write to sever";
        }
    }

    public String hash(String value) {
        File secretKeyFile = new File("keys/hmac.key");
        try {
            SecretKeySpec keySpec = Keys.readSecretKey(secretKeyFile);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);

            byte[] resBytes = mac.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(resBytes);
        } catch (Exception e) {
            // Don't catch as invalid setup would make it impossible to use the DMTP2.0 protocol at all
            throw new RuntimeException("Error during hash creation", e);
        }
    }

    @Override
    @Command
    public void inbox() {
        boolean linebreak = false;
        String res = sendDmap("list");
        String[] lines = res.split("\n");
        for (String line : lines) {
            if (line.equals("ok")) break;
            String number = line.split("\\s")[0];
            String res2 = sendDmap("show " + number);
            if (res2.endsWith("\nok"))
                if (linebreak) shell.out().println();
            linebreak = true;
            shell.out().println("Number: \t" + number);
            shell.out().println(res2.substring(0, res2.length() - 3));
        }
        //shell.out().println(res);
    }

    @Override
    @Command
    public void delete(String id) {
        String res = sendDmap("delete " + id);
        shell.out().println(res);
    }

    @Override
    @Command
    public void verify(String id) {
        String res = sendDmap("show " + id);
        Pattern p = Pattern.compile("^From: \t\t(?<from>.*)\nTo: \t\t(?<to>.*)\nSubject: \t(?<subject>.*)\nData: \t\t(?<data>.*)\nHash: \t\t(?<hash>.*)\nok$");
        Matcher m = p.matcher(res);
        if (m.find()) {
            String all = String.join("\n", m.group("from"), m.group("to"), m.group("subject"), m.group("data"));
            String hash = m.group("hash");
            shell.out().println(hash(all).equals(hash));
        } else {
            shell.out().println("error could not match regex to string");
        }
    }


    @Override
    @Command
    public void msg(String to, String subject, String data) {
        //formatting to
        List<String> list = Arrays.asList(to.split(","));
        list.replaceAll(String::strip);
        Set<String> addresses = new HashSet<>(list);
        for (String address : addresses) {
            if (address.split("@").length != 2) {
                shell.out().println("Argument of command 'to' must be in the format '<user>@<hostname>, <user1>@<hostname1>, ...'");
                return;
            }
        }
        String toFormat = String.join(",", addresses);
        String all = String.join("\n", email, toFormat, subject, data);

        Socket socket = null;
        try {
            socket = new Socket(config.getString("transfer.host"), config.getInt("transfer.port"));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream())) {
                send("begin", reader, writer);
                send("from " + email, reader, writer);
                send("to " + toFormat, reader, writer);
                send("subject " + subject, reader, writer);
                send("data " + data, reader, writer);
                send("hash " + hash(all), reader, writer);
                send("send", reader, writer);
                send("quit", reader, writer);
                shell.out().println("ok");
            }
        } catch (ProtocolException e) {
            LOG.info(e.getMessage());
            shell.out().println(e.getMessage());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (
                SocketException e) {
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


    //closes all allocated resources
    private void cleanup() {
        if (socketDmap != null && !socketDmap.isClosed()) {
            try {
                sendDmap("logout");
                socketDmap.close();
            } catch (IOException e) {
                // Ignored because we cannot handle it
            }
        }
        if (dmapReader != null) {
            try {
                dmapReader.close();
            } catch (IOException e) {
                // Ignored because we cannot handle it
            }
        }
        if (dmapWriter != null) {
            dmapWriter.close();
        }
    }

    @Override
    @Command
    public void shutdown() {
        cleanup();
        throw new StopShellException();

    }

    public static void main(String[] args) throws Exception {
        IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
        client.run();
    }
}
