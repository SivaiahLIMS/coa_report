package com.stability.coareport.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class TomcatWebServerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            connector.setProperty("maxParameterCount", "10000");
            connector.setProperty("maxPostSize", "52428800");
//            connector.setMaxSwallowSize(52428800);
            connector.setProperty("maxSwallowSize", "52428800");
        });
    }
}
