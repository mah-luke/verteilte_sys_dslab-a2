package dslab.monitoring.persistence;

import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;

public class MonitoringStorage {
    private final ConcurrentHashMap<String, Long> addresses;
    private final ConcurrentHashMap<String, Long> servers;

    public MonitoringStorage() {
        addresses = new ConcurrentHashMap<>();
        servers = new ConcurrentHashMap<>();
    }

    public void save(String host, String sender) {

        synchronized (addresses) {
            if (addresses.putIfAbsent(sender, 1L) != null) addresses.computeIfPresent(sender, (a, s) -> ++s);
        }

        synchronized (servers) {
            if (servers.putIfAbsent(host, 1L) != null) servers.computeIfPresent(host, (h, s) -> ++s);
        }
    }

    public AbstractMap<String, Long> getAddresses() {
        return addresses;
    }

    public AbstractMap<String, Long> getServers() {
        return servers;
    }


}
