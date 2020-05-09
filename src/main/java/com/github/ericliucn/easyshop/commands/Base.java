package com.github.ericliucn.easyshop.commands;

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

public class Base implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        int index = args.<Integer>getOne("index").orElse(1);
        try {
            Inventory shop = new InventoryBuilder(index).inventory;
            if (src instanceof Player){
                ((Player)src).openInventory(shop);
            }
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }

        return CommandResult.success();
    }

    public static CommandSpec build(){
        return CommandSpec.builder()
                .executor(new Base())
                .arguments(
                        GenericArguments.optional(
                                GenericArguments.integer(Text.of("index"))
                        )
                )
                .build();
    }
}
