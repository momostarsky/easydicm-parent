package com.easydicm.storescp.services;

import org.dcm4che3.data.Attributes;

public class StoreInfomation {
    private final String appId;
    private final String clientId;
    private final String sessionId;
    private final Attributes fileMetaInfomation;

    public StoreInfomation(String appId, String clientId, String sessionId, Attributes fileMetaInfomation) {

        this.appId = appId;
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.fileMetaInfomation = fileMetaInfomation;
    }

    public Attributes getFileMetaInfomation() {
        return fileMetaInfomation;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAppId() {
        return appId;
    }

    public String getSessionId() {
        return sessionId;
    }
}