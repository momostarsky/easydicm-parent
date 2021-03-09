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
    @Test
    void scanFiles() {


        final  String path="/home/dhz/dcmdata/larget.data";
        final  String ojk = "/home/dhz/dcmdata/large-copy-ojk.data";
        final long SEG = 1024L * 1024 * 1024;
        RandomAccessFile randomAccessFile = new RandomAccessFile(path, "rw");
        File wacc = new File(ojk);

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


        try (InputStream r1 = com.google.common.io.Files.asByteSource(new File(path)).openStream();
             InputStream r12 = com.google.common.io.Files.asByteSource(new File( ojk)).openStream()) {

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


    }
}
