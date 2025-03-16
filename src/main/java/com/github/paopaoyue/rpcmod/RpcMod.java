package com.github.paopaoyue.rpcmod;

import basemod.BaseMod;
import basemod.ModLabel;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.util.Arrays;

@SpireInitializer
public class RpcMod implements PostInitializeSubscriber {

    public static final Logger logger = LogManager.getLogger(RpcMod.class);

    public static final String MOD_ID = "ypp-rpc";

    private static SpireConfig config = null;

    public enum ConfigField {
        PROXY_ENABLED("PROXY_ENABLED");

        final String id;

        ConfigField(String val) {
            this.id = val;
        }
    }

    public RpcMod() {
        BaseMod.subscribe(this);
        try {
            config = new SpireConfig(MOD_ID, "Common");
            if (!config.has(ConfigField.PROXY_ENABLED.id)) {
                config.setBool(ConfigField.PROXY_ENABLED.id, false);
            }
        } catch (IOException e) {
            logger.error("Failed to load config", e);
        }
    }

    public static void initialize() {
        new RpcMod();
    }

    public static void sideload() {
        new RpcMod();
    }

    @Override
    public void receivePostInitialize() {
        if (isProxyEnabled()) {
            RpcApp.initializeFallbackClient();
        } else {
            RpcApp.initializeClient();
        }

        if (RpcApp.getEnv().equals("prod")) {
            Texture badgeTexture = ImageMaster.loadImage("image/icon/rpc_mod_badge.png");
            Gson gson = new Gson();
            ModInfo info = Arrays.stream(Loader.MODINFOS).filter(modInfo -> modInfo.ID.endsWith(MOD_ID)).findFirst().orElse(null);
            if (info == null) {
                logger.info("ModInfo not found for " + MOD_ID);
                return;
            }
            ModPanel settingsPanel = new ModPanel();
            settingsPanel.addUIElement(new ModLabel("Connectivity", 400.0f, 700.0f, settingsPanel, (me) -> {}));
            settingsPanel.addUIElement(new ModLabeledToggleButton("Use Proxy (ONLY Turn this on if experiencing connection issues)",
                    400f, 650f, Settings.CREAM_COLOR, FontHelper.charDescFont,
                    isProxyEnabled(), settingsPanel, (label) -> {
            }, (button) -> {
                config.setBool(ConfigField.PROXY_ENABLED.id, button.enabled);
                try {
                    config.save();
                    if (button.enabled) {
                        RpcApp.initializeFallbackClient();
                    } else {
                        RpcApp.initializeClient();
                    }
                } catch (IOException e) {
                    logger.error("Config save failed:", e);
                }
            }));
            BaseMod.registerModBadge(badgeTexture, info.Name, Strings.join(Arrays.asList(info.Authors), ','), info.Description, settingsPanel);
        }
    }

    static boolean isProxyEnabled() {
        if (config == null) {
            return false;
        }
        return config.getBool(ConfigField.PROXY_ENABLED.id);
    }

}
