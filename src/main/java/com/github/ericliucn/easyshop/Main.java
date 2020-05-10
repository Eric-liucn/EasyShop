package com.github.ericliucn.easyshop;

import com.github.ericliucn.easyshop.commands.Base;
import com.github.ericliucn.easyshop.config.Config;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.File;
import java.io.IOException;

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
    public void onServerStart(GameStartedServerEvent event) throws IOException {
        INSTANCE = this;
        Config.init();
        Sponge.getCommandManager().register(this, Base.build(),"es","easyshop","eshop");
    }

    @Listener
    public void onReload(GameReloadEvent event) throws IOException {
        Config.load();
    }
}
