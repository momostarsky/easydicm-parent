package com.easydicm.storescp;


import com.easydicm.storescp.services.IDicomSave;
import com.easydicm.storescp.services.StoreInfomation;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import freemarker.core.Environment;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.util.AttributesFormat;
import org.dcm4che3.util.SafeClose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;


/**
 * @author dhz
 */
@Component

public class StoreScp extends BasicCStoreSCP {

    static final Logger LOG = LoggerFactory.getLogger(StoreScp.class);


    private IDicomSave dicomSave;


    private final ExecutorService executorPools;
    private final ThreadFactory namedThreadFactory;

    public StoreScp(IDicomSave dicomSave) {
        super("*");

        this.dicomSave = dicomSave;
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


    private File storageDir;

    private void sleep(Association as, int[] delays) {
        int responseDelay = delays != null
                ? delays[(as.getNumberOfReceived(Dimse.C_STORE_RQ) - 1) % delays.length]
                : 0;
        if (responseDelay > 0) {
            try {
                Thread.sleep(responseDelay);
            } catch (InterruptedException ignore) {
            }
        }
    }


    public void setStorageDirectory(File storageDir) {
        if (storageDir != null) {
            storageDir.mkdirs();
        }
        this.storageDir = storageDir;
    }


    @Override
    protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp) throws IOException {


        String appid = "DicmQRSCP";
        String clientId = "HZJPDEV";
        if (as.containsProperty(GlobalConstant.AssicationApplicationId)) {
            appid = as.getProperty(GlobalConstant.AssicationApplicationId).toString();
        }
        if (as.containsProperty(GlobalConstant.AssicationClientId)) {
            clientId = as.getProperty(GlobalConstant.AssicationClientId).toString();
        }
        String sessionId = as.getProperty(GlobalConstant.AssicationSessionId).toString();
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        String tsuid = pc.getTransferSyntax();
        Attributes fmi = as.createFileMetaInformation(iuid, cuid, tsuid);
        final byte[] buffer = data.readAllBytes();
        final StoreInfomation storeInfomation = new StoreInfomation(appid, clientId, sessionId, fmi);
        // 提交到线程池中执行
        dicomSave.dicomFilePersist(storageDir, buffer, storeInfomation);
        rsp.setInt(Tag.Status, VR.US, Status.Success);

    }


}