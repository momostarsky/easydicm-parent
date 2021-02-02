package com.easydicm.scputil;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class DcmFileScannerTest {

    @Test
    void scanFiles() {
        Collection<File>  x = DcmFileScanner.scanFiles("/home/dhz/dcmdata");
        Assert.isTrue(x.isEmpty() ==false, "查找文件失败");


    }
}