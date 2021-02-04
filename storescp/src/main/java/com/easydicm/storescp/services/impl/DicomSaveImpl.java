package com.easydicm.storescp.services.impl;


import com.easydicm.scpdb.mapper.IDbPatientMapper;
import com.easydicm.storescp.services.IDicomSave;
import com.easydicm.storescp.services.IMessageQueueWriter;
import com.easydicm.storescp.services.IStorageWriter;
import com.easydicm.storescp.services.StoreInfomation;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBufInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.*;


/**
 * @author dhz
 */
@Service
public class DicomSaveImpl extends BaseImpl implements IDicomSave {


    private IDbPatientMapper dbPatientMapper;

    private IStorageWriter storageWriter;


    private IMessageQueueWriter messageQueueWriter;


    private final ExecutorService executorPools;
    private final ThreadFactory namedThreadFactory;

    public DicomSaveImpl(@Autowired IDbPatientMapper dbPatientMapper, @Autowired IStorageWriter storageWriter, @Autowired IMessageQueueWriter messageQueueWriter) {

        super();
        this.dbPatientMapper = dbPatientMapper;
        this.storageWriter = storageWriter;
        this.messageQueueWriter = messageQueueWriter;


        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = 10 * corePoolSize;
        long keepAliveTime = 10L;

        namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("StoreScp-pool-%d").build();
        executorPools = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(), namedThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );


    }

    @Override
    public void dicomFilePersist(final File storageDir, final byte[] buffer, StoreInfomation storeInfomation) {

        executorPools.submit(() -> {
//            String iuid = storeInfomation.getFileMetaInfomation().getString(Tag.AffectedSOPInstanceUID);
//            String ts = storeInfomation.getFileMetaInfomation().getString(Tag.TransferSyntaxUID);
//            Path dcmpath = Paths.get(storageDir.getAbsolutePath(), storeInfomation.getClientId(), iuid + ".dcm");
//            boolean ok = true;
//            if (!dcmpath.getParent().toFile().exists()) {
//                ok = dcmpath.getParent().toFile().mkdirs();
//            }
//            if (ok) {
//                try (DicomInputStream dicomInputStream = new DicomInputStream(new ByteArrayInputStream(buffer));
//                     DicomOutputStream dicomOutputStream = new DicomOutputStream(dcmpath.toFile())
//                ) {
//                    Attributes ds = dicomInputStream.readDataset(-1, Tag.PixelData);
//                    dicomOutputStream.writeFileMetaInformation(storeInfomation.getFileMetaInfomation());
//                    dicomOutputStream.write(buffer);
//                    dicomOutputStream.flush();
//                    LOG.info("DICOM文件写入磁盘: {}",  dcmpath);
//
//                } catch (IOException e) {
//                    LOG.error("DICOM文件写入磁盘失败:{}", e);
//                }
//            } else {
//                LOG.info("创建存储目录失败：{}", dcmpath);
//            }
        });


    }
}

