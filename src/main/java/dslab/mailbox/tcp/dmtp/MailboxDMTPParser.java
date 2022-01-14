package dslab.mailbox.tcp.dmtp;

import dslab.protocol.dmtp.DefaultDMTPParser;

import java.net.ProtocolException;
import java.util.Set;
import java.util.stream.Collectors;

public class MailboxDMTPParser extends DefaultDMTPParser {

    private final String domain;
    private final Set<String> users;

    MailboxDMTPParser(String domain, Set<String> users) {
        this.domain = domain;
        this.users = users;
    }

    @Override
    public Set<String> to(String request) throws ProtocolException {
        // use basic parsing from DefaultDMTPParser
        Set<String> addresses = super.to(request);

        // filter for domain
        addresses = addresses.stream()
                .filter(e -> domain.equals(e.split("@", 2)[1]))
                .filter(e -> users.contains(e.split("@", 2)[0]))
                .collect(Collectors.toSet());

        if (addresses.size() == 0) throw new ProtocolException("unknown recipient");

        //TODO: changed from addresses
        return super.to(request);
    }
}
