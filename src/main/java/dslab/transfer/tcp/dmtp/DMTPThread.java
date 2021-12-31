package dslab.transfer.tcp.dmtp;

import dslab.protocol.dmtp.DMTPProtocol;
import dslab.protocol.dmtp.DefaultDMTPParser;
import dslab.transfer.MessageForwardingListener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;

public class DMTPThread implements Runnable {

    private final Socket socket;
    private final MessageForwardingListener forwarder;

    public DMTPThread(Socket socket, MessageForwardingListener forwarder) {
        this.socket = socket;
        this.forwarder = forwarder;
    }

    public void run() {
        try {
            DMTPProtocol protocol = new DMTPProtocol(new DefaultDMTPParser(), socket);

            while (!socket.isClosed()) {
                forwarder.put(protocol.process());
            }
        } catch (SocketException e) {
            System.out.println("SocketException while handling socket: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            System.out.println("InterruptException during putting: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
