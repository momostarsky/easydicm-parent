package com.easydicm.storescp.services.impl;

import com.easydicm.storescp.services.IMessageQueueWriter;
import org.dcm4che3.data.Attributes;
import org.springframework.stereotype.Service;

/**
 * @author dhz
 */
@Service
public class MessageQueueWriterImpl extends BaseImpl implements IMessageQueueWriter {


    public MessageQueueWriterImpl() {
        super();
    }

    @Override
    public void write(String clientId, String applicationId, Attributes attributesWithoutPixelData) {


        LOG.info("MessageQueue Publish Message  :" + clientId + "->" + applicationId);

    }
}
