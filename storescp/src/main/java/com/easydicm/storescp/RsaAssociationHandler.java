package com.easydicm.storescp;


import com.easydicm.scputil.RSAUtil2048;
import com.easydicm.storescp.services.SessionFactory;
import com.easydicm.storescp.services.impl.SesssionFactoryImpl;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.pdu.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


/**
 * @author daihanzhang
 */
public class RsaAssociationHandler extends AssociationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RsaAssociationHandler.class);
    private static final SessionFactory sessionFactory = new SesssionFactoryImpl();

    private boolean withRsaCheck;
    private File storageDir;

    private final ExecutorService executorPools;
    private final ThreadFactory namedThreadFactory;

    /***
     * MappedByteBuffer 最大容量是2G
     * 可以接受批量发送的最大容量
     */
    private static final int MMAPSIZE = Integer.MAX_VALUE - 1024;


    /***
     * RSA 验证
     */
    public RsaAssociationHandler() {
        super();


        int corePoolSize = Runtime.getRuntime().availableProcessors();

        namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("AsScp-pool-%d").build();

        executorPools = Executors.newCachedThreadPool(namedThreadFactory);

//        executorPools = new ThreadPoolExecutor(
//                2 * corePoolSize,
//                2000,
//                KEEPALIVETIME,
//                TimeUnit.SECONDS,
//                new LinkedBlockingQueue<>(),
//                namedThreadFactory,
//                new ThreadPoolExecutor.CallerRunsPolicy()
//        );


    }

    public void setWithRsa(boolean rsa) {
        withRsaCheck = rsa;

    }


    public void setStorageDir(File storageDir) {
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        this.storageDir = storageDir;

    }

    public void setTempDir(File tmpDir) throws IOException {


        sessionFactory.setTemplateDirectory(tmpDir);

    }

    protected void rsaCheck(String remoteIdp, Association as, AAssociateRQ rq) throws AAssociateRJ {
        Collection<ExtendedNegotiation> extMsg = rq.getExtendedNegotiations();

        String clientId = "";
        String applicationId = "";
        String applicationIdEncrtyped = "";
        String applicationIdSignData = "";
        for (ExtendedNegotiation ext : extMsg
        ) {
            String sopClassUid = ext.getSOPClassUID();
            if (sopClassUid.equals(UID.dicomHostname)) {
                clientId = new String(ext.getInformation(), StandardCharsets.UTF_8);
            }
            if (sopClassUid.equals(UID.dicomDescription)) {
                applicationId = new String(ext.getInformation(), StandardCharsets.UTF_8);
            }
            if (sopClassUid.equals(UID.dicomDevice)) {
                applicationIdEncrtyped = Base64.getEncoder().encodeToString(ext.getInformation());
            }

            if (sopClassUid.equals(UID.dicomDeviceName)) {
                applicationIdSignData = Base64.getEncoder().encodeToString(ext.getInformation());
            }
        }
        if (!StringUtils.hasText(clientId)
                && !StringUtils.hasText(applicationId)
                && !StringUtils.hasText(applicationIdEncrtyped)
                && !StringUtils.hasText(applicationIdSignData)) {
            LOG.warn(String.format("ExtendedNegotiations is Empty :%s", remoteIdp));
            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_PRES, AAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED);

        }

        Path pubkey = Paths.get("./rsakey", clientId, applicationId + ".prikey");
        if (!pubkey.toFile().exists()) {
            LOG.warn(String.format("PrivateKey is not exits :%s - %s:%s", remoteIdp, clientId, applicationId));
            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_PRES, AAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED);

        }


        String appid = null;
        try {
            String keyContent = Files.readString(pubkey, StandardCharsets.UTF_8);
            appid = new String(RSAUtil2048.decryptByPrivateKey(applicationIdEncrtyped, keyContent), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warn(String.format("decryptByPrivateKey Error   :%s - %s:%s", remoteIdp, clientId, applicationId));
            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_PRES, AAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED);
        }
        LOG.info("applicationIdDecrypted " + "=" + appid);
        if (!appid.equals(applicationId)) {
            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_PRES, AAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED);
        }
        byte[] appData = applicationId.getBytes(StandardCharsets.UTF_8);
        Path userPk = Paths.get("./rsakey", clientId, applicationId + ".userpk");
        if (!userPk.toFile().exists()) {
            LOG.warn(String.format("userPublicKey is Not Exists :%s - %s:%s", remoteIdp, clientId, applicationId));
            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_PRES, AAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED);
        }
        try {
            String clientContent = Files.readString(userPk, StandardCharsets.UTF_8);
            boolean ok = RSAUtil2048.verify(appData, clientContent, applicationIdSignData);
            if (!ok) {
                throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_PRES, AAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED);
            }
        } catch (Exception e) {
            LOG.warn(String.format("verify is Error : %s:%s", clientId, applicationId));
            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_PRES, AAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED);

        }

        as.setProperty(GlobalConstant.AssicationClientId, clientId);
        as.setProperty(GlobalConstant.AssicationApplicationId, applicationId);
        as.setProperty(GlobalConstant.AssicationSessionId, UUID.randomUUID().toString());

    }

    @Override
    protected AAssociateAC makeAAssociateAC(Association as, AAssociateRQ rq, UserIdentityAC userIdentity) throws IOException {
        LOG.info("=====makeAAssociateAC BEGIN=====" + as.getCalledAET() + ">>" + as.getCallingAET());
        Socket socket = as.getSocket();
        //1. 通过建立一个SocketAddress对象，可以在多次连接同一个服务器时使用这个SocketAddress对象。
        //2. 在Socket类中提供了两个方法：getRemoteSocketAddress和getLocalSocketAddress，
        // 通过这两个方法可以得到服务器和本机的网络地址。而且所得到的网络地址在相应的Socket对象关闭后任然可以使用。
        SocketAddress sd = socket.getRemoteSocketAddress();
        //InetSocketAddress实现 IP 套接字地址（IP 地址 + 端口号）
        InetSocketAddress hold = (InetSocketAddress) sd;
        String remoteIdp = hold.getAddress().getHostAddress();
        if (withRsaCheck) {
            LOG.info("开启RSA验证！");
            rsaCheck(remoteIdp, as, rq);
        } else {
            LOG.info("关闭RSA验证！");
        }
        String sessionUid = UUID.randomUUID().toString();
        as.setProperty(GlobalConstant.AssicationSessionId, sessionUid);
        boolean  regok = sessionFactory.SessionRegister(sessionUid);
        if(!regok){
            LOG.error("会话注册失败！{} {}", hold.getAddress(), hold.getPort());
        } else {
            LOG.error("会话注册成功！{} {}", hold.getAddress(), hold.getPort());
        }
        return super.makeAAssociateAC(as, rq, userIdentity);
    }


    public static void writeDicomInfo(final String sessionUid, final String cuid, final String iuid, final String tsuid, final byte[] arr) {
        sessionFactory.writeDicomInfo(sessionUid, cuid, iuid, tsuid, arr);
    }

    @Override
    protected void onClose(Association as) {
        //-- 此处不用启动新的线程， 多个线程上下文切换的速度更慢
        final String sessionId = as.getProperty(GlobalConstant.AssicationSessionId).toString();
        final File dicomFileSaveDir = this.storageDir;
        executorPools.submit(() -> sessionFactory.saveDicomInfo(sessionId, dicomFileSaveDir));
        as.clearProperty(GlobalConstant.AssicationSessionId);
        super.onClose(as);
    }
}
