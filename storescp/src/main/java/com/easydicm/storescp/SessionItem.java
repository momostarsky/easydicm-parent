package com.easydicm.storescp;

import lombok.SneakyThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionItem {
    private static final int MMAPSIZE = Integer.MAX_VALUE - 1024;
    private final MappedByteBuffer mapBuffer;
    private int slicesCount;
    private final RandomAccessFile mapFile;
    private final Path data;


    @SneakyThrows
    public SessionItem(String sessionUid, File tmpDir) {

        data = Paths.get(tmpDir.getAbsolutePath(), sessionUid + ".data");

        mapFile = new RandomAccessFile(data.toFile(), "rw");
        mapBuffer = mapFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, MMAPSIZE);
        slicesCount = 0;


    }

    public MappedByteBuffer getMapBuffer() {
        return mapBuffer;
    }

    public int getSlicesCount() {
        return slicesCount;
    }

    public void SlicesCountIncrment() {
        slicesCount += 1;
    }

    public void clear() {
        mapBuffer.clear();
        if (data.toFile().exists()) {
            data.toFile().delete();
        }

    }


}
