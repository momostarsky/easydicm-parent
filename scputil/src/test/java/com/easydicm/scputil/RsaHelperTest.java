package com.easydicm.scputil;

import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RsaHelperTest {


    @Test

    public void TextEncrpt() {

        long temp = System.currentTimeMillis();
        //生成公钥和私钥
        Map<Integer, String> keyMap = RsaHelper.genKeyPair();
        //加密字符串
        System.out.println("公钥:" + keyMap.get(0));
        System.out.println("私钥:" + keyMap.get(1));
        System.out.println("生成密钥消耗时间:" + (System.currentTimeMillis() - temp) / 1000.0 + "秒");
        String errorMessage = "编程帮，一个分享编程知识的公众号。跟着站长一起学习，每天都有进步。" +
                "通俗易懂，深入浅出，一篇文章只讲一个知识点。" +
                "文章不深奥，不需要钻研，在公交、在地铁、在厕所都可以阅读，随时随地涨姿势。" +
                "文章不涉及代码，不烧脑细胞，人人都可以学习。" +
                "当你决定关注「编程帮」，你已然超越了90%的程序员！";

        try {
            RsaHelper.encrypt(errorMessage, keyMap.get(0));
        } catch (Exception e) {
            assertThrows(RsaException.class, () -> {
                throw e;
            });
        }


    }




    @Test
    public void TextEncrptPubDecryptPri() {

        try {
            long temp = System.currentTimeMillis();
            //生成公钥和私钥
            Map<Integer, String> keyMap = RsaHelper.genKeyPair();
            //加密字符串
            System.out.println("公钥:" + keyMap.get(RsaHelper.PUBLIC_KEY));
            System.out.println("私钥:" + keyMap.get(RsaHelper.PRIVATE_KEY));
            System.out.println("生成密钥消耗时间:" + (System.currentTimeMillis() - temp) / 1000.0 + "秒");
            String message = "它允许您在同一测试中测试多个异常 . 在Java 8中支持lambdas，这是在JUnit中测试异常的规范方法";
            String messageEn = RsaHelper.encrypt(message, keyMap.get(RsaHelper.PUBLIC_KEY));
            System.out.println("密文:" + messageEn);
            System.out.println("加密消耗时间:" + (System.currentTimeMillis() - temp) / 1000.0 + "秒");
            temp = System.currentTimeMillis();
            String messageDe = RsaHelper.decrypt(messageEn, keyMap.get(RsaHelper.PRIVATE_KEY));
            System.out.println("解密:" + messageDe);
            System.out.println("解密消耗时间:" + (System.currentTimeMillis() - temp) / 1000.0 + "秒");
            assertTrue(message.equals(messageDe));

        } catch (Exception e) {

        }
    }


    @Test
    public void TextEncrptPriDecryptPub() {

        try {
            long temp = System.currentTimeMillis();
            //生成公钥和私钥
            Map<Integer, String> keyMap = RsaHelper.genKeyPair();
            //加密字符串
            System.out.println("公钥:" + keyMap.get(RsaHelper.PUBLIC_KEY));
            System.out.println("私钥:" + keyMap.get(RsaHelper.PRIVATE_KEY));
            System.out.println("生成密钥消耗时间:" + (System.currentTimeMillis() - temp) / 1000.0 + "秒");
            String message = "它允许您在同一测试中测试多个异常 . 在Java 8中支持lambdas，这是在JUnit中测试异常的规范方法";
            String messageEn = RsaHelper.encrypt(message, keyMap.get(RsaHelper.PUBLIC_KEY));
            System.out.println("密文:" + messageEn);
            System.out.println("加密消耗时间:" + (System.currentTimeMillis() - temp) / 1000.0 + "秒");
            temp = System.currentTimeMillis();
            String messageDe = RsaHelper.decrypt(messageEn, keyMap.get(RsaHelper.PRIVATE_KEY));
            System.out.println("解密:" + messageDe);
            System.out.println("解密消耗时间:" + (System.currentTimeMillis() - temp) / 1000.0 + "秒");
            assertTrue(message.equals(messageDe));

        } catch (Exception e) {

        }
    }

    @Test
    public  void RsaPersistent() throws Exception {
        Map<Integer, String> keyMap = RsaHelper.genKeyPair();
        String pubkey = keyMap.get(RsaHelper.PUBLIC_KEY);
        String prikey = keyMap.get(RsaHelper.PRIVATE_KEY);

        PrivateKey privateKey = RsaHelper.loadPrivateKey(prikey);
        assertTrue( privateKey != null);
        PublicKey  publicKey = RsaHelper.loadPublicKey(pubkey);
        assertTrue(publicKey != null);

        // RSA公钥私钥的磁盘序列话
        RsaHelper.savePublicKey(publicKey, new File("./pub.keystore"));
        RsaHelper.savePrivateKey(privateKey, new File("./pri.keystore"));


        CharSource resultpub = Files.asCharSource(new File("./pub.keystore"),StandardCharsets.UTF_8);
        String apub = resultpub.read();
        assertTrue(  pubkey.equals( apub));

        CharSource resultpri = Files.asCharSource(new File("./pri.keystore"),StandardCharsets.UTF_8);
        String apri = resultpri.read();
        assertTrue(  prikey.equals( apri));


        RSAPrivateKey  kpri = RsaHelper.loadPrivateKey(apri);
        RSAPublicKey   kpub = RsaHelper.loadPublicKey(apub);
        String message = "它允许您在同一测试中测试多个异常 . 在Java 8中支持lambdas，这是在JUnit中测试异常的规范方法";
        System.out.println("原文:" + message);
        String messageEn = RsaHelper.encrypt(message,  kpri);
        System.out.println("密文:" + message);
        String messageDe = RsaHelper.decrypt(messageEn, kpub);
        System.out.println("解密:" + messageDe);
        assertTrue(message.equals(messageDe));



        System.out.println("---------------私钥签名过程------------------");
        String content="ihep_这是用于签名的原始数据";
        String signstr=RSASignature.sign(content, apri);
        System.out.println("签名原串："+content);
        System.out.println("签名串："+signstr);
        System.out.println();

        System.out.println("---------------公钥校验签名------------------");
        System.out.println("签名原串："+content);
        System.out.println("签名串："+signstr);

        System.out.println("验签结果："+RSASignature.doCheck(content, signstr,apub));
        System.out.println();




    }

    @Test
    public void signCheck(){

    }
}