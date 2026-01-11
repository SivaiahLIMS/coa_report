package com.stability.coareport.config;


import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
//import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

//    @Bean
//    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
//        return factory -> factory.addConnectorCustomizers((Connector connector) -> {
//            if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocolHandler) {
//                protocolHandler.setMaxSwallowSize(-1);
//                protocolHandler.setMaxParameterCount(10000);
//            }
//        });
//    }
}
