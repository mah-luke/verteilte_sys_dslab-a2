package dslab.protocol.dmtp;

import java.net.ProtocolException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultDMTPParser implements DMTPParser{

    @Override
    public String from(String request) throws ProtocolException {
        if (request.split("@").length != 2) throw new ProtocolException("Argument of command 'from' must be in the format '<user>@<hostname>'");
        return request;
    }

    @Override
    public Set<String> to(String request) throws ProtocolException {
        List<String> list = Arrays.asList(request.split(","));
        list.replaceAll(String::strip);
        Set<String> addresses = new HashSet<>(list);
        for (String address : addresses) {
            if (address.split("@").length != 2)
                throw new ProtocolException("Argument of command 'to' must be in the format '<user>@<hostname>, <user1>@<hostname1>, ...'");
        }
        return addresses;
    }

    @Override
    public String subject(String subject) {
        return subject;
    }

    @Override
    public String data(String data) {
        return data;
    }
}
