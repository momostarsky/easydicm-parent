package com.easydicm.storescp.services;

import java.io.IOException;

public interface StoreProcessor {

    void writeDicomInfo( final String cuid, final String iuid, final String tsuid, final byte[] arr) throws IOException;
    void saveDicomInfo(  ) throws IOException;

    void clear();


}
