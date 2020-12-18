package com.easydicm.scputil;

import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RsaHelperTest {


    @Test
    public void dotnet2Javat() throws Exception {


        File save2 = new File("./rsakey/C919");
        String pkname = "CJ1.pubkey", prname = "CJ1.prikey";

        RSAUtil2048.genKeyPairByFilePath(save2, pkname, prname);

        File pkFile = Paths.get(save2.getAbsolutePath(), pkname).toFile();
        File prFile = Paths.get(save2.getAbsolutePath(), prname).toFile();

        assertTrue(pkFile.exists());
        assertTrue(prFile.exists());
        String pubkey = Files.asCharSource(pkFile, StandardCharsets.UTF_8).read();
        String prikey = Files.asCharSource(prFile, StandardCharsets.UTF_8).read();


        String data = "b52ae6a3-1edb-35ce-b777-35733ddb57db:CJ10001 public static String publicDecrypt(String data, RSAPublicKey publicKey) {\n" +
                "        try {" +
                "            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);" +
                "            cipher.init(Cipher.DECRYPT_MODE, publicKey);" +
                "            return new String(rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, Base64.decodeBase64(data), publicKey.getModulus().bitLength()), CHARSET);\n" +
                "        } catch (Exception e) {" +
                "            throw new RuntimeException(\"解密字符串[\" + data + \"]时遇到异常\", e);" +
                "        }" +
                "    }";
        byte[] dataBuffer = data.getBytes(StandardCharsets.UTF_8);
        String dataEncrypted = RSAUtil2048.encryptByPrivateKey(dataBuffer, prikey);
        byte[] dataDe = RSAUtil2048.decryptByPublicKey(dataEncrypted, pubkey);
        String txtDe = new String(dataDe, StandardCharsets.UTF_8);
        assertTrue(txtDe.equals(data));

        String sign = RSAUtil2048.sign(dataBuffer, prikey);
        boolean aok = RSAUtil2048.verify(dataBuffer, pubkey, sign);
        assertTrue(aok);


        String dataEncryptedX = RSAUtil2048.encryptByPublicKey(dataBuffer, pubkey);
        byte[] dataDeX = RSAUtil2048.decryptByPrivateKey(dataEncryptedX, prikey);
        String txtDeX = new String(dataDeX, StandardCharsets.UTF_8);
        assertTrue(txtDeX.equals(data));


        String netPubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAus/pIWOvSVUADsTCU1nF6iDMTYv3CHUQRCFJMVwV6XsDlwsSetHergNpzwNglGlFEQfEyGEGKYH5OWwqI0HPKIlYs3KQ9jbUaR++oRpTcgXZgzZN4lVecSchPiCSBYRWAuCMLoV4joLm7uwKVoQlLYG/7yj8c3LacznL1pYBKOff+xfnQxdZ1dLkfo9HwATJGlWYp5RLRsPbDVpbr5aUEyp4Pyr4XQ/T90BbkHfSVvlCdJdP2MLkGTNqoJNTcsiYcyFDduoY+z4KTJTi3Kjac2fLH8Qvfkwt+Vhl38O6aKusju/NhhwxNGGb/KwaA03SI30ujTiJO/beuXlNcCFOtQIDAQAB";
        String netEncryptedData = "ctsJBWwzpxtKr6+XXlomr2colqVOg+F9z8/LuqXuBMP5dHhCIU1PIkbZNbH0l+VLA05Ku4228aS73lLiGipYOIMTtvEZ+Mw+sQZE0eihFPhPmXxhbr4dd2LOoG23p9jJhxjSWUi8Iv6rK38xx6UJn6UrIPC0seNldQRZh8Mm0g1boWayqFk8R6DtQpNaD6WP7IzeoVZmnX0KQofhuLg9AYOP8h+jxAEuwBWdSTmvvlYd7kq5AEflzM/w6ONIcdCk8WUOY7LnsTASvbx1kvykpw+iaijpY67Zan11fubzGkYg7+taqdGyS4Vv5DTbNACNg6ew5Ht8yod5Syil5/v0wg==";
        String netEncryptedSign = "pa9n3UxDVuRdI7TA1Ms2Zsr6v6BkjUfS7PXhQErXAeJjqQfWqnW7hyaSlOt1dSkN7mqN6yLMRvYaD6o8VNwzW6tyPTmL+Tn4QRQn16xtWrVmyJflTouLU5m3P+bGui0y7Xm5c0jKTi3L1i2EXDZr76Ob56iUgClM4iQzRdfgrG4y9h5arUeWJhdXP7u/5bGuRtG134Bsul6z1oStB5kekq8sIgO58aaNT7czlxYVGqhnSVqExviCFzw+n11boYzzj82fU2S7Symb8e8U4gypMrqJp5GeXgwe4Fv/UGpHjzm1UU3yvVjNR8ZrIqAGhtNAVgc5uBU9UNEHdpMqcJxCOQ==";


        boolean ok = RSAUtil2048.verify(Base64.getDecoder().decode(netEncryptedData), netPubKey, netEncryptedSign);

        assertTrue(ok);


    }


}