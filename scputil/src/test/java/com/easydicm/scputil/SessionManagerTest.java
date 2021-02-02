package com.easydicm.scputil;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

class SessionManagerTest {

    @Test
    public void bufferTest() throws IOException {

        String r1 = "Although a mapped write would seem to use a FileOutputStream";
        String r2 = "all output in file mapping must use a RandomAccessFile";
        String r3 = "just as read write does in the preceding code";
        byte[] d1 = r1.getBytes(StandardCharsets.UTF_8);
        byte[] d2 = r2.getBytes(StandardCharsets.UTF_8);
        byte[] d3 = r3.getBytes(StandardCharsets.UTF_8);
        Random ad = new Random(System.currentTimeMillis());
        int x = ad.nextInt();
        String uid = String.format("sid-%d", x);
        System.out.println("session-uid is :" + uid);
        SessionManager sm = new SessionManager(uid);
        sm.appendMessage(d1);
        sm.appendMessage(d2);
        sm.appendMessage(d3);

        Map<Integer, Integer> datam = sm.getDataMap();
        int  k1 = 0;
        int  k2 = d1.length;
        int  k3 = k2 + d2.length;
        Assert.isTrue(datam.containsKey(k1), "K1 not Exits ");
        Assert.isTrue(datam.containsKey(k2), "K2 not Exits ");
        Assert.isTrue(datam.containsKey(k3), "K3 not Exits ");


        System.out.println(sm.mappedByteBuffer.isLoaded());  //prints false
        System.out.println(sm.mappedByteBuffer.capacity());  //Get the size based on content size of file


        byte[] dataBuffer = new byte[200];
        ByteBuffer buffer = ByteBuffer.wrap(dataBuffer);// ByteBuffer.allocateDirect(200);
        {
            buffer.limit(d1.length);
            // sm.fileChannel.position(k1);
            sm.fileChannel.read(buffer, k1);
            String  txt="痛不痛";


            buffer.flip();

            String str = new String(dataBuffer, 0, d1.length, StandardCharsets.UTF_8);
            Assert.isTrue(str.equals(r1), "ss");
            buffer.clear();
        }
        {
            buffer.limit(d2.length);
            sm.fileChannel.position(k2);

            sm.fileChannel.read(buffer);
            buffer.flip();

            String str = new String(dataBuffer, 0, d2.length, StandardCharsets.UTF_8);
            Assert.isTrue(str.equals(r2), "第2行不匹配这个定位");
            buffer.clear();
        }
        {
            buffer.limit(d3.length);
            sm.fileChannel.position(k3);
            sm.fileChannel.read(buffer);
            buffer.flip();

            String str = new String(dataBuffer, 0, d3.length, StandardCharsets.UTF_8);
            Assert.isTrue(str.equals(r3), "第3行不匹配这个定位");
            buffer.clear();
        }

        sm.dispose();


    }

    @Test
    void appendMessage() throws IOException {

        String sid = "1110";
        SessionManager sm = new SessionManager(sid);

        Collection<File> dcmFiles = DcmFileScanner.scanFiles("./dcm/testdata");
        int sz = 0;
        int maxlen = 0;


        for (File dx : dcmFiles) {
            byte[] data = Files.asByteSource(dx).read();
            if (data.length > maxlen) {
                maxlen = data.length;
            }
            sm.appendMessage(data);
            sz += data.length;
        }
        Assert.isTrue(sz <= sm.mappedByteBuffer.limit(), "数据加载失败");
        Assert.isTrue(!dcmFiles.isEmpty() , "数据文件为空！");



        byte[] dataBuffer = new byte[maxlen];
        ByteBuffer buffer = ByteBuffer.wrap(dataBuffer);// ByteBuffer.allocateDirect(200);


        Map<Integer, Integer> dm = sm.getDataMap();
        for (Map.Entry<Integer, Integer> entry : dm.entrySet()) {
            int pos = entry.getKey() ;
            int len = entry.getValue() ;
            buffer.limit(len);
            sm.fileChannel.position(pos);
            sm.fileChannel.read(buffer);
            buffer.flip();
            FileUtils.writeByteArrayToFile(new File("./dcm/" + pos + ".dcm"), dataBuffer, 0, len);
            buffer.clear();
            // https://css-tricks.com/designing-a-javascript-plugin-system/
        }
        sm.dispose();


    }
}