package com.easydicm.storescp.services;

import java.io.File;
import java.io.IOException;

public interface SessionFactory {
    boolean SessionRegister(String sessionUid);

    void writeDicomInfo(String sessionUid, final String cuid, final String iuid, final String tsuid, final byte[] arr);
    void saveDicomInfo(String sessionUid, File dicomFileSaveDir);

    void setTemplateDirectory(File  directory) throws IOException;
}
