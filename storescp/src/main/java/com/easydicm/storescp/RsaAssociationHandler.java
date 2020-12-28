package com.easydicm.storescp;


import com.easydicm.scputil.RSAUtil2048;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.pdu.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


/**
 * @author daihanzhang
 */
public class RsaAssociationHandler extends AssociationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RsaAssociationHandler.class);

    /***
     * RSA 验证
     */
    public RsaAssociationHandler() {
        super();
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
        LOG.info(String.format("remotePort:%d", hold.getPort()));
        LOG.info(String.format("remoteIpAddress:%s", remoteIdp));
        rsaCheck(remoteIdp, as, rq);

        return super.makeAAssociateAC(as, rq, userIdentity);
    }

    @Override
    protected void onClose(Association as) {
        LOG.warn("========makeAAssociateAC CLOSED:=============" + as.getCallingAET());
        super.onClose(as);
    }
}
