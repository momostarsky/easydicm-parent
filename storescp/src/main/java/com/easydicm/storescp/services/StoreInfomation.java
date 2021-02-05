package com.easydicm.storescp.services;

import org.dcm4che3.data.Attributes;

public class StoreInfomation {

    private final Attributes fileMetaInfomation;
    private final int     dataLength;
    private final int     dataPositon;

    public StoreInfomation( Attributes fileMetaInfomation, int postion, int dataLength) {

        this.fileMetaInfomation = fileMetaInfomation;
        this.dataLength = dataLength;
        this.dataPositon = postion;
    }

    public Attributes getFileMetaInfomation() {
        return fileMetaInfomation;
    }



    public int getDataLength() {
        return dataLength;
    }

    public int getDataPositon() {
        return dataPositon;
    }
}