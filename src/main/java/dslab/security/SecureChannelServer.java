package dslab.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class SecureChannelServer {

    private final Cipher encrypter;
    private final Cipher decrypter;
    private final Base64.Encoder encoder;
    private final Base64.Decoder decoder;
    private static final Log LOG = LogFactory.getLog(SecureChannelServer.class);
    private final static String TRANSFORMATION = "AES";

    public SecureChannelServer(Key key, byte[] iv) throws
            InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        encrypter = Cipher.getInstance(TRANSFORMATION);
        encrypter.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        decrypter = Cipher.getInstance(TRANSFORMATION);
        decrypter.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        encoder = Base64.getEncoder();
        decoder = Base64.getDecoder();
    }

    public String encrypt(String plain) {

        try {
            byte[] bytes = encrypter.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return encoder.encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encrypted) {

        try {
            byte[] bytes = decrypter.doFinal(decoder.decode(encrypted));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
