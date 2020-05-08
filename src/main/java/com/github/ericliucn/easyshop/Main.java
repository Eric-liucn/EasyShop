package com.github.ericliucn.easyshop;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.File;

@Plugin(
        id = "easyshop",
        name = "EasyShop",
        description = "一个简单的菜单商店",
        authors = {
                "EricLiu"
        }
)
public class Main {

    @Inject
    public Logger logger;

    @Inject
    public PluginContainer pluginContainer;

    @Inject
    @ConfigDir(sharedRoot = false)
    public File file;

    public static Main INSTANCE;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        INSTANCE = this;


    }
}
