package com.easydicm.storescp.services;

public interface StoreProcessor {

    void writeDicomInfo( final String cuid, final String iuid, final String tsuid, final byte[] arr);
    void saveDicomInfo(  );


}
