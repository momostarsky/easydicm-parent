package com.easydicm.storescp.services;

import org.dcm4che3.data.Attributes;

/**
 * 收图信息写入消息队列
 *
 * @author dhz
 */
public interface IMessageQueueWriter {
    /***
     * 切片基本信息写入消息队列
     * @param clientId     scuClientId
     * @param applicationId  scuApplication Id
     * @param attributesWithoutPixelData   Dicom Attributes Without PxielData
     */
    void write(String clientId, String applicationId, Attributes attributesWithoutPixelData);
}
