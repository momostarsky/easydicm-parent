package com.easydicm.storescp.services.impl;

import com.easydicm.storescp.SessionItem;
import com.easydicm.storescp.services.SessionFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;


public class SesssionFactoryImpl implements SessionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SesssionFactoryImpl.class);
    private static final Map<String, SessionItem> sessions = new ConcurrentHashMap<>(100);
    private final ExecutorService executorPools;
    private final ThreadFactory namedThreadFactory;
    private File tmpDir;

    public SesssionFactoryImpl() {
        namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("SessionFactory-%d").build();
        executorPools = Executors.newCachedThreadPool(namedThreadFactory);
    }


    @Override
    public boolean SessionRegister(String sessionUid) {
        sessions.put(sessionUid, new SessionItem(sessionUid, this.tmpDir));
        return sessions.containsKey(sessionUid);
    }


    @Override
    public void setTemplateDirectory(File directory) throws IOException {


        if (!directory.exists()) {
            directory.mkdirs();
        }
        Path dx = Paths.get(directory.getAbsolutePath(), "tempDirectoryTest.txt");
        FileUtils.write(dx.toFile(), "这个是用来测试权限的！", StandardCharsets.UTF_8, false);
        this.tmpDir = directory;

    }

    @Override
    public void writeDicomInfo(String sessionUid, final String cuid, final String iuid, final String tsuid, final byte[] arr) {

        SessionItem sessionItem = sessions.get(sessionUid);
        MappedByteBuffer mapBuffer = sessionItem.getMapBuffer();

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

        sessionItem.SlicesCountIncrment();

    }

    @Override
    public void saveDicomInfo(String sessionUid, final File dicomFileSaveDir) {

        SessionItem sessionItem = sessions.get(sessionUid);
        try {
            StopWatch sw = new StopWatch();
            sw.start();
            MappedByteBuffer mapBuffer = sessionItem.getMapBuffer();
            int allItems = sessionItem.getSlicesCount();
            mapBuffer.flip();
            for (int idx = 0; idx < allItems; idx++) {
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
                        Path save = Paths.get(dicomFileSaveDir.getAbsolutePath(), patId, stdId, serId, sopUid + ".dcm");
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
            sessionItem.clear();
            sw.stop();
            LOG.debug("Generate Dicom Files :{} with {} MS", allItems, sw.getTime(TimeUnit.MILLISECONDS));

        }
        finally {
            sessions.remove(sessionUid);
        }


    }

}
