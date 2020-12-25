package com.easydicm.storescp;

import com.easydicm.storescp.services.IDicomSave;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.*;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.dcm4che3.tool.common.CLIUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;



/**
 * @author dhz
 */
@Controller("DicmScp")
public class DicmScp {



    static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DicmScp.class);
    private final Device device = new Device("HzjpDicomDevice");
    private final ApplicationEntity ae = new ApplicationEntity("*");
    private final Connection conn = new Connection();

    private final AssociationHandler associationHandler = new RsaAssociationHandler();
    private final IDicomSave dicomSave;

    private int port = 11112;
    private String aeTitle = "EasySCP";
    private String host = "127.0.0.1";
    private boolean enableTls = false;
    private boolean ServerStarted = false;

    private File storageDir;







    private void configureTransferCapability() throws IOException {
        ae.addTransferCapability(new TransferCapability(null,
                UID.VerificationSOPClass,
                TransferCapability.Role.SCP,
                UID.ImplicitVRLittleEndian));
        {
            Properties storageSOPClasses = CLIUtils.loadProperties(
                    "resource:storage-sop-classes.properties",
                    null);
            addTransferCapabilities(storageSOPClasses, TransferCapability.Role.SCP, null);
            addTransferCapabilities(storageSOPClasses, TransferCapability.Role.SCU, null);
        }
        {
            Properties p = CLIUtils.loadProperties("resource:retrieve-sop-classes.properties", null);
            addTransferCapabilities(p, TransferCapability.Role.SCP, null);
        }
        {
            Properties p = CLIUtils.loadProperties("resource:query-sop-classes.properties", null);
            addTransferCapabilities(p, TransferCapability.Role.SCP, null);
        }
    }

    private void addTransferCapabilities(
            Properties p, TransferCapability.Role role,
            EnumSet<QueryOption> queryOptions) {
        for (String cuid : p.stringPropertyNames()) {
            String ts = p.getProperty(cuid);
            TransferCapability tc = new TransferCapability(null,
                    CLIUtils.toUID(cuid), role, CLIUtils.toUIDs(ts));
            tc.setQueryOptions(queryOptions);
            ae.addTransferCapability(tc);
        }
    }


    protected DicomServiceRegistry createServiceRegistry() {
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new BasicCEchoSCP());
        StoreScp scp = new StoreScp(this.dicomSave);
        scp.setStorageDirectory(storageDir);
        serviceRegistry.addDicomService(scp);

        StorageCommitmentScp stgCmt = new StorageCommitmentScp(this.device);
        serviceRegistry.addDicomService(stgCmt);


        return serviceRegistry;
    }

    private final ApplicationArguments ctx;

    /*
    usage  // --ae=DicmQRSCP  --host=192.168.1.92  --port=11112
     */


    public DicmScp(@Autowired ApplicationArguments ctx,
                   @Autowired  IDicomSave dicomSave

                   ) {

        this.ctx = ctx;
        this.dicomSave = dicomSave;


    }

    final String OPT_PORT ="port";
    final String OPT_AE ="ae";
    final String OPT_HOST ="host";
    final String OPT_STORAGEDIR ="storagedir";

    @PostConstruct
    public void start() {
        LOG.info("DicmSCP    start ....");
        try {

            Properties dcmcfg = CLIUtils.loadProperties(
                    "resource:scpsettings.properties",
                    null);
            aeTitle = dcmcfg.getProperty(OPT_AE);
            port = Integer.parseInt(dcmcfg.getProperty(OPT_PORT));
            host = dcmcfg.getProperty(OPT_HOST);
            storageDir = new File(dcmcfg.getProperty(OPT_STORAGEDIR));

            if (ctx != null) {
                LOG.info(ctx.toString());

                if (ctx.containsOption(OPT_PORT)) {

                    port = Integer.parseInt(ctx.getOptionValues(OPT_PORT).get(0));
                }
                if (ctx.containsOption(OPT_AE)) {

                    aeTitle = ctx.getOptionValues(OPT_AE).get(0);
                }
                if (ctx.containsOption(OPT_HOST)) {

                    host = ctx.getOptionValues(OPT_HOST).get(0);
                }
                if (ctx.containsOption(OPT_STORAGEDIR)) {

                    storageDir = new File(ctx.getOptionValues(OPT_STORAGEDIR).get(0));
                }
            }

            LOG.info("DicmSCP Setting is  {" + aeTitle + "," + host + "," + port + "}");


            conn.setReceivePDULength(Connection.DEF_MAX_PDU_LENGTH);
            conn.setSendPDULength(Connection.DEF_MAX_PDU_LENGTH);
            conn.setMaxOpsInvoked(0);
            conn.setMaxOpsPerformed(0);
            conn.setHostname(host);
            conn.setPort(port);

            ae.setAETitle(aeTitle);
            ae.setAssociationAcceptor(true);
            ae.addConnection(conn);
            configureTransferCapability();

            device.addConnection(conn);
            device.addApplicationEntity(ae);
            device.setDimseRQHandler(createServiceRegistry());
            device.setAssociationHandler(associationHandler);
            ExecutorService executorService = Executors.newCachedThreadPool();
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            device.setScheduledExecutor(scheduledExecutorService);
            device.setExecutor(executorService);
            device.bindConnections();
            ServerStarted = true;
            LOG.info("DicmSCP    start success !");

        } catch (Exception ex) {

            LOG.error("DicmSCP    Start Failed !", ex);
        }

    }


    /**
     * 移除所有连接
     * PreDestroy 在销毁前执行
     */
    @PreDestroy
    public void clear() {
        LOG.info("DicmSCP    remove connections ....");
        List<Connection> connections = ae.getConnections();
        if (connections.size() > 0) {

            for (Connection cn : connections) {
                if (cn != null) {
                    device.removeConnection(cn);
                }

            }
        }
        device.removeApplicationEntity(ae);
        LOG.info("DicmSCP    remove connections end !");
    }


}
