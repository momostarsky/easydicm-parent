package com.easydicm.storescp.services.impl;


import com.easydicm.scpdb.mapper.IDbPatientMapper;
import com.easydicm.storescp.services.IDicomSave;
import com.easydicm.storescp.services.IMessageQueueWriter;
import com.easydicm.storescp.services.IStorageWriter;
import com.google.common.io.ByteSource;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * @author dhz
 */
@Service
public class DicomSaveImpl extends BaseImpl implements IDicomSave {


    private IDbPatientMapper dbPatientMapper;
    private IStorageWriter storageWriter;
    private IMessageQueueWriter messageQueueWriter;

    @Autowired
    public void setPatientMapper(IDbPatientMapper dbPatientMapper) {
        this.dbPatientMapper = dbPatientMapper;
    }

    @Autowired
    public void setStorageWriter(IStorageWriter storageWriter) {
        this.storageWriter = storageWriter;
    }

    @Autowired
    public void setMessageQueueWriter(IMessageQueueWriter messageQueueWriter) {
        this.messageQueueWriter = messageQueueWriter;
    }

    public DicomSaveImpl() {

        super();
    }

    @Override
    public void dicomFilePersist(ByteBuffer byteBuffer, Attributes fileMetaInfomation,
                                 File storageDir,
                                 String clientId,
                                 String applicationId, Attributes rsp) {


        try (InputStream input = ByteSource.wrap(byteBuffer.array()).openStream();
             DicomInputStream dicomInputStream = new DicomInputStream(input)) {
            Attributes attribs = dicomInputStream.readDataset(-1, Tag.PixelData);
            String patId = attribs.getString(Tag.PatientID);
            String studyUid = attribs.getString(Tag.StudyInstanceUID);
            String serisUid = attribs.getString(Tag.SeriesInstanceUID);
            String sopUid = attribs.getString(Tag.SOPInstanceUID);
            if (StringUtils.isEmpty(patId)
                    || StringUtils.isEmpty(studyUid)
                    || StringUtils.isEmpty(serisUid)
                    || StringUtils.isEmpty(sopUid)
            ) {
                rsp.setInt(Tag.Status, VR.US, Status.MissingAttribute);
                return;
            }
            LOG.info("PatientId -{}", patId);
            LOG.info("StudyInstUID -{}", studyUid);
            LOG.info("SeriesUID -{}", serisUid);
            LOG.info("SopInstUID -{}", sopUid);
            Path dcmpath = Paths.get(storageDir.getAbsolutePath(), clientId, patId, studyUid, serisUid, sopUid + ".dcm");
            if (!dcmpath.getParent().toFile().exists()) {
                dcmpath.getParent().toFile().mkdirs();
            }
            try (DicomOutputStream dicomOutputStream = new DicomOutputStream(dcmpath.toFile())) {
                dicomOutputStream.writeFileMetaInformation(fileMetaInfomation);
                dicomOutputStream.write(byteBuffer.array());
                dicomOutputStream.flush();
            }
            rsp.setInt(Tag.Status, VR.US, Status.Success);

        } catch (IOException e) {
            rsp.setInt(Tag.Status, VR.US, Status.ProcessingFailure);
        }


    }
}
