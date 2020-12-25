package com.easydicm.storescp;


import com.easydicm.storescp.services.IDicomSave;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.util.AttributesFormat;
import org.dcm4che3.util.SafeClose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * @author dhz
 */
@Component

public class StoreScp extends BasicCStoreSCP {

    static final Logger LOG = LoggerFactory.getLogger(StoreScp.class);
    private static final String PART_EXT = ".part";


    private IDicomSave IDicomSave;

    public StoreScp(IDicomSave IDicomSave) {
        super("*");

        this.IDicomSave = IDicomSave;

    }

    private AttributesFormat filePathFormat;

    private int[] receiveDelays;
    private int[] responseDelays;
    private File storageDir;

    private void sleep(Association as, int[] delays) {
        int responseDelay = delays != null
                ? delays[(as.getNumberOfReceived(Dimse.C_STORE_RQ) - 1) % delays.length]
                : 0;
        if (responseDelay > 0)
            try {
                Thread.sleep(responseDelay);
            } catch (InterruptedException ignore) {
            }
    }

    private void storeTo(Association as, Attributes fmi,
                         PDVInputStream data, File file) throws IOException {
        LOG.info("{}: M-WRITE {}", as, file);
        file.getParentFile().mkdirs();
        DicomOutputStream out = new DicomOutputStream(file);
        try {
            out.writeFileMetaInformation(fmi);
            data.copyTo(out);
        } finally {
            SafeClose.close(out);
        }
    }

    private static void renameTo(Association as, File from, File dest)
            throws IOException {
        LOG.info("{}: M-RENAME {} to {}", as, from, dest);
        if (!dest.getParentFile().mkdirs())
            dest.delete();
        if (!from.renameTo(dest))
            throw new IOException("Failed to rename " + from + " to " + dest);
    }

    private static Attributes parse(File file) throws IOException {
        DicomInputStream in = new DicomInputStream(file);
        try {
            in.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            return in.readDataset(-1, Tag.PixelData);
        } finally {
            SafeClose.close(in);
        }
    }

    private static void deleteFile(Association as, File file) {
        if (file.delete())
            LOG.info("{}: M-DELETE {}", as, file);
        else
            LOG.warn("{}: M-DELETE {} failed!", as, file);
    }

    public void setStorageDirectory(File storageDir) {
        if (storageDir != null)
            storageDir.mkdirs();
        this.storageDir = storageDir;
    }

    public void setStorageFilePathFormat(String pattern) {
        this.filePathFormat = new AttributesFormat(pattern);
    }


    public void setReceiveDelays(int[] receiveDelays) {
        this.receiveDelays = receiveDelays;
    }

    public void setResponseDelays(int[] responseDelays) {
        this.responseDelays = responseDelays;
    }


    @Override
    protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp) throws IOException {

        sleep(as, receiveDelays);
        try {

            if (storageDir == null) {
                rsp.setInt(Tag.Status, VR.US, Status.ProcessingFailure);
                return;
            }

            String clientId = as.getProperty(GlobalConstant.AssicationClientId).toString();
            String appid = as.getProperty(GlobalConstant.AssicationApplicationId).toString();
            String cuid = rq.getString(Tag.AffectedSOPClassUID);
            String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
            String tsuid = pc.getTransferSyntax();

            File file = new File(storageDir, iuid + PART_EXT);
            String dir = storageDir.getAbsolutePath();
            Path savePath = Paths.get( dir ,clientId,  iuid + ".dcm");
            try {

                storeTo(as, as.createFileMetaInformation(iuid, cuid, tsuid),
                        data, file);


                if (!savePath.getParent().toFile().exists()) {
                    savePath.toFile().mkdirs();
                }

                renameTo(as, file, savePath.toFile());
                IDicomSave.dicomFilePersist(savePath.toFile(), cuid, iuid, clientId, appid);
                rsp.setInt(Tag.Status, VR.US, Status.Success);
            } catch (Exception e) {
                deleteFile(as, file);
                deleteFile(as, savePath.toFile());
                rsp.setInt(Tag.Status, VR.US, Status.ProcessingFailure);
                LOG.error(e.getMessage());
            }
        } finally {
            sleep(as, responseDelays);
        }
    }
}