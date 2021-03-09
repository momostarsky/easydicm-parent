package com.easydicm.storescp.services.impl;


import com.easydicm.scpdb.mapper.IDbPatientMapper;
import com.easydicm.storescp.services.IDicomSave;
import com.easydicm.storescp.services.IMessageQueueWriter;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * @author dhz
 */
@Service
public class DicomSaveImpl extends BaseImpl implements IDicomSave {


    private IDbPatientMapper dbPatientMapper;


    private IMessageQueueWriter messageQueueWriter;



    private  String  storagePath;

    public DicomSaveImpl(@Autowired IDbPatientMapper dbPatientMapper, @Autowired IMessageQueueWriter messageQueueWriter) {

        super();
        this.dbPatientMapper = dbPatientMapper;

        this.messageQueueWriter = messageQueueWriter;


    }


    @Override
    public Path computeSavePath(Attributes attr) {
        String patId = attr.getString(Tag.PatientID, "");
        String stdId = attr.getString(Tag.StudyInstanceUID, "");
        String serId = attr.getString(Tag.SeriesInstanceUID, "");
        String sopUid = attr.getString(Tag.SOPInstanceUID);
        Path save = Paths.get(  this.storagePath, patId, stdId, serId, sopUid + ".dcm");
        if (!save.getParent().toFile().exists()) {
            save.getParent().toFile().mkdirs();
        }
        return  save;
    }

    @Override
    public void storagePath(String storage) {

        this.storagePath = storage;

    }
}

