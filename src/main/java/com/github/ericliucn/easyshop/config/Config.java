package com.github.ericliucn.easyshop.config;

import com.github.ericliucn.easyshop.Main;
import com.google.common.reflect.TypeToken;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    public static CommentedConfigurationNode rootNode;
    public static final File config = new File(Main.INSTANCE.file, "easyshop.conf");
    public static ConfigurationLoader<CommentedConfigurationNode> loader
            = HoconConfigurationLoader.builder().setFile(config).build();
    public static final Map<ItemStack, Map<String,Double>> items = new HashMap<>();


    public static void init() throws IOException {

        if (!Main.INSTANCE.file.exists()){
            Main.INSTANCE.file.mkdir();
        }

        if (!config.exists()){
            Main.INSTANCE.pluginContainer.getAsset("easyshop.conf").ifPresent(conf->{
                try {
                    conf.copyToDirectory(Main.INSTANCE.file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        load();

    }

    public static void load() throws IOException {
        rootNode = loader.load();
    }

    public static void save() throws IOException {
        loader.save(rootNode);
    }

    public static void loadItems() throws ObjectMappingException {
        items.clear();
        List<String> strings = rootNode.getNode("Items").getList(TypeToken.of(String.class));
        for (String string:strings
             ) {
            String[] one = string.split(",");
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(one[0]));
            assert item != null;
            net.minecraft.item.ItemStack itemStack = new net.minecraft.item.ItemStack(item,1,Integer.parseInt(one[1]));
            double price = Double.parseDouble(one[2]);
            String currency = one[3];
            Map<String, Double> priceAndDouble = new HashMap<>();
            priceAndDouble.put(currency,price);
            items.put(ItemStackUtil.fromNative(itemStack),priceAndDouble);
        }
    }

}
