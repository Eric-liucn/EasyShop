package com.github.ericliucn.easyshop.inventory;

import com.github.ericliucn.easyshop.Main;
import com.github.ericliucn.easyshop.config.Config;
import com.github.ericliucn.easyshop.utils.Utils;
import com.google.common.reflect.TypeToken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.property.SlotSide;
import org.spongepowered.api.item.inventory.query.QueryOperation;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.mixin.api.mcp.entity.player.EntityPlayerMPMixin_API;

import java.math.BigDecimal;
import java.util.*;

public class InventoryBuilder {

    public Inventory inventory;
    private final List<String> itemStr;
    public Map<Integer, ItemStack> itemStacks = new HashMap<>();
    public Map<Integer,Map<String,Double>> indexOfPriceAndCurrency = new HashMap<>();
    private final int invIndex;
    public EconomyService economyService = Sponge.getServiceManager().provideUnchecked(EconomyService.class);

    public InventoryBuilder(int index) throws ObjectMappingException {
        this.invIndex = index;
        this.itemStr = Config.rootNode
                .getNode("Shops", String.valueOf(index), "Items")
                .getList(TypeToken.of(String.class));
        this.loadInventory();
        this.loadItemStacks();
    }

    private void loadItemStacks(){
        if (itemStr.size()!=0){
            for (String string:itemStr){
                String[] strings = string.split(",");
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(strings[0]));
                if (item!=null){
                    net.minecraft.item.ItemStack nativeItemStack
                            = new net.minecraft.item.ItemStack(item,1, Integer.parseInt(strings[1]));
                    int index = this.addItemStackToInv(ItemStackUtil.fromNative(nativeItemStack));
                    if (index!=-1){
                        Map<String,Double> currencyAndPrice = new HashMap<>();
                        currencyAndPrice.put(strings[3],Double.parseDouble(strings[2]));
                        indexOfPriceAndCurrency.put(index,currencyAndPrice);
                    }else {
                        Main.INSTANCE.logger.error("添加物品到列表时出错，请检查配置文件");
                    }

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
                        if (event.getSlot().isPresent()){
                            System.out.println(itemStacks);
                            tryTransaction(event.getSlot().get(), ((Player) event.getSource()));
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        Main.INSTANCE.logger.error(((Player)event.getSource()).getName()+"尝试交易失败！");
                    }
                })
                .build(Main.INSTANCE);
    }

    private Integer addItemStackToInv(ItemStack itemStack){
        if (this.inventory.canFit(itemStack)){
            for (Inventory slot:this.inventory.slots()){
                if (!slot.peek().isPresent()){
                    slot.set(itemStack);
                    int index = slot.getInventoryProperty(SlotIndex.class).get().getValue();
                    itemStacks.put(index, itemStack);
                    return index;
                }
            }
        }
        return -1;
    }

    private void tryTransaction(Slot slot, Player player){
        slot.getInventoryProperty(SlotIndex.class).ifPresent(slotIndex -> {
            if (slotIndex.getValue()<54 && getCurrency(slotIndex.getValue())!=null){
                this.economyService.getOrCreateAccount(player.getUniqueId()).ifPresent(uniqueAccount -> {

                    if (!player.getInventory().canFit(itemStacks.get(slotIndex.getValue()))){
                        player.closeInventory();
                        player.sendMessage(Utils.strFormat("&4&l交易失败！你的背包空间不足"));
                    }

                    TransactionResult result = uniqueAccount.withdraw(getCurrency(slotIndex.getValue()),
                            BigDecimal.valueOf(getPrice(slotIndex.getValue())),
                            Cause.of(EventContext.builder().add(EventContextKeys.PLUGIN, Main.INSTANCE.pluginContainer).build(), Main.INSTANCE.pluginContainer));
                    if (result.getResult().equals(ResultType.SUCCESS)){
                        player.sendMessage(Utils.strFormat("&a交易成功"));
                        player.getInventory().offer(itemStacks.get(slotIndex.getValue()));
                    }else {
                        player.sendMessage(Utils.strFormat("&4交易失败！，请联系管理员"));
                    }

                });
            }
        });
    }

    private Currency getCurrencyByName(String name){
        for (Currency currency:economyService.getCurrencies()){
            if (currency.getDisplayName().toPlain().equalsIgnoreCase(name)){
                return currency;
            }
        }
        return null;
    }

    private Double getPrice(int index){

        for (Map.Entry<String, Double> entry:indexOfPriceAndCurrency.get(index).entrySet()
             ) {
            return entry.getValue();
        }
        return 10000000000D;
    }

    private Currency getCurrency(int index){
        for (Map.Entry<String, Double> entry:indexOfPriceAndCurrency.get(index).entrySet()
        ) {
            return getCurrencyByName(entry.getKey());
        }
        return null;
    }

}
