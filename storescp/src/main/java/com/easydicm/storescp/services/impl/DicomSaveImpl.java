package com.easydicm.storescp.services.impl;


import com.easydicm.scpdb.mapper.IDbPatientMapper;
import com.easydicm.storescp.services.IDicomSave;
import com.easydicm.storescp.services.IMessageQueueWriter;
import com.easydicm.storescp.services.IStorageWriter;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.common.io.ByteSource;
import io.netty.buffer.ByteBufInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
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


    public DicomSaveImpl(@Autowired IDbPatientMapper dbPatientMapper, @Autowired IStorageWriter storageWriter, @Autowired IMessageQueueWriter messageQueueWriter) {

        super();
        this.dbPatientMapper = dbPatientMapper;
        this.storageWriter = storageWriter;
        this.messageQueueWriter = messageQueueWriter;
    }

    @Override
    public void dicomFilePersist(final PDVInputStream data, Attributes fileMetaInfomation,
                                 File storageDir,
                                 String clientId,
                                 String applicationId, Attributes rsp) throws IOException, RemotingException, MQClientException, InterruptedException {

            ByteBuffer byteBuffer = ByteBuffer.wrap(data.readAllBytes());
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(byteBuffer.array());
                 DicomInputStream dis = new DicomInputStream(inputStream)){
                Attributes attribs = dis.readDataset(-1, Tag.PixelData);
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
                LOG.info("PatientId -{}, StudyUID-{}, SeriesUid={}, SopInstUID={}", patId, studyUid, serisUid, sopUid);
                Path dcmpath = Paths.get(storageDir.getAbsolutePath(), clientId, patId, studyUid, serisUid, sopUid + ".dcm");
                if (!dcmpath.getParent().toFile().exists()) {
                    if (!dcmpath.getParent().toFile().mkdirs()) {
                        rsp.setInt(Tag.Status, VR.US, Status.ProcessingFailure);
                        return;
                    }
                }
                try (DicomOutputStream dicomOutputStream = new DicomOutputStream(dcmpath.toFile())) {
                    dicomOutputStream.writeFileMetaInformation(fileMetaInfomation);
                    dicomOutputStream.write(byteBuffer.array());
                    dicomOutputStream.flush();
                }
                messageQueueWriter.write(clientId, applicationId, fileMetaInfomation.getString(Tag.TransferSyntaxUID), attribs);
                rsp.setInt(Tag.Status, VR.US, Status.Success);
            }
            finally {
                byteBuffer.clear();
            }


    }
}

