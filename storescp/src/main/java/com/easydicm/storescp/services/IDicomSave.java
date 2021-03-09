package com.easydicm.storescp.services;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.PDVInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;


/**
 * @author dhz
 */
public interface IDicomSave {

    Path computeSavePath(Attributes attributes );
    void storagePath(String  storage);
}
