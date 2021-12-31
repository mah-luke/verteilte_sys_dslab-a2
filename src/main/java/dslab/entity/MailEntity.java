package dslab.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MailEntity {
    private String from;
    private Set<String> to;
    private String subject;
    private String data;

    public MailEntity() {
        to = new HashSet<>();
    }

    public Set<String> getTo() {
        return to;
    }

    public void setTo(Set<String> to) {
        this.to = to;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @Override
    public String toString() {
        String ret = "";
        ret += String.format("From: \t\t%s\n", from);
        ret += String.format("To: \t\t%s\n", String.join(", ", to));
        ret += String.format("Subject: \t%s\n", subject);
        ret += String.format("Data: \t\t%s", data);
        return ret;
    }

    public boolean isComplete() {
        return from != null &&
                to.size() > 0 &&
                subject != null &&
                data != null;
    }
}
