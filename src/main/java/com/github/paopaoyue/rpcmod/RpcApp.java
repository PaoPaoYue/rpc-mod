package com.github.paopaoyue.rpcmod;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
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
import java.util.Arrays;
import java.util.Properties;

@SpringBootApplication(
        exclude = GsonAutoConfiguration.class
)
@ComponentScan(basePackages = "com.github")
public class RpcApp{

    private static final String DEV_PROPERTIES = "dev/application-dev.properties";
    private static final String TEST_PROPERTIES = "dev/application-test.properties";
    private static final String PROD_PROPERTIES = "prod/application-prod.properties";

    static ConfigurableApplicationContext context;

    static void initializeClient(ClassLoader classLoader) {
        String env = getEnv();
        Properties properties;
        if ("dev".equals(env)) {
            properties = loadProperties(DEV_PROPERTIES);
        } else if ("test".equals(env)) {
            properties = loadProperties(TEST_PROPERTIES);
        } else {
            properties = loadProperties(PROD_PROPERTIES);
        }
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

    static String getEnv() {
        ModInfo info = Arrays.stream(Loader.MODINFOS)
                .filter(modInfo -> modInfo.ID.startsWith("Ypp rpc"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Ypp rpc mod not loaded"));
        if (info.ID.endsWith("dev")) {
            return "dev";
        } else if (info.ID.endsWith("test")) {
            return "test";
        } else {
            return "prod";
        }
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
