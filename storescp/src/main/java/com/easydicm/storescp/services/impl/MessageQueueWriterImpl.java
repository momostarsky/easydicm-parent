package com.easydicm.storescp.services.impl;

import com.easydicm.storescp.services.IMessageQueueWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author dhz
 */

@Service
@Slf4j
public class MessageQueueWriterImpl extends BaseImpl implements IMessageQueueWriter {


    Properties mqAddressProeprs = new Properties();
    DefaultMQProducer producer;

    String topic = "DicmQRSCP";
    File topicSave;

    public MessageQueueWriterImpl() {
        super();

    }

    @PostConstruct
    public void Startup() throws MQClientException, IOException {

        mqAddressProeprs = PropertiesLoaderUtils.loadAllProperties("rocketmq.properties");
        String producerGroup = mqAddressProeprs.getProperty("productGroup");
        String nameServerAddr = mqAddressProeprs.getProperty("nameServerAddress");
        topic = mqAddressProeprs.getProperty("topic");
        producer = new DefaultMQProducer(producerGroup);
        // 设置NameServer的地址
        producer.setNamesrvAddr(nameServerAddr);
        // 启动Producer实例
        producer.start();
        producer.setRetryTimesWhenSendAsyncFailed(0);
        topicSave = new File(String.format("./%s",topic));
        topicSave.mkdirs();
        log.info("Start MQProduct Success:{}", producerGroup, nameServerAddr);
    }

    @PreDestroy
    public void Stop() {
        if (producer != null) {
            producer.shutdown();
        }
        log.info("Start MQProduct Stop");
    }


    @Override
    public void write(String clientId, String applicationId, String transferSyntax, Attributes attributesWithoutPixelData) {


        LOG.info("MessageQueue Publish Message  :" + clientId + "->" + applicationId);

        String patientId = attributesWithoutPixelData.getString(Tag.PatientID);
        String sopInstUid = attributesWithoutPixelData.getString(Tag.SOPInstanceUID);
        String msgKey = String.format("%s-%s", clientId, patientId);
        byte[] data = sopInstUid.getBytes(StandardCharsets.UTF_8);
        File msgFile = new File(String.format("./%s/%s.msg", topic, sopInstUid));
        try {
            ArrayList<String> arr = new ArrayList<>(5);
            arr.add(clientId);
            arr.add(applicationId);
            arr.add(patientId);
            arr.add(sopInstUid);
            arr.add(transferSyntax);
            String msgContent = String.join("|", arr);
            FileUtils.write(msgFile, msgContent, StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            LOG.error("{}", ioException.getMessage());
        }
        Message message = new Message(topic, transferSyntax, msgKey, data);
        message.setWaitStoreMsgOK(true);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            producer.send(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("{}- SendMessage OK", sopInstUid);
                    msgFile.delete();
                    countDownLatch.countDown();
                }

                @Override
                public void onException(Throwable e) {
                    log.info("{}- SendMessage Error ", e.getMessage());
                    countDownLatch.countDown();
                }
            });

        } catch (MQClientException e) {
            log.info(" 消息发送失败,客户端错误:{}",e.getMessage());
            countDownLatch.countDown();
        } catch (RemotingException e) {
            log.info(" 消息发送失败,远程访问错误:{}",e.getMessage());
            countDownLatch.countDown();
        } catch (InterruptedException e) {
            log.info(" 消息发送失败,调用中断:{}",e.getMessage());
            countDownLatch.countDown();
        }
        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info(" 消息发送失败,超时:{}",e.getMessage());
        }


    }
}
