package com.easydicm.storescp.services;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class MemMapBufferWrapperTest {


    @BeforeEach
    void setUp() {
    }


    @Test
    void create(){
        long  mapSize = 2L *1024L *1024*1024 + 480L;

        MemMapBufferWrapper memMapBufferWrappe= null;
        try {
            memMapBufferWrappe = new MemMapBufferWrapper(Paths.get("./","1111.data"), mapSize);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        Assert.isTrue(memMapBufferWrappe.getBufferSize()==3, "BufferSize 计算错误!");

        Assert.isTrue(memMapBufferWrappe.getMapSize()==mapSize, "mapSize 计算错误!");

        memMapBufferWrappe.getFilePath().toFile().deleteOnExit();


    }

    @AfterEach
    void tearDown() {
    }
}