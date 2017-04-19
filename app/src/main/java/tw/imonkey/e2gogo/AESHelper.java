package tw.imonkey.e2gogo;
import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

class AESHelper {

    private static String SALT = "imonkey.tw";
    private final static String HEX = "0123456789ABCDEF";

    static String encrypt(String seed, String cleartext)
            throws Exception {
        SecretKey key = generateKey(seed.toCharArray(), SALT.getBytes());
        byte[] rawKey = key.getEncoded();
        byte[] result = encrypt(rawKey, cleartext.getBytes());
        return Base64.encodeToString(result, Base64.DEFAULT);
        // return toHex(result);
    }

     static String decrypt(String seed, String encrypted)
            throws Exception {
        SecretKey key = generateKey(seed.toCharArray(), SALT.getBytes());
        byte[] rawKey = key.getEncoded();
        byte[] enc = Base64.decode(encrypted, Base64.DEFAULT);
        //   byte[] enc = toByte(encrypted);
        byte[] result = decrypt(rawKey, enc);
        return new String(result);
    }

    private static SecretKey generateKey(char[] passphraseOrPin, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Number of PBKDF2 hardening rounds to use. Larger values increase
        // computation time. You should select a value that causes computation
        // to take >100ms.
        final int iterations = 1000;

        // Generate a 256-bit key
        final int outputKeyLength = 256;

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations, outputKeyLength);
        return secretKeyFactory.generateSecret(keySpec);
    }

    private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        return cipher.doFinal(clear);

    }

    private static byte[] decrypt(byte[] raw, byte[] encrypted)
            throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);

        return cipher.doFinal(encrypted);
    }

    private static String toHex(String txt) {
        return toHex(txt.getBytes());
    }

    private static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    private static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];

        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.
                    substring(2 * i, 2 * i + 2),16).byteValue();

        return result;
    }

    private static String toHex(byte[] buf) {
        if (buf == null)
            return "";

        StringBuffer result = new StringBuffer(2 * buf.length);

        for (byte aBuf : buf) {
            appendHex(result, aBuf);
        }

        return result.toString();
    }

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }
}