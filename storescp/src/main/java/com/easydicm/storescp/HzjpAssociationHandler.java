package com.easydicm.storescp;


import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.pdu.AAssociateAC;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.ExtendedNegotiation;
import org.dcm4che3.net.pdu.UserIdentityAC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


/**
 * @author daihanzhang
 */
public class HzjpAssociationHandler extends AssociationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HzjpAssociationHandler.class);
    //构造
    public HzjpAssociationHandler( ) {
        super();
    }

    @Override
    protected AAssociateAC makeAAssociateAC(Association as, AAssociateRQ rq, UserIdentityAC userIdentity) throws IOException {
        LOG.info( "=====makeAAssociateAC BEGIN=====" );
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
        LOG.info( String.format("remotePort:%d", hold.getPort()) );
        LOG.info( String.format("remoteIpAddress:%s", hold.getAddress().getHostAddress()) );

        return super.makeAAssociateAC(as, rq, userIdentity);
    }

    @Override
    protected void onClose(Association as) {
        LOG.warn( "========makeAAssociateAC CLOSED:=============" + as.getCallingAET() );
        super.onClose(as);
    }
}
