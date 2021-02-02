package com.easydicm.scputil;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SessionManager {

    public static final AtomicInteger TOTAL_MAPPED_VIRTUAL_MEMORY = new AtomicInteger(0);
    public static final AtomicInteger TOTAL_MAPPED_FILES = new AtomicInteger(0);
    public FileChannel fileChannel = null;
    public final MappedByteBuffer mappedByteBuffer;
    public final File file;
    public int fileSize = Integer.MAX_VALUE - 1024;

    public final AtomicInteger wrotePosition = new AtomicInteger(0);
    public final AtomicInteger committedPosition = new AtomicInteger(0);
    public final AtomicInteger flushedPosition = new AtomicInteger(0);

    public final Map<Integer, Integer> dataMap = new ConcurrentHashMap<>();

    public SessionManager(String sessionId) throws IOException {
        boolean ok = false;
        try {
            this.file = new File("./" + sessionId);
            this.fileChannel = new RandomAccessFile(this.file, "rw").getChannel();
            this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            TOTAL_MAPPED_VIRTUAL_MEMORY.addAndGet(fileSize);
            TOTAL_MAPPED_FILES.incrementAndGet();
            ok = true;
        } catch (FileNotFoundException e) {

            throw e;
        } catch (IOException e) {

            throw e;
        } finally {
            if (!ok && this.fileChannel != null) {
                this.fileChannel.close();
            }
        }

    }

    /***
     * 添加消息信息
     * @param data
     * @return
     */
    public boolean appendMessage(final byte[] data) {
        int currentPos = this.wrotePosition.get();

        if ((currentPos + data.length) <= this.fileSize) {
            try {
                this.fileChannel.position(currentPos);
                this.fileChannel.write(ByteBuffer.wrap(data));
            } catch (Throwable e) {
                log.error("Error occurred when append message to mappedFile.", e);
            }
            this.wrotePosition.addAndGet(data.length);
            dataMap.put(currentPos, data.length);
            return true;
        }

        return false;
    }


    public Map<Integer, Integer> getDataMap() {
        return dataMap;
    }


    @PreDestroy
    public void dispose() {

        try {
            this.fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.file.delete();
    }


}
