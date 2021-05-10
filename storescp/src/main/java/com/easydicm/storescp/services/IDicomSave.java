package com.easydicm.storescp.services;

import org.dcm4che3.data.Attributes;

import java.io.IOException;
import java.nio.file.Path;


/**
 * @author dhz
 */
public interface IDicomSave {

    Path computeSavePath(Attributes attributes ) throws IOException;
    void storagePath(String  storage);
}
