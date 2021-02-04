package com.easydicm.storescp.services;

import org.dcm4che3.data.Attributes;

public class StoreInfomation {

    private final Attributes fileMetaInfomation;
    private final int     dataLength;

    public StoreInfomation( Attributes fileMetaInfomation,int dataLength) {

        this.fileMetaInfomation = fileMetaInfomation;
        this.dataLength = dataLength;
    }

    public Attributes getFileMetaInfomation() {
        return fileMetaInfomation;
    }



    public int getDataLength() {
        return dataLength;
    }
}