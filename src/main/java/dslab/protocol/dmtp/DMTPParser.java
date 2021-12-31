package dslab.protocol.dmtp;

import java.net.ProtocolException;
import java.util.Set;

public interface DMTPParser {

    String from(String request) throws ProtocolException;

    Set<String> to(String request) throws ProtocolException;

    String subject(String subject);

    String data(String data);
}
