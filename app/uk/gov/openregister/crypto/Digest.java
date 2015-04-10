package uk.gov.openregister.crypto;

import org.apache.commons.codec.binary.Hex;
import play.Logger;

import java.security.MessageDigest;

public class Digest {

    public static String shasum(String raw) {
        try {
            String head = "blob " + raw.getBytes("UTF-8").length + "\0";

            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update((head + raw).getBytes("UTF-8"));
            return new String(Hex.encodeHex(md.digest()));

        } catch (Exception e) {
            Logger.error("Unable to create hash", e);
            throw new RuntimeException(e);
        }
    }
}
