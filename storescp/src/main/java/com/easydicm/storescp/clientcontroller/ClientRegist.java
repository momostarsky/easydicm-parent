package com.easydicm.storescp.clientcontroller;

import com.easydicm.scputil.RSAUtil2048;
import com.easydicm.storescp.models.AppInfo;
import com.easydicm.storescp.models.RegInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "api")

public class ClientRegist {

    private final Logger logger = LogManager.getLogger(this.getClass());
    private final String regFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @PostMapping(value = "regist")
    @ResponseBody
    public AppInfo client(@RequestBody RegInfo regInfo) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(regInfo);
        logger.info("注册客户端!" + jsonString);

        String txt = String.format("%s-%s", regInfo.getClientId(), regInfo.getClientName());
        UUID clientUid = UUID.nameUUIDFromBytes(txt.getBytes(StandardCharsets.UTF_8));
        String cid = clientUid.toString();

        String saveDir = String.format("./rsakey/%s", regInfo.getClientId());
        File save2 = Paths.get(saveDir).toFile();
        if (!save2.exists()) {
            save2.mkdirs();
        }
        String pkname = String.format("%s.pubkey", cid);
        String prname = String.format("%s.prikey", cid);
        String user = String.format("%s.userpk", cid);
        Map<String, String> km = RSAUtil2048.genKeyPairString();

        Path pubpath = Paths.get(saveDir, pkname);
        Files.writeString(pubpath, km.get(RSAUtil2048.PUBLIC_KEY), StandardCharsets.UTF_8);

        Path pripath = Paths.get(saveDir, prname);
        Files.writeString(pripath, km.get(RSAUtil2048.PRIVATE_KEY), StandardCharsets.UTF_8);

        Path userPath = Paths.get(saveDir, user);
        Files.writeString(userPath, regInfo.getPubkey(), StandardCharsets.UTF_8);
        logger.info("注册客户端!" + regInfo.getClientId() + "==>" + cid + ":成功! " + regInfo.getClientName() );
        return new AppInfo(clientUid.toString(), km.get(RSAUtil2048.PUBLIC_KEY));

    }

    @RequestMapping("echo")
    public String echo() {

        LocalDateTime dt = LocalDateTime.now(); // 当前日期和时间
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(regFormat);
        return dtf.format(dt);
    }
}
