package com.easydicm.storescp.services;

import java.io.File;


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
      void  dicomFilePersist(File dcmFile,String clsUid, String instUid,String clientId ,String applicationId) ;

}
