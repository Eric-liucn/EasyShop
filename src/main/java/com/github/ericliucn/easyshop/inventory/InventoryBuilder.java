package com.github.ericliucn.easyshop.inventory;

import com.github.ericliucn.easyshop.Main;
import com.github.ericliucn.easyshop.config.Config;
import com.github.ericliucn.easyshop.utils.Utils;
import com.google.common.reflect.TypeToken;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.math.BigDecimal;
import java.util.*;

public class InventoryBuilder {

    public Inventory inventory;
    private final List<String> itemStr;
    public List<ItemStack> itemStacks = new ArrayList<>();
    public Map<ItemStack, Double> itemStackAndPrice = new HashMap<>();
    public Map<ItemStack, String> itemStackAndCurrency = new HashMap<>();
    private final int invIndex;

    public InventoryBuilder(int index) throws ObjectMappingException {
        this.invIndex = index;
        this.itemStr = Config.rootNode
                .getNode("Shops", String.valueOf(index), "Items")
                .getList(TypeToken.of(String.class));
        this.loadItemStacks();
        this.loadInventory();
    }

    private void loadItemStacks(){
        if (itemStr.size()!=0){
            for (String string:itemStr){
                String[] strings = string.split(",");
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(strings[0]));
                if (item!=null){
                    net.minecraft.item.ItemStack nativeItemStack
                            = new net.minecraft.item.ItemStack(item,1, Integer.parseInt(strings[1]));
                    itemStacks.add(ItemStackUtil.fromNative(nativeItemStack));
                    itemStackAndPrice.put(ItemStackUtil.fromNative(nativeItemStack),Double.parseDouble(strings[2]));
                    itemStackAndCurrency.put(ItemStackUtil.fromNative(nativeItemStack),strings[3]);
                }
            }
        }
    }

    private void loadInventory(){
        this.inventory = Inventory.builder()
                .property(InventoryDimension.PROPERTY_NAME,InventoryDimension.of(9,6))
                .property(InventoryTitle.PROPERTY_NAME,InventoryTitle.of(Utils.strFormat(
                        Config.rootNode.getNode("Shops",String.valueOf(invIndex),"Name").getString()
                )))
                .listener(ClickInventoryEvent.class,event->{
                    event.setCancelled(true);
                    try {
                        tryTransaction(
                                event.getTransactions().get(0).getSlot().peek().orElse(ItemStack.empty()),
                                (Player) event.getSource()
                        );
                    }catch (Exception e){
                        Main.INSTANCE.logger.error(((Player)event.getSource()).getName()+"尝试交易失败！");
                    }
                })
                .build(Main.INSTANCE);
    }

    private boolean tryTransaction(ItemStack itemStack, Player player){
        if (!itemStackAndCurrency.containsKey(itemStack) || !itemStackAndPrice.containsKey(itemStack)){
            return false;
        }else {
            Optional<EconomyService> optionalEconomyService = Sponge.getServiceManager().provide(EconomyService.class);
            if (!optionalEconomyService.isPresent()) {
                return false;
            }

            EconomyService economyService = optionalEconomyService.get();
            Currency currency = getCurrencyByName(economyService, itemStackAndCurrency.get(itemStack));
            if (currency==null){
                Main.INSTANCE.logger.error("未找到对应货币");
                return false;
            }

            Double price = itemStackAndPrice.get(itemStack);

            economyService.getOrCreateAccount(player.getUniqueId()).ifPresent(uniqueAccount -> {
                BigDecimal balanceBig = uniqueAccount.getBalance(currency);
                double balance = balanceBig.doubleValue();
                if ((balance - price) >= 0){
                    if (price>0){
                        TransactionResult result = uniqueAccount.withdraw(currency,BigDecimal.valueOf(price),
                                Cause.builder()
                                        .build(EventContext.builder().
                                                add(EventContextKeys.PLUGIN,Main.INSTANCE.pluginContainer)
                                                .build()));
                        if (result.getResult().equals(ResultType.SUCCESS)){
                            player.sendMessage(Utils.strFormat("&a交易成功！"));
                        }else {
                            player.sendMessage(Utils.strFormat("&4交易失败！"));
                        }
                    }else {
                        TransactionResult result = uniqueAccount.deposit(currency,BigDecimal.valueOf(price),
                                Cause.builder()
                                        .build(EventContext.builder().
                                                add(EventContextKeys.PLUGIN,Main.INSTANCE.pluginContainer)
                                                .build()));
                        if (result.getResult().equals(ResultType.SUCCESS)){
                            player.sendMessage(Utils.strFormat("&a交易成功！"));
                        }else {
                            player.sendMessage(Utils.strFormat("&4交易失败！"));
                        }
                    }
                }else {
                    player.sendMessage(Utils.strFormat("&4你的账户余额不足或为负值！"));
                }
            });
            return true;
        }
    }

    private Currency getCurrencyByName(EconomyService economyService, String name){
        for (Currency currency:economyService.getCurrencies()){
            if (currency.getDisplayName().toPlain().equals(name)){
                return currency;
            }
        }
        return null;
    }

}
