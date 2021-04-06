package com.example.Encryption;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.security.NoSuchAlgorithmException;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

//https://howtodoinjava.com/java/java-security/java-aes-encryption-example/?fbclid=IwAR2NvID66LfmnhDF_1z4YwkR7JYgZz_7mwN4E90TEhSLdB1afZbPnQZAaq0

public class AES{
    private static SecretKeySpec secretKey;

    public static void main(String[] args) {
        String secretKey = "SomeRandomString";
        String originalMessage = "Super secret classified message";
        String encryptedMessage = AES.encrypt(originalMessage, secretKey) ;
        String decryptedMessage = AES.decrypt(encryptedMessage, secretKey) ;

        System.out.println(originalMessage);
        System.out.println(encryptedMessage);
        System.out.println(decryptedMessage);
    }

    public static void setKey(String myKey) {
        MessageDigest sha;
        try {
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    public static String encrypt(String msgToEncrypt, String secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(msgToEncrypt.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }
    public static String decrypt(String msgToDecrypt, String secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(msgToDecrypt)));
        }
        catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
}


