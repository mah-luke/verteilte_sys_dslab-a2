package dslab.monitoring.udp;

import dslab.monitoring.persistence.MonitoringStorage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class MonitoringThread extends Thread {

    DatagramSocket datagramSocket;
    MonitoringStorage storage;
    private final Log LOG = LogFactory.getLog(MonitoringThread.class);

    public MonitoringThread(DatagramSocket datagramSocket, MonitoringStorage storage) {
        this.datagramSocket = datagramSocket;
        this.storage = storage;
    }

    public void run() {
        byte[] buffer;
        DatagramPacket packet;

        try {
            while (true) {
                buffer = new byte[1024];
                packet = new DatagramPacket(buffer, buffer.length);

                datagramSocket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength());
                LOG.info("Recieved request-packet from client: " + request);

                String[] parts = request.split("\\s");
                String response;

                if (validRequest(parts)) {
                    String host = parts[0];
                    String sender = parts[1];
                    storage.save(host, sender);
                    response = String.format("Added %s %s\n", host, sender);
                } else {
                    response = "!error provided message does not fit the expected format: <host>:<port> <email>";
                    LOG.info("Unexpected request: " + request);
                }

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                buffer = response.getBytes();

                packet = new DatagramPacket(buffer, buffer.length, address, port);
                datagramSocket.send(packet);
            }
        } catch (SocketException e) {
            LOG.warn("SocketException while waiting for/handling packets: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }

    }

    private boolean validRequest(String[] request) {

        if (request.length != 2) return false;

        String[] splitHost = request[0].split(":");
        if (splitHost.length != 2) return false;
        try {
            Integer.parseInt(splitHost[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        String[] splitMail = request[1].split("@");
        if (splitMail.length != 2) return false;

        return true;
    }
}
