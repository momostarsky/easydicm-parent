package com.easydicm.storescp.services.impl;

import com.easydicm.storescp.services.IStorageWriter;
import org.dcm4che3.data.Attributes;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author dhz
 */

@Service
public class StorageWriterImpl extends BaseImpl implements IStorageWriter {

    public StorageWriterImpl() {
        super();
    }

    @Override
    public void write(String clientId, String applicationId, Attributes attributesWithoutPixelData, File dicomFile) {
        LOG.info("Save Dicom File To OSS  :" + clientId + "->" + applicationId);
    }
}
