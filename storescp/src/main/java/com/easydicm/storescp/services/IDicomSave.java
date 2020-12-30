package com.easydicm.storescp.services;

import org.dcm4che3.data.Attributes;

import java.io.File;
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
    void dicomFilePersist(ByteBuffer byteBuffer, Attributes fileMetaInfomation, File storageDir,
                          String clientId,
                          String applicationId, Attributes rsp);

}
