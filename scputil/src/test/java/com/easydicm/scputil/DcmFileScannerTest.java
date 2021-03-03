package com.easydicm.scputil;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

class DcmFileScannerTest {

    byte[] intToBytes(final int data) {
        return new byte[]{
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data     ) & 0xff),
        };
    }

    int intFromBytes(final byte[] data) {
        if (data == null || data.length != 4) {
            return 0x0;
        }

        return  (
                (0xff & data[0]) << 24 |
                        (0xff & data[1]) << 16 |
                        (0xff & data[2]) << 8 |
                        (0xff & data[3])
        );
    }


    @SneakyThrows
    void createLargeFile(String largeDataFilePath) {
        File root = new File("/home/dhz/dcmdata/datax");
        File root2 = new File("/home/dhz/dcmdata/sample-data");
        Collection<File> x = new ArrayList<>();
        Files.walk(root.toPath())
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> x.add(path.toFile()));
        Files.walk(root2.toPath())
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> x.add(path.toFile()));

        Assert.isTrue(!x.isEmpty(), "查找文件失败");

        RandomAccessFile randomAccessFile = new RandomAccessFile(largeDataFilePath, "rw");
        AtomicLong bufferSize = new AtomicLong(0L);
        x.forEach(file -> {
            try {
                byte[] data = FileUtils.readFileToByteArray(file);
                int size = data.length;
                byte[] sizeArr = intToBytes(size);
                int sz = intFromBytes(sizeArr);
                Assert.isTrue(size == sz, "转换结果不对");
                randomAccessFile.write(sizeArr, 0, 4);
                randomAccessFile.write(data, 0, data.length);
                randomAccessFile.write(data, 0, data.length);
                bufferSize.addAndGet(2L * size + 4);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        Assert.isTrue(randomAccessFile.length() == bufferSize.get(), "文件尺寸不对");
        randomAccessFile.close();
    }

    @SneakyThrows
    @Test
    void scanFiles() {


        final long SEG = 1024L * 1024 * 1024;
        RandomAccessFile randomAccessFile = new RandomAccessFile("/home/dhz/dcmdata/larget.data", "rw");
        File wacc = new File("/home/dhz/dcmdata/large-copy.data");
        wacc.delete();
        long bufferSize = randomAccessFile.length();
        long ga = bufferSize / SEG;
        long gb = bufferSize % SEG;
        if (gb > 0L) {
            ga += 1;
        }
        int bs = (int) ga;
        MappedByteBuffer[] buffers = new MappedByteBuffer[bs];


        for (int i = 0; i < bs; i++) {
            buffers[i] = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE,   i * SEG, i == (bs - 1) ? gb : SEG);
        }
        byte[] tmp = new byte[128 * 1024 * 1024];
        for (int i = 0; i < bs; i++) {

            MappedByteBuffer mem = buffers[i];
            int gcd = mem.limit() / tmp.length;
            int gcv = mem.limit() % tmp.length;
            for (int si = 0; si < gcd; si++) {
                mem.get(tmp, 0, tmp.length);
                FileUtils.writeByteArrayToFile(wacc, tmp, 0, tmp.length, true);
            }
            if (gcv > 0) {
                mem.get(tmp, 0, gcv);
                FileUtils.writeByteArrayToFile(wacc, tmp, 0, gcv, true);
            }
        }


        for (int i = 0; i < bs; i++) {
            buffers[i].clear();
        }
        Assert.isTrue(wacc.length() == randomAccessFile.length(), "写入数据丢失");

        randomAccessFile.seek(0L);

        randomAccessFile.close();

        byte[] tmpx = new byte[128 * 1024 * 1024];


        try (InputStream r1 = com.google.common.io.Files.asByteSource(new File("/home/dhz/dcmdata/larget.data")).openStream();
             InputStream r12 = com.google.common.io.Files.asByteSource(new File("/home/dhz/dcmdata/large-copy.data")).openStream()) {

            while (true) {
                int k1 = r1.read(tmp, 0, tmp.length);
                int k2 = r12.read(tmpx, 0, tmpx.length);
                Assert.isTrue(k1 == k2, "读取缓冲区长度不一致");
                for (int x = 0; x < k1; x++) {
                    Assert.isTrue(tmp[x] == tmpx[x], "读取缓冲区长度不一致");
                }
                if (k1 != tmp.length) {
                    break;
                }
            }


        }


    }




    @SneakyThrows
    @Test
    void mapPool() {
        //
        // 用一个比较小的数字方便计算
        //
        MemPool mp=new MemPool(33L);
        MemPool.MemNode[] nodes = mp.getNodes();
        Assert.isTrue(  nodes.length == 4 ,"个数设置失败");
        for(int i=0;i<3;i++){
            Assert.isTrue(  nodes[i].getSegIndex()  == i ,"sn  设置失败");
            Assert.isTrue(  nodes[i].getSegMapPostion() == i * 10  ,"Pos  设置失败");
            Assert.isTrue(  nodes[i].getSegMapSize() ==  10  ,"Cap  设置失败");
            Assert.isTrue(  nodes[i].getNxtPode() != null  ,"Cap  设置失败");
            Assert.isTrue(  nodes[i].getNxtPode().getSegIndex() ==  i+ 1  ,"Cap  设置失败");
        }
        Assert.isTrue(  nodes[3].getSegIndex() == 3 ,"Sn 设置失败");
        Assert.isTrue(  nodes[3].getSegMapSize() == 3 ,"Cap 设置失败");
        Assert.isTrue(  nodes[3].getSegMapPostion() == 30 ,"Pos 设置失败");
        Assert.isTrue(  nodes[3].getNxtPode() == null  ,"NxtNode 设置失败");
        for(int i=1;i<4;i++){
            Assert.isTrue(  nodes[i].getPreNode() != null  ,"PreNode  设置失败");
            Assert.isTrue(  nodes[i].getPreNode().getSegIndex() ==  i- 1  ,"PreNodeSn 设置失败");
        }

        Assert.isTrue( mp.getPosition() == 0L ,"位置指针错误");
        Assert.isTrue( mp.currentNode().getSegIndex()== 0L ,"位置指针错误");
        long cp = mp.getPosition();
        Assert.isTrue( cp == 0L ,"位置指针错误");

        long np = mp.seekPosiont(3L);
        Assert.isTrue(mp.currentNode().getSegIndex() == 0,"SegIndex Error");
        Assert.isTrue(mp.currentNode().getDataBuffer().position() == 3,"SegIndex Error");
        Assert.isTrue(np  == 3L,"SegIndex Error");

        Assert.isTrue(mp.getPosition()  == 3L,"SegIndex Error");

        long np2 = mp.seekPosiont(17L);
        Assert.isTrue(mp.currentNode().getSegIndex() == 1,"SegIndex Error");
        Assert.isTrue(mp.currentNode().getDataBuffer().position() == 7,"SegIndex Error");
        Assert.isTrue(np2  == 17L,"SegIndex Error");
        Assert.isTrue(mp.getPosition()  == 17,"SegIndex Error");


        long np3 = mp.seekPosiont(23L);
        Assert.isTrue(mp.currentNode().getSegIndex() == 2,"SegIndex Error");
        Assert.isTrue(mp.currentNode().getDataBuffer().position() == 3,"SegIndex Error");
        Assert.isTrue(np3  == 23L,"SegIndex Error");
        Assert.isTrue(mp.getPosition()  == 23,"SegIndex Error");

        mp.mark();
        long np4 = mp.seekPosiont(35L);
        Assert.isTrue(mp.currentNode().getSegIndex() == 3,"SegIndex Error");
        Assert.isTrue(mp.currentNode().getDataBuffer().position() == 3,"SegIndex Error");
        Assert.isTrue(np4  == 33L,"SegIndex Error");
        Assert.isTrue(mp.getPosition()  == 33,"SegIndex Error");

        long np5 = mp.seekPosiont(19L);
        Assert.isTrue(mp.currentNode().getSegIndex() == 1,"SegIndex Error");
        Assert.isTrue(mp.currentNode().getDataBuffer().position() == 9,"SegIndex Error");
        Assert.isTrue(np5  == 19L,"SegIndex Error");
        Assert.isTrue(mp.getPosition()  == 19,"SegIndex Error");

        mp.reset();
        Assert.isTrue(mp.currentNode().getSegIndex() == 2,"SegIndex Error");
        Assert.isTrue(mp.currentNode().getDataBuffer().position() == 3,"SegIndex Error");
        Assert.isTrue(mp.getPosition()  == 23,"SegIndex Error");


        long np6 = mp.seekPosiont(33L);
        Assert.isTrue(mp.currentNode().getSegIndex() == 3,"SegIndex Error");
        Assert.isTrue(mp.currentNode().getDataBuffer().position() == 3,"SegIndex Error");
        Assert.isTrue(np6  == 33,"SegIndex Error");
        Assert.isTrue(mp.getPosition()  == 33,"SegIndex Error");



        {
            mp.seekPosiont(0L);
            byte[]  a1="123".getBytes(StandardCharsets.US_ASCII);
            mp.write(a1);
            Assert.isTrue(mp.currentNode().getSegIndex() == 0,"SegIndex Error");
            Assert.isTrue(mp.currentNode().getDataBuffer().position() == 3,"SegIndex Error");
            Assert.isTrue(mp.getPosition()  == 3,"SegIndex Error");
        }
        {
            mp.seekPosiont(0L);
            byte[]  a1="0123456789".getBytes(StandardCharsets.US_ASCII);
            mp.write(a1);
            Assert.isTrue(mp.currentNode().getSegIndex() == 1,"SegIndex Error");
            Assert.isTrue(mp.currentNode().getDataBuffer().position() == 0,"SegIndex Error");
            Assert.isTrue(mp.getPosition()  == 10,"SegIndex Error");
        }

        {
            mp.seekPosiont(0L);
            byte[]  a1="0123456789ABC".getBytes(StandardCharsets.US_ASCII);
            mp.write(a1);
            Assert.isTrue(mp.currentNode().getSegIndex() == 1,"SegIndex Error");
            Assert.isTrue(mp.currentNode().getDataBuffer().position() == 3,"SegIndex Error");
            Assert.isTrue(mp.getPosition()  == a1.length,"SegIndex Error");
        }
        int seg = (int)MemPool.MAP_SEG;
        {
            mp.seekPosiont(0L);
            byte[]  a1="0123456789ABCDEQWERTabcdezxcvb!@#".getBytes(StandardCharsets.US_ASCII);
            mp.write(a1);
            Assert.isTrue(a1.length == mp.getMapSize(),"长度是否一致");
            Assert.isTrue(mp.currentNode().getSegIndex() == a1.length / seg ,"SegIndex Error");
            Assert.isTrue(mp.currentNode().getDataBuffer().position() == a1.length % seg ,"SegIndex Error");
            Assert.isTrue(mp.getPosition()  == a1.length,"SegIndex Error");
            for(MemPool.MemNode mn : mp.getNodes()){
                mn.getDataBuffer().position(0);
                byte[]  a2 =new byte[ (int)mn.getSegMapSize() ];
                mn.getDataBuffer().get(a2);
                Assert.isTrue(a2.length == mn.getSegMapSize() ,"内存块是否一致");
                String tx =new String(a2, StandardCharsets.US_ASCII);
                System.out.println(tx);
            }
            mp.mark();
            mp.seekPosiont(0L);
            byte[] buffer=new byte[33];
            mp.read(buffer);
            Assert.isTrue(a1.length == buffer.length,"长度是否一致");
            for(int i=0;i<33;i++){
                Assert.isTrue(buffer[i]== a1[i],"内容不一致");
            }
            String tx2 =new String(buffer, StandardCharsets.US_ASCII);
            System.out.println(tx2);
        }
        {
            mp.seekPosiont(0L);
            byte[]  a1="0123456789ABCDEQWERTabcdezxcvb!@#+_*".getBytes(StandardCharsets.US_ASCII);
            mp.write(a1);
            Assert.isTrue(mp.currentNode().getSegIndex() == 0,"SegIndex Error");
            Assert.isTrue(mp.currentNode().getDataBuffer().position() ==0,"SegIndex Error");
            Assert.isTrue(mp.getPosition()  ==0,"SegIndex Error");
        }

        mp.close();

    }
}
