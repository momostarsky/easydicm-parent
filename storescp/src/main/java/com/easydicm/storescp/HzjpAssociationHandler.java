package com.easydicm.storescp;


import com.easydicm.scputil.RSAUtil2048;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.pdu.AAssociateAC;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.ExtendedNegotiation;
import org.dcm4che3.net.pdu.UserIdentityAC;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.interfaces.RSAPublicKey;
import java.util.*;


/**
 * @author daihanzhang
 */
public class HzjpAssociationHandler extends AssociationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HzjpAssociationHandler.class);

    //构造
    public HzjpAssociationHandler() {
        super();
    }

    @Override
    protected AAssociateAC makeAAssociateAC(Association as, AAssociateRQ rq, UserIdentityAC userIdentity) throws IOException {
        LOG.info("=====makeAAssociateAC BEGIN=====");
        //LOG.info("=====makeAAssociateAC BEGIN=====");
        LOG.info(as.getCalledAET());
        LOG.info(as.getCallingAET());
        Socket socket = as.getSocket();
        //1. 通过建立一个SocketAddress对象，可以在多次连接同一个服务器时使用这个SocketAddress对象。
        //2. 在Socket类中提供了两个方法：getRemoteSocketAddress和getLocalSocketAddress，
        // 通过这两个方法可以得到服务器和本机的网络地址。而且所得到的网络地址在相应的Socket对象关闭后任然可以使用。
        SocketAddress sd = socket.getRemoteSocketAddress();
        //InetSocketAddress实现 IP 套接字地址（IP 地址 + 端口号）
        InetSocketAddress hold = (InetSocketAddress) sd;
        LOG.info(String.format("remotePort:%d", hold.getPort()));
        LOG.info(String.format("remoteIpAddress:%s", hold.getAddress().getHostAddress()));

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

        LOG.info("clientId" + "=" + clientId);
        LOG.info("applicationId" + "=" + applicationId);
        LOG.info("applicationIdEncrtyped" + "=" + applicationIdEncrtyped);
        LOG.info("applicationIdSignData" + "=" + applicationIdSignData);


        Path pubkey = Paths.get("./rsakey", clientId, applicationId + ".prikey");
        String keyContent = Files.readString(pubkey, StandardCharsets.UTF_8);
        String appid = null;
        try {

            appid = new String(RSAUtil2048.decryptByPrivateKey(applicationIdEncrtyped, keyContent), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.info("applicationIdDecrypted " + "=" + appid);
        if (!appid.equals(applicationId)) {
            throw new IOException("applicationId is not Exits !");
        }
        byte[] appData = applicationId.getBytes(StandardCharsets.UTF_8);
        Path userPk = Paths.get("./rsakey", clientId, applicationId + ".userpk");
        String clientContent = Files.readString(userPk, StandardCharsets.UTF_8);
        boolean ok;
        try {
            ok = RSAUtil2048.verify(appData, clientContent, applicationIdSignData);
        } catch (Exception e) {
            throw new IOException("data is not signed!");
        }
        if (!ok) {
            throw new IOException("data is uncorrect!");
        }

        return super.makeAAssociateAC(as, rq, userIdentity);


    }

    @Override
    protected void onClose(Association as) {
        LOG.warn("========makeAAssociateAC CLOSED:=============" + as.getCallingAET());
        super.onClose(as);
    }
}
