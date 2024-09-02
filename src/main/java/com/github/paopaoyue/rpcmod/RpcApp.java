package com.github.paopaoyue.rpcmod;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

@SpringBootApplication(
        exclude = GsonAutoConfiguration.class
)
@ComponentScan(basePackages = "com.github")
public class RpcApp{

    private static final String INDICATOR = "application-dev.properties";
    private static final String DEV_PROPERTIES = "application-dev.properties";
    private static final String PROD_PROPERTIES = "application-prod.properties";

    static ConfigurableApplicationContext context;

    static void initializeClient(ClassLoader classLoader) {
        String env = new ClassPathResource(INDICATOR).exists() ? "prod" : "dev";
        Properties properties = loadProperties("dev".equals(env) ? DEV_PROPERTIES : PROD_PROPERTIES);
        if (properties == null) {
            throw new IllegalArgumentException("Properties file not found: " + DEV_PROPERTIES + " or " + PROD_PROPERTIES);
        }

        Thread.currentThread().setContextClassLoader(classLoader);
        context = new SpringApplicationBuilder()
                .sources(RpcApp.class)
                .resourceLoader(new DefaultResourceLoader(classLoader))
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .run();

    }

    private static Properties loadProperties(String propertiesFile) {
        try {
            InputStream inputStream;

            if (isURL(propertiesFile)) {
                URL url = new URL(propertiesFile);
                URLConnection connection = url.openConnection();
                inputStream = connection.getInputStream();
            } else {
                inputStream = RpcApp.class.getClassLoader().getResourceAsStream(propertiesFile);
            }

            if (inputStream == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(inputStream);

            for (String key : properties.stringPropertyNames()) {
                System.setProperty(key, properties.getProperty(key));
            }

            return properties;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties file: " + propertiesFile, e);
        }
    }

    private static boolean isURL(String location) {
        try {
            new URL(location);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
