package com.easydicm.storescp.services.impl;

import com.easydicm.storescp.services.DicomSave;
import org.springframework.stereotype.Service;

import java.io.File;


@Service
public class DicomSaveImpl implements DicomSave {
    @Override
    public void dicomFilePersist(File attributes, String clsUid, String instUid,String clientId ,String applicationId) {
        System.out.println( "ClientId:"+ clientId +"->AppId:" + applicationId +   " > instUid file Save:" + instUid);

    }
}
