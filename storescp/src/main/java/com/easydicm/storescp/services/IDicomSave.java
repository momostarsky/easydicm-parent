package com.easydicm.storescp.services;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.PDVInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * @author dhz
 */
public interface IDicomSave {


    /***
     * 保存文件到磁盘
     * @param storageDir
     * @param buffer
     * @param storeInfomation
     * @throws IOException
     * @throws RemotingException
     * @throws MQClientException
     * @throws InterruptedException
     */
    void dicomFilePersist(final File storageDir, final byte[] buffer, StoreInfomation storeInfomation);

}
