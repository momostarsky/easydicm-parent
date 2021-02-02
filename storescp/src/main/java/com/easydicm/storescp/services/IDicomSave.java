package com.easydicm.storescp.services;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.PDVInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * @author dhz
 */
public interface IDicomSave {


    /***
     * DICOM File  Save To Persistent Storage
     * @param dcmFile   DicomFile
     * @param clsUid   SOpClassUID
     * @param instUid   SopIntanceUID
     * @param clientId   From Where  ClientId
     * @param applicationId    Applicaiton Identity
     */
    void dicomFilePersist(final PDVInputStream data, Attributes fileMetaInfomation, File storageDir,
                          String clientId,
                          String applicationId, Attributes rsp) throws IOException, RemotingException, MQClientException, InterruptedException;

}
