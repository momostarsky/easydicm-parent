package com.easydicm.storescp;

import com.easydicm.storescp.services.IDicomSave;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import java.util.concurrent.*;


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


    private volatile boolean serverStarted = false;

    private File storageDir;
    private Boolean withRsa;


    private void configureTransferCapability() throws IOException {
        ae.addTransferCapability(new TransferCapability(null,
                UID.VerificationSOPClass,
                TransferCapability.Role.SCP,
                UID.ImplicitVRLittleEndian));
        {
            Properties storageSopClasses = CLIUtils.loadProperties("resource:storage-sop-classes.properties", null);
            addTransferCapabilities(storageSopClasses, TransferCapability.Role.SCP, null);
            addTransferCapabilities(storageSopClasses, TransferCapability.Role.SCU, null);
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
        StoreScp scp = new StoreScp(this.dicomSave );
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
                   @Autowired IDicomSave dicomSave

    ) {

        this.ctx = ctx;
        this.dicomSave = dicomSave;

    }

    final String OPT_PORT = "port";
    final String OPT_AE = "ae";
    final String OPT_HOST = "host";
    final String OPT_STORAGEDIR = "storagedir";
    final String OPT_RSA= "rsa";



    @PostConstruct
    public void start() {
        LOG.info("DicmSCP    start ....");
        try {
            if (serverStarted) {
                LOG.info("DicmSCP    started!");
                return;
            }
            Properties dcmcfg = CLIUtils.loadProperties("resource:scpsettings.properties", null);
            String aeTitle = dcmcfg.getProperty(OPT_AE);
            int port = Integer.parseInt(dcmcfg.getProperty(OPT_PORT));
            String host = dcmcfg.getProperty(OPT_HOST);
            withRsa = Boolean.parseBoolean( dcmcfg.getProperty(OPT_RSA));
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
                if (ctx.containsOption(OPT_RSA)) {
                    withRsa = Boolean.parseBoolean( ctx.getOptionValues(OPT_RSA).get(0));
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

            RsaAssociationHandler  handler = (RsaAssociationHandler) associationHandler;
            handler.setWithRsa(withRsa);
            device.setAssociationHandler(handler);
            int corePoolSize = Runtime.getRuntime().availableProcessors();
            int maxPoolSize = corePoolSize * 10;
            long keepAliveTime = 60L;
            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("DicmQRSCP-pool-%d").build();
            ExecutorService executorPools = new ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    keepAliveTime,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(), namedThreadFactory,
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            device.setScheduledExecutor(scheduledExecutorService);
            device.setExecutor(executorPools);
            device.bindConnections();
            serverStarted = true;
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
        try {
            device.waitForNoOpenConnections();
        } catch (Exception ex) {
            LOG.info("Stop DicmQRSCP:" + ex.getMessage());
        }
        List<Connection> connections = device.listConnections();
        if (connections != null && connections.size() > 0) {
            for (Connection cn : connections) {
                if (cn != null) {
                    try {
                        device.removeConnection(cn);
                    } catch (Exception ex) {
                        LOG.info(ex.getMessage());
                    }
                }
            }
        }
        device.removeApplicationEntity(ae);
        LOG.info("DicmSCP    remove connections end !");
    }


}
