package dslab.transfer.udp;

import dslab.entity.MailEntity;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;

public class UDPClient implements Runnable {

    private final MailEntity mail;
    private final Config config;
    private final Log LOG = LogFactory.getLog(UDPClient.class);

    public UDPClient(MailEntity mail, Config config) {
        this.mail = mail;
        this.config = config;
    }

    @Override
    public void run() {
        DatagramSocket socket = null;

        try {
            // open a new DatagramSocket
            socket = new DatagramSocket();

            byte[] buffer;
            DatagramPacket packet;

            String input = String.format("%s:%s %s",
                    socket.getLocalAddress().getHostAddress(),
                    config.getString("tcp.port"),
                    mail.getFrom());

            // convert the input String to a byte[]
            buffer = input.getBytes();
            // create the datagram packet with all the necessary information
            // for sending the packet to the server
            packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(config.getString("monitoring.host")),
                    config.getInt("monitoring.port"));

            // send request-packet to server
            socket.send(packet);
            LOG.debug("UDP packet sent");
        } catch (UnknownHostException e) {
            LOG.warn("Cannot connect to host: " + e.getMessage());
        } catch (SocketException e) {
            LOG.warn("SocketException: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
