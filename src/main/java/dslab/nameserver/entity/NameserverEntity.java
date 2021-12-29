package dslab.nameserver.entity;

import dslab.nameserver.INameserverRemote;
import dslab.util.Config;
import java.util.Hashtable;

public class NameserverEntity {

    private final String componentId;
    private final Hashtable<String, String> mailboxes = new Hashtable<>();
    private final Hashtable<String, INameserverRemote> zones = new Hashtable<>();
    private final Config config;

    public NameserverEntity(Builder builder) {
        componentId = builder.componentId;
        config = builder.config;
    }

    public String getComponentId() {
        return componentId;
    }

    public Hashtable<String, String> getMailboxes() {
        return mailboxes;
    }

    public Hashtable<String, INameserverRemote> getZones() {
        return zones;
    }

    public Config getConfig() {
        return config;
    }

    public static class Builder {

        private String componentId;
        private Config config;

        private Builder() {}

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder setComponentId(String componentId) {
            this.componentId = componentId;
            return this;
        }

        public Builder setConfig(Config config) {
            this.config = config;
            return this;
        }

        public NameserverEntity build() {
            return new NameserverEntity(this);
        }
    }
}
