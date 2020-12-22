package com.easydicm.storescp.services;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.service.DicomService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;


public interface DicomSave {



      void  dicomFilePersist(File attributes,String clsUid, String instUid,String clientId ,String applicationId) ;

}
