package com.stability.coareport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class CoaReportApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CoaReportApplication.class);
    }



    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileCountMax", "10000");
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileSizeMax", "52428800");
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileItemFactory.sizeThreshold", "0");
        SpringApplication.run(CoaReportApplication.class, args);
    }
}
