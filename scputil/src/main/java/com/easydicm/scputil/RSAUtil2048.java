package com.easydicm.scputil;


import org.bouncycastle.util.encoders.Base64;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;


/**
 * @Auther： 李志强
 * @Description: RSA2048加密    --如果需要改成1024的,需要改下生成方法的密钥长度并重新生成,然后MAX_DECRYPT_BLOCK改成128就可以了
 * @Date: https://www.cnblogs.com/caolingyi/p/12395379.html
 **/
public class RSAUtil2048 {

    //加密算法RSA
    public static final String KEY_ALGORITHM = "RSA";
    //签名算法
    public static final String SIGNATURE_ALGORITHM = "SHA1WithRSA";
    //获取公钥的key
    public static final String PUBLIC_KEY = "RSAPublicKey";
    //获取私钥的key
    public static final String PRIVATE_KEY = "RSAPrivateKey";
    //RSA最大加密明文大小
    private static final int MAX_ENCRYPT_BLOCK = 200;
    //RSA最大解密密文大小
    private static final int MAX_DECRYPT_BLOCK = 211;
    //RSA KeySize
    private static final int KeySize = 1688;


    protected  static  KeyPair getKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        //使用给定的随机源(和默认的参数集合)初始化确定密钥长度的密钥对生成器       SecureRandom()随机源
        keyPairGenerator.initialize(KeySize, new SecureRandom());
        //generateKeyPair()生成密钥对
        return  keyPairGenerator.generateKeyPair();
    }
    /**
     * @return void
     * @Author：
     * @Description: //TODO 生成密钥对(公钥和私钥)文件并保存本地    下面的生成方法和这个差不多 只是不保存和初始密钥长度的地方有所区别
     * @Date:
     * @Param: [filePath]  保存文件路径
     **/
    public static void genKeyPairByFilePath(File saveDir, String publickeyFileName, String privateKeyFileName) throws Exception {
        //KeyPairGenerator秘钥构成器，也就是可以生成一对秘钥，可以是公钥也可以是私钥，所以大部分用在非对称加密中   getInstance()设置密钥的格式
        KeyPair  keyPair = getKeyPair();
        //强转成公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        //强转成私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        //二进制转字符串
        String publicKeyString = Base64.toBase64String(publicKey.getEncoded());
        //二进制转字符串
        String privateKeyString = Base64.toBase64String(privateKey.getEncoded());
        //下面是保存方法

        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        Path f1 = Paths.get(saveDir.getAbsolutePath(), publickeyFileName);
        Path f2 = Paths.get(saveDir.getAbsolutePath(), privateKeyFileName);
        Files.writeString(f1, publicKeyString, StandardCharsets.UTF_8);
        Files.writeString(f2, privateKeyString, StandardCharsets.UTF_8);
    }

    public static Map<String, String> genKeyPairString( ) throws Exception {
        //KeyPairGenerator秘钥构成器，也就是可以生成一对秘钥，可以是公钥也可以是私钥，所以大部分用在非对称加密中   getInstance()设置密钥的格式
        Map<String, String> km = new HashMap<>();
        KeyPair  keyPair = getKeyPair();
        //强转成公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        //强转成私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        //二进制转字符串
        String publicKeyString = Base64.toBase64String(publicKey.getEncoded());
        //二进制转字符串
        String privateKeyString = Base64.toBase64String(privateKey.getEncoded());
        //下面是保存方法
        km.put(PUBLIC_KEY, publicKeyString);
        km.put(PRIVATE_KEY, privateKeyString);
        return km;

    }


    /**
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Author：
     * @Description: //TODO  生成密钥对(公钥和私钥)
     * @Date:
     **/
    public static Map<String, Object> genKeyPair() throws Exception {
        //秘钥构成器，也就是可以生成一对秘钥，可以是公钥也可以是私钥，所以大部分用在非对称加密中

        KeyPair keyPair =getKeyPair();
        //强转成公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        //强转成私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        Map<String, Object> keyMap = new HashMap<String, Object>(2);
        keyMap.put(PUBLIC_KEY, publicKey);
        keyMap.put(PRIVATE_KEY, privateKey);
        return keyMap;
    }

    /**
     * @return java.lang.String
     * @Author：
     * @Description: //TODO  从文件中读取公钥或私钥
     * @Date:
     * @Param: [filePath]
     **/
    public static String readKeyFromFile(String filePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                sb.append(readLine);
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param data       已加密数据
     * @param privateKey 私钥(BASE64编码)
     * @return java.lang.String   Base64编码字符串
     * @Author：
     * @Description: //TODO  私钥加签
     * @Date:
     **/
    public static String sign(byte[] data, String privateKey) throws Exception {
        //字符串解码为二进制数据
        byte[] keyBytes = Base64Utils.decodeFromString(privateKey);
        //私钥的ASN.1编码规范
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        //KeyFactory一般通过自己的静态方法keyFactory.generatePublic()获得;
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        //根据给定的密钥材料生成私钥对象
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        //Signature类用做签名的，一般通过自己的静态方法getInstance("算法名称")获取
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        //通过传入的私钥初始化待签名对象
        signature.initSign(privateK);
        //更新待签名或验证的数据
        signature.update(data);
        //返回所有已更新的签名字节
        return Base64Utils.encodeToString(signature.sign());
    }

    /**
     * @param data      已加密数据
     * @param publicKey 公钥(BASE64编码)
     * @param sign      数字签名  Base64编码字符串
     * @return boolean
     * @Author：
     * @Description: //TODO  公钥验签
     * @Date:
     **/
    public static boolean verify(byte[] data, String publicKey, String sign)
            throws Exception {
        //字符串解码为二进制数据
        byte[] keyBytes = Base64Utils.decodeFromString(publicKey);
        //密钥的X509编码
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        //KeyFactory一般通过自己的静态方法keyFactory.generatePublic()获得;
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        //根据给定的密钥材料生成公钥对象
        PublicKey publicK = keyFactory.generatePublic(keySpec);
        //Signature类用做签名的，一般通过自己的静态方法getInstance("算法名称")获取
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        //通过给定的公钥初始化对象
        signature.initVerify(publicK);
        //更新待签名或验证的数据
        signature.update(data);
        //验证待传入的签名
        return signature.verify(Base64Utils.decodeFromString(sign));
    }


    /**
     * @param data      源数据
     * @param publicKey 公钥(BASE64编码)
     * @return java.lang.String  Base64编码字符串
     **/
    public static String encryptByPublicKey(byte[] data, String publicKey)
            throws Exception {
        byte[] keyBytes = org.springframework.util.Base64Utils.decodeFromString(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, publicK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        String s = org.springframework.util.Base64Utils.encodeToString(encryptedData);

        out.close();
        return s;
    }

    /**
     * @param encryptedData 已加密数据 Base64编码字符串
     * @return byte[]
     **/
    public static byte[] decryptByPrivateKey(String encryptedData, String privateKey)
            throws Exception {
        byte[] keyBytes = Base64Utils.decodeFromString(privateKey);
        byte[] data = org.springframework.util.Base64Utils.decodeFromString(encryptedData);
        PKCS8EncodedKeySpec x509KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePrivate(x509KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, publicK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * @param data       源数据
     * @param privateKey 私钥(BASE64编码)
     * @return java.lang.String  Base64编码字符串
     **/
    public static String encryptByPrivateKey(byte[] data, String privateKey)
            throws Exception {
        byte[] keyBytes = Base64Utils.decodeFromString(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, privateK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        String s = org.springframework.util.Base64Utils.encodeToString(encryptedData);
        out.close();
        return s;
    }

    /**
     * @param encryptedData 已加密数据 Base64字符串
     * @param publicKey     公钥(BASE64编码)
     * @return byte[]
     * @Author：
     * @Description: //TODO  公钥解密
     * @Date:
     **/
    public static byte[] decryptByPublicKey(String encryptedData, String publicKey)
            throws Exception {
        byte[] keyBytes = Base64Utils.decodeFromString(publicKey);
        byte[] data = org.springframework.util.Base64Utils.decodeFromString(encryptedData);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, publicK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }
}