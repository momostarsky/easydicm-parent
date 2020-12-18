package com.easydicm.storescp;

import com.easydicm.storescp.models.RegInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

@SpringBootTest
class StorescpApplicationTests {

    @Test
    void contextLoads() throws JsonProcessingException {
        RegInfo  regInfo=new RegInfo();
        regInfo.setClientId("NC001");
        regInfo.setClientName("妇女之友联谊会");
        regInfo.setPubkey("LK:klasdfas");
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(regInfo);

        RegInfo r2 =  mapper.readValue(jsonString, RegInfo.class);
        Assert.isTrue(regInfo.getClientId().equals(r2.getClientId()), "clientId are Error!");
        Assert.isTrue(regInfo.getClientName().equals(r2.getClientName()), "clientName are Error!");
        Assert.isTrue(regInfo.getPubkey().equals(r2.getPubkey()), "pubkey are Error!");
    }

}

