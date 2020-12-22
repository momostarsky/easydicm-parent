package com.easydicm.storescp;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.easydicm.storescp")

public class StorescpApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(StorescpApplication.class, args);
    }



        @Override
        public void run(ApplicationArguments args) throws Exception {
            args.getOptionNames().forEach(optionName -> {
                System.out.println(optionName + "=" + args.getOptionValues(optionName));
            });
        }


}
