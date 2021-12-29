package dslab.nameserver.entity;

import dslab.nameserver.INameserverRemote;
import dslab.nameserver.impl.NameserverRemote;
import dslab.util.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class NameserverEntity {

    private final String componentId;
    private final Hashtable<String, String> mailboxes = new Hashtable<>();
    private final Hashtable<String, INameserverRemote> zones = new Hashtable<>();
    private final Config config;
//    private final INameserverRemote remote;

    public NameserverEntity(Builder builder) {
        componentId = builder.componentId;
        config = builder.config;
//        remote = builder.remote;
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
//        private INameserverRemote remote;

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

//        public Builder setRemote(INameserverRemote remote) {
//            this.remote = remote;
//            return this;
//        }

        public NameserverEntity build() {
            return new NameserverEntity(this);
        }
    }
}
