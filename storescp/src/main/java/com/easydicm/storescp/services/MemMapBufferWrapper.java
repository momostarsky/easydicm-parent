package com.easydicm.storescp.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class MemMapBufferWrapper {


    /***
     *   每个文件1G 的容量
     */
    private final long segmentSize = 1024L * 1024 * 1024;
    ;
    private final Path filePath;
    private final long mapSize;
    private final int bufferSize;

    private final RandomAccessFile mapFile;
    private final MappedByteBuffer[] buffers;

    public MemMapBufferWrapper(final Path filePath, final long mapSize) throws IOException {
        this.filePath = filePath;
        this.mapSize = mapSize;
        long gcd = mapSize / segmentSize;
        long gdf = mapSize % segmentSize;

        if (gdf > 0) {
            bufferSize = (int) gcd + 1;
        } else {
            bufferSize = (int) gcd;
        }
        buffers = new MappedByteBuffer[bufferSize];
        mapFile = new RandomAccessFile(filePath.toFile(), "rw");
        for (int i = 0; i < bufferSize; i++) {
            long size = i == bufferSize - 1 ? gdf : segmentSize;
            buffers[i] = mapFile.getChannel().map(FileChannel.MapMode.READ_WRITE, i * segmentSize, size);
        }
    }

    public MappedByteBuffer[] getBuffers() {
        return buffers;
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getMapSize() {
        return mapSize;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public long getSegmentSize() {
        return segmentSize;
    }
}
