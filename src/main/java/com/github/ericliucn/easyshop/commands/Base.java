package com.github.ericliucn.easyshop.commands;

import com.github.ericliucn.easyshop.config.Config;
import com.github.ericliucn.easyshop.inventory.InventoryBuilder;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Base implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        String shopName = args.<String>getOne("shop").get();
        try {
            Inventory shop = new InventoryBuilder(shopName).inventory;
            if (src instanceof Player){
                ((Player)src).openInventory(shop);
            }
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }

        return CommandResult.success();
    }

    public static CommandSpec build(){
        List<String> list = new ArrayList<>();
        for (Object object:Config.rootNode.getNode("Shops").getChildrenMap().keySet()
             ) {
            list.add(object.toString());
        }

        return CommandSpec.builder()
                .executor(new Base())
                .permission("easyshop.base")
                .arguments(
                        GenericArguments.withSuggestions(
                                GenericArguments.string(Text.of("shop")),
                                list
                        )
                )
                .build();
    }
}
