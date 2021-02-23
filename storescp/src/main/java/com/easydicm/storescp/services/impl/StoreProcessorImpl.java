package com.easydicm.storescp.services.impl;

import com.easydicm.storescp.services.StoreProcessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

public class StoreProcessorImpl implements StoreProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(StoreProcessorImpl.class);
    private final File tmpDir;
    private final File sorageDir;

    private static final int MMAPSIZE = Integer.MAX_VALUE - 1024;
    private final MappedByteBuffer mapBuffer;

    private final RandomAccessFile mapFile;
    private final Path data;
    private int sliceCount;

    private final File tsp;
    private final File ssp;

//    private final ExecutorService executorPools;
//    private final ThreadFactory namedThreadFactory;
//     static void clean(final Object buffer) throws Exception {
//        AccessController.doPrivileged(new PrivilegedAction() {
//            public Object run() {
//                try {
//                    Method getCleanerMethod = buffer.getClass().getMethod("cleaner",new Class[0]);
//                    getCleanerMethod.setAccessible(true);
//                    sun.misc.Cleaner cleaner =(sun.misc.Cleaner)getCleanerMethod.invoke(buffer,new Object[0]);
//                    cleaner.clean();
//                } catch(Exception e) {
//                    e.printStackTrace();
//                }
//                return null;}});
//    }


    public StoreProcessorImpl(String sessionUid, File storageDir, File tempDir) throws IOException {

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        String fileName = String.format("%s.txt", sessionUid);
        String content = String.format("%s", sessionUid);
        tsp = Paths.get(tempDir.getAbsolutePath(), fileName).toFile();
        ssp = Paths.get(storageDir.getAbsolutePath(), fileName).toFile();
        FileUtils.write(tsp, content, StandardCharsets.UTF_8);
        FileUtils.write(ssp, content, StandardCharsets.UTF_8);
        this.tmpDir = tempDir;
        this.sorageDir = storageDir;
        data = Paths.get(tmpDir.getAbsolutePath(), sessionUid + ".data");
        mapFile = new RandomAccessFile(data.toFile(), "rw");
        mapBuffer = mapFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, MMAPSIZE);
        //---添加一个计数器
        mapBuffer.putInt(0);


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


    protected void createDicomFiles(final int posx, final int dataSize) throws IOException {
        // final ByteBuffer memBuffer = ByteBuffer.wrap(buffer);
        MappedByteBuffer finalMemBuffer = mapFile.getChannel().map(FileChannel.MapMode.READ_WRITE, posx, dataSize);
        try {
            byte[] fmiData = new byte[64 * 3];
            int cuidSize = finalMemBuffer.getInt();
            finalMemBuffer.get(fmiData, 0, cuidSize);

            int iuidSize = finalMemBuffer.getInt();
            finalMemBuffer.get(fmiData, 64, iuidSize);

            int tsSize = finalMemBuffer.getInt();
            finalMemBuffer.get(fmiData, 128, tsSize);

            final Attributes fmi = Attributes.createFileMetaInformation(
                    new String(fmiData, 0, iuidSize, StandardCharsets.UTF_8),
                    new String(fmiData, 64, cuidSize, StandardCharsets.UTF_8),
                    new String(fmiData, 128, tsSize, StandardCharsets.UTF_8)
            );

            int arrSize = finalMemBuffer.getInt();
            //int pos = finalMemBuffer.position();
            try (InputStream inputStream = new ByteBufferBackedInputStream(finalMemBuffer);
                 DicomInputStream dis = new DicomInputStream(inputStream)) {
                Attributes attr = dis.readDataset(-1, Tag.PixelData);
                String patId = attr.getString(Tag.PatientID, "");
                String stdId = attr.getString(Tag.StudyInstanceUID, "");
                String serId = attr.getString(Tag.SeriesInstanceUID, "");
                String sopUid = attr.getString(Tag.SOPInstanceUID);
                Path save = Paths.get(sorageDir.getAbsolutePath(), patId, stdId, serId, sopUid + ".dcm");
                if (!save.getParent().toFile().exists()) {
                    if (!save.getParent().toFile().mkdirs()) {
                        LOG.error("没有写入权限:{}", save.getParent());
                    }
                }
                inputStream.reset();
                try (DicomOutputStream dos = new DicomOutputStream(save.toFile())) {
                    dos.writeFileMetaInformation(fmi);
                    StreamUtils.copy(inputStream, dos, arrSize);
                    dos.flush();
                }
                LOG.info("Save DICOM  File {}", save);
            }
        } finally {
            finalMemBuffer.clear();

        }


    }


    @Override
    public void saveDicomInfo() throws IOException {

        //
        //---切片个数也写入缓冲区文件
        //
        final int pose = mapBuffer.position();
        /*
 持久化到磁盘
        mapFile.getChannel().truncate(pose);
        mapFile.getChannel().force(true);
*/


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
            final int dataSize = mapBuffer.getInt();
            final int pos = mapBuffer.position();
            Assert.isTrue(dataSize + pos <= MMAPSIZE, "磁盘映射尺寸超出范围!");
            //--不要使用线程池,顺序读比线程池的切换速度更快
            try {
                createDicomFiles(pos, dataSize);
            } catch (IOException ioException) {
                LOG.error("生成DICOM  切片失败:{}", ioException);
            }
            mapBuffer.position(pos + dataSize);
        }

        sw.stop();
        LOG.debug("Generate Dicom Files :{} with {} MS", recordCount, sw.getTime(TimeUnit.MILLISECONDS));
    }


    @Override
    public void clear() {

        tsp.delete();
        ssp.delete();
        data.toFile().delete();
    }

    class ByteBufferBackedInputStream extends InputStream {

        final MappedByteBuffer buf;

        final boolean bDuplicateUsage;

        ByteBufferBackedInputStream(MappedByteBuffer buf) {

            this.buf = buf;
            this.buf.mark();
            this.bDuplicateUsage = false;

        }

        ByteBufferBackedInputStream(MappedByteBuffer buf, boolean duplicateUsage) {

            this.buf = buf;
            this.buf.mark();
            this.bDuplicateUsage = duplicateUsage;

        }

        public synchronized int read() {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get();
        }

        public synchronized int read(byte[] bytes, int off, int len) {
            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }

        @Override
        public synchronized void reset() {
            this.buf.reset();
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void close() {
            //---如果
            if (bDuplicateUsage) {
                this.buf.reset();
            } else {
                this.buf.clear();
            }


        }

        @Override
        public int available() {
            return this.buf.capacity() - this.buf.position();
        }

    }

}
