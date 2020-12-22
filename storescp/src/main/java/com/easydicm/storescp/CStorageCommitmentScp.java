package com.easydicm.storescp;

import org.dcm4che3.data.*;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.AbstractDicomService;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.io.IOException;
import java.util.Map;

public class CStorageCommitmentScp extends AbstractDicomService {

    static final Logger LOG = LoggerFactory.getLogger(CStorageCommitmentScp.class);

    private final Device device;
    public CStorageCommitmentScp(Device device) {
        super(UID.StorageCommitmentPushModelSOPClass);
        this.device = device;
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                          Attributes rq, Attributes actionInfo) throws IOException {
        if (dimse != Dimse.N_ACTION_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);

        int actionTypeID = rq.getInt(Tag.ActionTypeID, 0);
        if (actionTypeID != 1)
            throw new DicomServiceException(Status.NoSuchActionType)
                    .setActionTypeID(actionTypeID);

        Attributes rsp = Commands.mkNActionRSP(rq, Status.Success);
        String callingAET = as.getCallingAET();
        String calledAET = as.getCalledAET();
        Connection remoteConnection = as.getConnection();
        if (remoteConnection == null)
            throw new DicomServiceException(Status.ProcessingFailure,  "Unknown Calling AET: " + callingAET);
        Attributes eventInfo =
                calculateStorageCommitmentResult(calledAET, actionInfo);
        try {
            as.writeDimseRSP(pc, rsp, null);
            device.execute(new SendStgCmtResult(as, eventInfo,
                    true, remoteConnection));
        } catch (AssociationStateException e) {
            LOG.warn("{} << N-ACTION-RSP failed: {}", as, e.getMessage());
        }
    }

    private static Attributes refSOP(String iuid, String cuid, int failureReason) {
        Attributes attrs = new Attributes(3);
        attrs.setString(Tag.ReferencedSOPClassUID, VR.UI, cuid);
        attrs.setString(Tag.ReferencedSOPInstanceUID, VR.UI, iuid);
        if (failureReason != Status.Success)
            attrs.setInt(Tag.FailureReason, VR.US, failureReason);
        return attrs ;
    }

    protected  boolean  sopItemExists(String sopInstUID, String sopClassUID  ){
        return true;
    }

    public Attributes calculateStorageCommitmentResult(String calledAET,
                                                       Attributes actionInfo) throws DicomServiceException {

         LOG.info("CallingAET:"+ calledAET);

        Sequence requestSeq = actionInfo.getSequence(Tag.ReferencedSOPSequence);
        int size = requestSeq.size();
        String[] sopIUIDs = new String[size];
        Attributes eventInfo = new Attributes(6);
        eventInfo.setString(Tag.RetrieveAETitle, VR.AE, calledAET);
        eventInfo.setString(Tag.TransactionUID, VR.UI, actionInfo.getString(Tag.TransactionUID));
        Sequence successSeq = eventInfo.newSequence(Tag.ReferencedSOPSequence, size);
        Sequence failedSeq = eventInfo.newSequence(Tag.FailedSOPSequence, size);
        for (int i = 0; i < sopIUIDs.length; i++) {
            Attributes item = requestSeq.get(i);
            String instUid =  item.getString(Tag.ReferencedSOPInstanceUID);
            String clsUid = item.getString(Tag.ReferencedSOPClassUID);
            LOG.info(  instUid +"=>" +  clsUid );
            if(sopItemExists(instUid, clsUid)){
                successSeq.add(refSOP(instUid, clsUid, Status.Success));
            } else {
                successSeq.add(refSOP(instUid, clsUid, Status.NoSuchObjectInstance));
            }
        }
        if (failedSeq.isEmpty())
            eventInfo.remove(Tag.FailedSOPSequence);
        return eventInfo;

    }
} 
