package dev.adrwas.trafficlib.util;

import com.google.common.hash.Hashing;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class EncryptionManager {

    public static byte[] encrypt(byte[] input, String password) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        if(password == null || password.length() == 0) {
            return input;
        }

        SecretKey secretKey = getSecretKeyFromPassword(password);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        System.out.println("Using OFB");

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(getIVFromPassword(password)));

        return cipher.doFinal(input);
    }

    public static byte[] decrypt(byte[] input, String password) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        if(password == null || password.length() == 0) {
            return input;
        }

        SecretKey secretKey = getSecretKeyFromPassword(password);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        System.out.println("Using OFB");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(getIVFromPassword(password)));

        return cipher.doFinal(input);
    }

    private static SecretKey getSecretKeyFromPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(), password.getBytes(), 65536, 256)).getEncoded(), "AES");
    }

    private static byte[] getIVFromPassword(String password) {
        StringBuilder iv = new StringBuilder();
        iv.append(password.substring(0, Math.min(password.length(), 16)));
        while (iv.length() < 16) {
            iv.append(" ");
        }
        return iv.toString().getBytes();
    }
}
