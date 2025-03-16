package com.github.paopaoyue.rpcmod;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.MTSClassLoader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@SpringBootApplication(
        exclude = GsonAutoConfiguration.class
)
@ComponentScan(basePackages = "com.github")
public class RpcApp{

    private static final String DEV_PROPERTIES = "application.properties";
    private static final String TEST_PROPERTIES = "application-test.properties";
    private static final String PROD_PROPERTIES = "application-prod.properties";
    private static final String FALLBACK_PROPERTIES = "application-prod-fallback.properties";

    static ConfigurableApplicationContext context;

    static void initializeClient() {
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
        try {
            initialize(properties);
        } catch (IOException | IllegalAccessException e) {
            RpcMod.logger.error("Failed to initialize client", e);
        }
    }

    static void initializeFallbackClient() {
        if (!getEnv().equals("prod")) {
            throw new IllegalArgumentException("Only prod env supports fallback client");
        }
        Properties properties = loadProperties(FALLBACK_PROPERTIES);
        if (properties == null) {
            throw new IllegalArgumentException("Properties file not found for fallback");
        }
        try {
            initialize(properties);
        } catch (IOException | IllegalAccessException e) {
            RpcMod.logger.error("Failed to initialize client", e);
        }
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

    private static void initialize(Properties properties) throws IOException, IllegalAccessException {
        if (context != null) {
            context.close();
        }
        MTSClassLoader classLoader = new MTSClassLoader(Loader.class.getResourceAsStream("/corepatches.jar"),
                buildUrlArray(Loader.MODINFOS), RpcMod.class.getClassLoader());
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

    private static URL[] buildUrlArray(ModInfo[] modInfos) {
        List<URL> urls = new ArrayList<>(modInfos.length + 1);

        for (ModInfo modInfo : modInfos) {
            urls.add(modInfo.jarURL);
        }

        return urls.toArray(new URL[0]);
    }

}
