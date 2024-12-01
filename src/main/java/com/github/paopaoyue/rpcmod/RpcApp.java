package com.github.paopaoyue.rpcmod;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.lwjgl.Sys;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

@SpringBootApplication(
        exclude = GsonAutoConfiguration.class
)
@ComponentScan(basePackages = "com.github")
public class RpcApp{

    private static final String DEV_PROPERTIES = "application.properties";
    private static final String TEST_PROPERTIES = "application-test.properties";
    private static final String PROD_PROPERTIES = "application-prod.properties";

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
            throw new IllegalArgumentException("Properties file not found for env: " + env);
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
                .filter(modInfo -> modInfo.ID.endsWith(RpcMod.MOD_ID))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Ypp rpc mod not loaded"));
        if (info.Name.endsWith("Dev")) {
            return "dev";
        } else if (info.Name.endsWith("Test")) {
            return "test";
        } else {
            return "prod";
        }
    }

    private static Properties loadProperties(String propertiesFile) {
        try {
            InputStream inputStream;

            if (isURL(propertiesFile)) {
                try {
                    URL url = new URL(propertiesFile);
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        inputStream = new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    RpcMod.logger.error("Failed to load properties file from URL: {} ,using fallback", propertiesFile, e);
                    return null;
                }
            } else {
                inputStream = RpcApp.class.getClassLoader().getResourceAsStream(propertiesFile);
            }

            Properties properties = new Properties();
            properties.load(inputStream);

            RpcMod.logger.info("Loaded properties file from: {}", propertiesFile);

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
