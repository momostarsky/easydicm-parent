package com.easydicm.storescp.services.impl;

import com.easydicm.storescp.SessionItem;
import com.easydicm.storescp.services.StoreProcessor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class StoreProcessorImpl implements StoreProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(StoreProcessorImpl.class);
    private final File tmpDir;
    private final File sorageDir;

    private static final int MMAPSIZE = Integer.MAX_VALUE - 1024;
    private final MappedByteBuffer mapBuffer;
    private int slicesCount;
    private final RandomAccessFile mapFile;
    private final Path data;
    private int sliceCount;

    private final ExecutorService executorPools;
    private final ThreadFactory namedThreadFactory;

    public StoreProcessorImpl(String sessionUid, File storageDir, File tempDir) throws IOException {

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        String fileName = String.format("%s.txt", sessionUid);
        String content = String.format("%s", sessionUid);
        File tsp = Paths.get(tempDir.getAbsolutePath(), fileName).toFile();
        File ssp = Paths.get(storageDir.getAbsolutePath(), fileName).toFile();
        FileUtils.write(tsp, content, StandardCharsets.UTF_8);
        FileUtils.write(ssp, content, StandardCharsets.UTF_8);
        this.tmpDir = tempDir;
        this.sorageDir = storageDir;
        data = Paths.get(tmpDir.getAbsolutePath(), sessionUid + ".data");
        mapFile = new RandomAccessFile(data.toFile(), "rw");
        mapBuffer = mapFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, MMAPSIZE);
        //---添加一个计数器
        mapBuffer.putInt(0);
        namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("ScpImpl-%d").build();
        executorPools = Executors.newCachedThreadPool(namedThreadFactory);

    }

    @Override
    public void writeDicomInfo(String cuid, String iuid, String tsuid, byte[] arr) {

        byte[] cuidData = cuid.getBytes(StandardCharsets.UTF_8);
        int cuidSize = cuidData.length;

        byte[] iuidData = iuid.getBytes(StandardCharsets.UTF_8);
        int iuidSize = iuidData.length;

        byte[] tsData = tsuid.getBytes(StandardCharsets.UTF_8);
        int tsSize = tsData.length;

        int arrSize = arr.length;

        //----必须加上16 个字节（4 个整数）
        int dataSize = cuidSize + iuidSize + tsSize + arrSize + 16;
        mapBuffer.putInt(dataSize);

        mapBuffer.putInt(cuidSize);
        mapBuffer.put(cuidData);

        mapBuffer.putInt(iuidSize);
        mapBuffer.put(iuidData);

        mapBuffer.putInt(tsSize);
        mapBuffer.put(tsData);

        mapBuffer.putInt(arrSize);
        mapBuffer.put(arr);

        sliceCount += 1;

    }

    @Override
    public void saveDicomInfo() {

        //
        //---切片个数也写入缓冲区文件
        //
        final int pose = mapBuffer.position();
        mapBuffer.position(0);
        mapBuffer.putInt(sliceCount);
        mapBuffer.position(pose);
        StopWatch sw = new StopWatch();
        sw.start();
        mapBuffer.flip();
        int recordCount = mapBuffer.getInt();
        Assert.isTrue(recordCount == sliceCount, "切片个数不一致！");
        for (int idx = 0; idx < recordCount; idx++) {
            //---读取总长度
            int dataSize = mapBuffer.getInt();
            final byte[] buffer = new byte[dataSize];
            mapBuffer.get(buffer, 0, dataSize);
            executorPools.submit(() -> {
                final ByteBuffer memBuffer = ByteBuffer.wrap(buffer);
                byte[] fmiData = new byte[64 * 3];
                int cuidSize = memBuffer.getInt();
                memBuffer.get(fmiData, 0, cuidSize);

                int iuidSize = memBuffer.getInt();
                memBuffer.get(fmiData, 64, iuidSize);

                int tsSize = memBuffer.getInt();
                memBuffer.get(fmiData, 128, tsSize);

                final Attributes fmi = Attributes.createFileMetaInformation(
                        new String(fmiData, 0, iuidSize, StandardCharsets.UTF_8),
                        new String(fmiData, 64, cuidSize, StandardCharsets.UTF_8),
                        new String(fmiData, 128, tsSize, StandardCharsets.UTF_8)
                );

                int arrSize = memBuffer.getInt();

                ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(buffer, memBuffer.position(), arrSize);
                try (DicomInputStream dis = new DicomInputStream(arrayInputStream)) {
                    Attributes attr = dis.readDataset(-1, Tag.PixelData);
                    String patId = attr.getString(Tag.PatientID, "");
                    String stdId = attr.getString(Tag.StudyInstanceUID, "");
                    String serId = attr.getString(Tag.SeriesInstanceUID, "");
                    String sopUid = attr.getString(Tag.SOPInstanceUID);
                    Path save = Paths.get(sorageDir.getAbsolutePath(), patId, stdId, serId, sopUid + ".dcm");
                    if (!save.getParent().toFile().exists()) {
                        save.getParent().toFile().mkdirs();
                    }
                    try (DicomOutputStream dos = new DicomOutputStream(save.toFile())) {
                        dos.writeFileMetaInformation(fmi);
                        dos.write(buffer, memBuffer.position(), arrSize);
                        dos.flush();
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                memBuffer.clear();
            });
        }

        sw.stop();
        LOG.debug("Generate Dicom Files :{} with {} MS", recordCount, sw.getTime(TimeUnit.MILLISECONDS));
    }
}
