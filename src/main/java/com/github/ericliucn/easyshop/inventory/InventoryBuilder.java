package com.github.ericliucn.easyshop.inventory;

import com.github.ericliucn.easyshop.Main;
import com.github.ericliucn.easyshop.config.Config;
import com.github.ericliucn.easyshop.utils.Utils;
import com.google.common.reflect.TypeToken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.math.BigDecimal;
import java.util.*;

public class InventoryBuilder {

    public Inventory inventory;
    private final List<String> itemStr;
    protected final Map<Integer, ItemStack> itemStacks = new HashMap<>();
    protected final Map<Integer,Map<String,Double>> indexOfPriceAndCurrency = new HashMap<>();
    public EconomyService economyService = Sponge.getServiceManager().provideUnchecked(EconomyService.class);
    private String shopName;

    public InventoryBuilder(String shopName) throws ObjectMappingException {
        this.shopName = shopName;
        this.itemStr = Config.rootNode
                .getNode("Shops", shopName, "Items")
                .getList(TypeToken.of(String.class));
        this.loadInventory();
        this.loadItemStacks();
        this.addItemStackToInv();
    }

    private void loadItemStacks(){
        if (itemStr.size()!=0){
            int i = 0;
            for (String string:itemStr
                 ) {
                if (i<=54) {
                    String[] strings = string.split(",");
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(strings[0]));
                    net.minecraft.item.ItemStack itemStack = new net.minecraft.item.ItemStack(item, 1, Integer.parseInt(strings[1]));
                    ItemStack spongeItemStack = ItemStackUtil.fromNative(itemStack);
                    itemStacks.put(i, spongeItemStack);
                    Map<String, Double> currencyAndPrice = new HashMap<>();
                    currencyAndPrice.put(strings[3], Double.parseDouble(strings[2]));
                    indexOfPriceAndCurrency.put(i, currencyAndPrice);
                    i += 1;
                }
            }
        }
    }

    private void loadInventory(){
        //构建背包
        this.inventory = Inventory.builder()
                //背包格子数量
                .property(InventoryDimension.PROPERTY_NAME,InventoryDimension.of(9,6))
                //背包标题
                .property(InventoryTitle.PROPERTY_NAME,InventoryTitle.of(Utils.strFormat(
                        Config.rootNode.getNode("Shops",shopName,"Name").getString()
                )))
                //背包监听器
                .listener(ClickInventoryEvent.class,event->{
                    //首先取消点击事件
                    event.setCancelled(true);
                    try {
                        //尝试交易
                        if (event.getSlot().isPresent()){

                            tryTransaction(event.getSlot().get(), ((Player) event.getSource()));
                            //重载物品Map
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        Main.INSTANCE.logger.error(((Player)event.getSource()).getName()+"尝试交易失败！");
                    }
                })
                .build(Main.INSTANCE);
    }

    private void addItemStackToInv(){

        if (this.inventory!=null){
            for (Map.Entry<Integer,ItemStack> entry:itemStacks.entrySet()
                 ) {
                //对于物品Map里的每一个物品，先判断是否能放进去
                if (this.inventory.canFit(entry.getValue())) {
                    //可以
                    ItemStack itemStack = ItemStack.builder().fromSnapshot(entry.getValue().createSnapshot()).build();
                    //价格小于0
                    if (getPrice(entry.getKey()) <= 0){
                        //添加收购的标签
                        List<Text> list = new ArrayList<>();
                        list.add(Utils.strFormat("&a&l收购"));
                        list.add(Utils.strFormat("&6价格：&b"+Math.abs(getPrice(entry.getKey()))));
                        list.add(Utils.strFormat("&6货币：&b"+Config.rootNode.getNode(getCurrency(entry.getKey()).getDisplayName().toPlain().toLowerCase()).getString()));
                        itemStack.offer(Keys.ITEM_LORE,list);
                    }else {
                        //大于0，添加出售的标签
                        List<Text> list = new ArrayList<>();
                        list.add(Utils.strFormat("&a&l出售"));
                        list.add(Utils.strFormat("&6价格：&b"+Math.abs(getPrice(entry.getKey()))));
                        list.add(Utils.strFormat("&6货币：&b"+Config.rootNode.getNode(getCurrency(entry.getKey()).getDisplayName().toPlain().toLowerCase()).getString()));
                        itemStack.offer(Keys.ITEM_LORE,list);
                    }
                    //把这个物品放进指定格子
                    this.inventory.query(QueryOperationTypes.INVENTORY_PROPERTY.of(SlotIndex.of(entry.getKey()))).set(itemStack);
                }
            }
        }
    }

    /**
     * 尝试交易
     * @param slot
     * @param player
     */

    private void tryTransaction(Slot slot, Player player){
        slot.getInventoryProperty(SlotIndex.class).ifPresent(slotIndex -> {
            //获取slot的index参数，并且这个参数在54之内
            int index = slotIndex.getValue();
            if (index<54 && index>=0 && getCurrency(index)!=null && itemStacks.containsKey(index)){
                this.economyService.getOrCreateAccount(player.getUniqueId()).ifPresent(uniqueAccount -> {
                    //获取玩家账号
                    if (!player.getInventory().canFit(itemStacks.get(index))){
                        //如果玩家背包不能放下这个东西，就关闭背包，提示玩家背包空间不足
                        player.closeInventory();
                        player.sendMessage(Utils.strFormat("&4&l交易失败！你的背包空间不足"));
                        return;
                    }
                    //获取该交易的价格、货币
                    Currency currency = getCurrency(index);
                    assert currency!=null;
                    BigDecimal price = BigDecimal.valueOf(getPrice(index));
                    Cause cause = Cause.of(EventContext.builder().add(EventContextKeys.PLUGIN, Main.INSTANCE.pluginContainer).build(), Main.INSTANCE.pluginContainer);


                    //判断是收购还是出售
                    if(price.compareTo(BigDecimal.ZERO) >= 0){
                        //出售
                        //判断玩家账户是否有足够余额
                        BigDecimal balance = uniqueAccount.getBalance(currency).subtract(price);
                        if (balance.compareTo(BigDecimal.ZERO) < 0){
                            player.sendMessage(Utils.strFormat("&4你的余额不足"));
                        }else {
                            //余额够，尝试交易
                            TransactionResult result = uniqueAccount.withdraw(currency, price, cause);
                            //获取交易结果
                            if (result.getResult().equals(ResultType.SUCCESS)){
                                player.sendMessage(Utils.strFormat("&a交易成功"));
                                //给物品
                                ItemStack itemStack = ItemStack.builder().fromSnapshot(itemStacks.get(index).createSnapshot()).build();
                                player.getInventory().offer(itemStack);
                            }
                        }
                    }else {
                        //价格小于0，收购
                        //先检测玩家背包里有没有这个东西
                        ItemStack itemStack = ItemStack.builder().fromSnapshot(itemStacks.get(index).createSnapshot()).build();
                        if (player.getInventory().contains(itemStack)){
                            //玩家有这个物品，尝试交易
                            //先扣除物品
                            boolean subtractResult = subtract(itemStack, player.getInventory());
                            if (subtractResult){
                                //扣除成功，给钱
                                TransactionResult result = uniqueAccount.deposit(currency,price.abs(),cause);
                                if (result.getResult().equals(ResultType.SUCCESS)){
                                    //给钱成功
                                    player.sendMessage(Utils.strFormat("&a交易成功！"));
                                }else {
                                    //交易有问题
                                    player.sendMessage(Utils.strFormat("&4交易失败！请联系管理员"));
                                }
                            }else {
                                //扣除失败
                                player.sendMessage(Utils.strFormat("&4交易失败！未能成功从你的背包扣除相应物品"));
                            }
                        }else {
                            //背包里没有交易物品
                            player.sendMessage(Utils.strFormat("&4交易失败！未能成功从你的背包找到相应物品"));
                        }
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

    //扣除一个物品
    private static boolean subtract(ItemStack itemStack, Inventory inventory){

        for (Inventory slot:inventory.slots()){
            Optional<ItemStack> optionalItemStack = slot.peek();
            if (optionalItemStack.isPresent() && optionalItemStack.get().getType().equals(itemStack.getType())){
                try {
                    System.out.println(slot.peek());
                    slot.poll(1);
                    return true;
                }catch (Exception e){
                    return false;
                }
            }
        }
        return false;
    }

}
