package com.easydicm.storescp;


import com.easydicm.storescp.services.IDicomSave;
import com.easydicm.storescp.services.StoreInfomation;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import freemarker.core.Environment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.util.AttributesFormat;
import org.dcm4che3.util.SafeClose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;


/**
 * @author dhz
 */
@Component

public class StoreScp extends BasicCStoreSCP {

    static final Logger LOG = LoggerFactory.getLogger(StoreScp.class);


    private IDicomSave dicomSave;


    public StoreScp(IDicomSave dicomSave) {
        super("*");

        this.dicomSave = dicomSave;


    }


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


    @Override
    protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp) throws IOException {
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        String tsuid = pc.getTransferSyntax();
        Attributes fmi = as.createFileMetaInformation(iuid, cuid, tsuid);
        byte[] arr = data.readAllBytes();

        ArrayList<StoreInfomation> pos = (ArrayList<StoreInfomation>) as.getProperty(GlobalConstant.AssicationSopPostion);
        MappedByteBuffer mapBuffer = (MappedByteBuffer) as.getProperty(GlobalConstant.AssicationSessionData);
        StoreInfomation storeInfomation = new StoreInfomation(fmi, mapBuffer.position(), arr.length);
        mapBuffer.put(arr);
        pos.add(storeInfomation);
        rsp.setInt(Tag.Status, VR.US, Status.Success);
    }


}