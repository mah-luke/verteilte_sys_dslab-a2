package dslab.entity;

import dslab.util.Keys;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MailEntity {
    private String from;
    private Set<String> to;
    private String subject;
    private String data;
    private String hash = "";

    public String getHash() {return hash;}

    public void setHash(String hash) {this.hash = hash;}

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

    public boolean checkHash(String hash) {
        return hash().equals(hash);
    }

    public String hash() {
        File secretKeyFile = new File("keys/hmac.key");
        try {
            SecretKeySpec keySpec = Keys.readSecretKey(secretKeyFile);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);

            byte[] resBytes = mac.doFinal(value().getBytes());
            return Base64.getEncoder().encodeToString(resBytes);
        } catch (Exception e) {
            // Don't catch as invalid setup would make it impossible to use the DMTP2.0 protocol at all
            throw new RuntimeException("Error during hash creation", e);
        }
    }

    public String value() {
        List<String> list = new ArrayList<>();

        list.add(from);
        list.add(String.join(",", to));
        list.add(subject);
        list.add(data);

        return String.join("\n", list);
    }

    @Override
    public String toString() {
        String ret = "";
        ret += String.format("From: \t\t%s\n", from);
        ret += String.format("To: \t\t%s\n", String.join(",", to));
        ret += String.format("Subject: \t%s\n", subject);
        ret += String.format("Data: \t\t%s\n", data);
        ret += String.format("Hash: \t\t%s",hash);
        return ret;
    }

    public boolean isComplete() {
        return from != null &&
                to.size() > 0 &&
                subject != null &&
                data != null;
    }
}
