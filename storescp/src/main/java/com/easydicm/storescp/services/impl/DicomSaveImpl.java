package com.easydicm.storescp.services.impl;

import com.easydicm.scpdb.mapper.PatientMapper;
import com.easydicm.storescp.services.IDicomSave;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;


/**
 * @author dhz
 */
@Service
public class DicomSaveImpl implements IDicomSave {


    static final Logger LOG = LoggerFactory.getLogger(DicomSaveImpl.class);



    @Override
    public void dicomFilePersist(File dcmFile, String clsUid, String instUid, String clientId, String applicationId) {
        LOG.info("ClientId:" + clientId + "->AppId:" + applicationId + " > instUid file Save:" + instUid);
        try (DicomInputStream dis = new DicomInputStream(dcmFile)) {
            Attributes ds = dis.readDataset(-1, Tag.PixelData);
            String patId = ds.getString(Tag.PatientID, "");
            String studyUid = ds.getString(Tag.StudyInstanceUID, "");
            String seriesUid = ds.getString(Tag.SeriesInstanceUID, "");
            String accessionNum = ds.getString(Tag.AccessionNumber, "");
            String modality = ds.getString(Tag.Modality, "");
            LOG.info(String.format("写入永久存储:  Azsure BLOB , AWS, AliyunOSS  With :%s, %s, %s,%s,%s", patId, studyUid, seriesUid, accessionNum, modality));
            LOG.info(String.format("写入消息队列 With :%s, %s, %s,%s,%s", patId, studyUid, seriesUid, accessionNum, modality));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
