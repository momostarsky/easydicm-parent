package com.easydicm.storescp;


import com.easydicm.storescp.services.IDicomSave;
import com.easydicm.storescp.services.StoreProcessor;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * @author dhz
 */
@Component

public class StoreScp extends BasicCStoreSCP {

    static final Logger LOG = LoggerFactory.getLogger(StoreScp.class);


    private IDicomSave dicomSave;


    public StoreScp(IDicomSave dicomSave) {
        super("*");

        this.dicomSave = dicomSave;


    }


    private void sleep(Association as, int[] delays) {
        int responseDelay = delays != null
                ? delays[(as.getNumberOfReceived(Dimse.C_STORE_RQ) - 1) % delays.length]
                : 0;
        if (responseDelay > 0) {
            try {
                Thread.sleep(responseDelay);
            } catch (InterruptedException ignore) {
            }
        }
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes rq, PDVInputStream data) throws IOException {
        as.addAssociationListener(new AssociationListener() {
            @Override
            public void onClose(Association association) {
                LOG.info("AssociationListener2 Closed :{} {}",association.getSerialNo() ,rq);
            }
        });
        super.onDimseRQ(as, pc, dimse, rq, data);
    }

    @Override
    public void onClose(Association as) {
        LOG.info("AssociationListener3 Closed :{}",as.getSerialNo());
        super.onClose(as);
    }

    @Override
    protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp) throws IOException {
        LOG.info("SN:{}",as.getSerialNo());

        byte[] arr = data.readAllBytes();
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        String tsuid = pc.getTransferSyntax();
        StoreProcessor sp = (StoreProcessor)as.getProperty(GlobalConstant.AssicationSessionId);
        sp.writeDicomInfo(cuid,iuid,tsuid,arr);
        rsp.setInt(Tag.Status, VR.US, Status.Success);
    }


}