package com.easydicm.storescp.services;

import org.dcm4che3.data.Attributes;

import java.io.File;

/**
 * 写入分布式存储系统
 * @author dhz
 */
public interface IStorageWriter {
    /***
     *
     * @param clientId
     * @param applicationId
     * @param attributesWithoutPixelData
     * @param dicomFile
     */
    void write(String clientId, String applicationId, Attributes attributesWithoutPixelData , File dicomFile);
}
