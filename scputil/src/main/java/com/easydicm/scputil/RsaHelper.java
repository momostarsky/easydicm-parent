package com.easydicm.scputil;

import com.google.common.collect.Maps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

public class RsaHelper {


    /***
     * RSA  算法名称
     */
    private static final String RSA_ALG = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final int Segment_Size = 245;


    public static final int PUBLIC_KEY = 0;
    public static final int PRIVATE_KEY = 1;

    /***
     * 随机生成密钥对  Map[0]:Publickey , Map[1]:PrivateKey
     * @return Map<Integer, String>   Map[0]:Publickey , Map[1]:PrivateKey
     * @throws NoSuchAlgorithmException
     */
    public static Map<Integer, String> genKeyPair() {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        Map<Integer, String> keyMap = Maps.newHashMap();
        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance(RSA_ALG);
        } catch (NoSuchAlgorithmException e) {

        }

        // 初始化密钥对生成器
        keyPairGen.initialize(KEY_SIZE, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        //公钥字符串
        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        // 得到私钥字符串
        String privateKeyString = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        // 将公钥和私钥保存到Map
        //0表示公钥
        keyMap.put(PUBLIC_KEY, publicKeyString);
        //1表示私钥
        keyMap.put(PRIVATE_KEY, privateKeyString);


        return keyMap;
    }

    /**
     * RSA公钥加密
     *
     * @param str       待加密字符串
     * @param publicKey 公钥
     * @return 密文  Base64
     * @throws Exception 加密过程中的异常信息
     */
    public static String encrypt(String str, String publicKey) throws Exception {
        //base64编码的公钥
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        if (decoded.length > Segment_Size) {
            throw new RsaException("Data must not be longer than 245 bytes");
        }
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance(RSA_ALG).generatePublic(new X509EncodedKeySpec(decoded));
        //RSA加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] messageData = str.getBytes("UTF-8");
        byte[] resultData = cipher.doFinal(messageData);
        return Base64.getEncoder().encodeToString(resultData);
    }

    /***
     * 私钥加密
     * @param str  待加密字符串
     * @param privateKey 私钥
     * @return 密文  Base64
     * @throws Exception  加密过程中的异常信息
     */
    public static String encrypt(String str, RSAPrivateKey privateKey) throws Exception {

        byte[] plainTextData = str.getBytes(StandardCharsets.UTF_8);
        if (plainTextData.length > Segment_Size) {
            throw new RsaException("Data must not be longer than 245 bytes");
        }
        Cipher cipher = Cipher.getInstance(RSA_ALG);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] output = cipher.doFinal(plainTextData);
        return Base64.getEncoder().encodeToString(output);

    }


    /**
     * RSA私钥解密
     *
     * @param str        待解密的字符串
     * @param privateKey 私钥
     * @return UTF8编码的字符串
     * @throws Exception 解密过程中的异常信息
     */
    public static String decrypt(String str, String privateKey) throws Exception {
        //64位解码加密后的字符串
        byte[] inputByte = Base64.getDecoder().decode(str);
        //base64编码的私钥
        byte[] decoded = Base64.getDecoder().decode(privateKey);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance(RSA_ALG).generatePrivate(new PKCS8EncodedKeySpec(decoded));
        //RSA解密
        Cipher cipher = Cipher.getInstance(RSA_ALG);
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        byte[] resultData = cipher.doFinal(inputByte);
        return new String(resultData, StandardCharsets.UTF_8);
    }


    /**
     * RSA 公钥解密
     *
     * @param str       待解密的字符串
     * @param publicKey 公钥
     * @return UTF8编码的字符串
     * @throws Exception 解密过程中的异常信息
     */
    public static String decrypt(String str, RSAPublicKey publicKey) throws Exception {

        // 使用默认RSA
        byte[] cipherData = Base64.getDecoder().decode(str);
        Cipher cipher = Cipher.getInstance("RSA");
        // cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] output = cipher.doFinal(cipherData);
        return new String(output, StandardCharsets.UTF_8);

    }


    /**
     * 从字符串中加载公钥
     */
    public static RSAPublicKey loadPublicKey(String publicKeyStr) throws Exception {
        try {
            byte[] buffer = Base64.getDecoder().decode(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALG);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    //
    public static RSAPrivateKey loadPrivateKey(String privateKeyStr) throws Exception {
        try {
            byte[] buffer = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALG);
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static void savePublicKey(PublicKey publicKey, File saveTo) throws IOException {
        // 得到公钥字符串
        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        FileWriter fw = new FileWriter(saveTo);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(publicKeyString);
        bw.close();
    }

    public static void savePrivateKey(PrivateKey privateKey, File saveTo) throws IOException {
        // 得到私钥字符串
        String privateKeyString = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        BufferedWriter bw = new BufferedWriter(new FileWriter(saveTo));
        bw.write(privateKeyString);
        bw.close();
    }


}
