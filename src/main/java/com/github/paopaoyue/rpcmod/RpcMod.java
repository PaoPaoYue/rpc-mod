package com.github.paopaoyue.rpcmod;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.MTSClassLoader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@SpireInitializer
public class RpcMod implements PostInitializeSubscriber {

    private static final Logger logger = LogManager.getLogger(RpcMod.class);

    public static final String MOD_ID = "Ypp rpc";

    public RpcMod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new RpcMod();
    }

    @Override
    public void receivePostInitialize() {
        try {
            MTSClassLoader classLoader = new MTSClassLoader(Loader.class.getResourceAsStream("/corepatches.jar"),
                    buildUrlArray(Loader.MODINFOS), RpcMod.class.getClassLoader());
            RpcApp.initializeClient(classLoader);
        } catch (IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
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
