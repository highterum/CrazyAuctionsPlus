package studio.trc.bukkit.crazyauctionsplus.event;

import studio.trc.bukkit.crazyauctionsplus.util.Category;
import studio.trc.bukkit.crazyauctionsplus.util.enums.Messages;
import studio.trc.bukkit.crazyauctionsplus.util.enums.ShopType;
import studio.trc.bukkit.crazyauctionsplus.util.PluginControl;
import studio.trc.bukkit.crazyauctionsplus.util.FileManager.*;
import studio.trc.bukkit.crazyauctionsplus.util.enums.CancelledReason;
import studio.trc.bukkit.crazyauctionsplus.util.GUI;
import studio.trc.bukkit.crazyauctionsplus.util.FileManager;
import studio.trc.bukkit.crazyauctionsplus.database.GlobalMarket;
import studio.trc.bukkit.crazyauctionsplus.database.Storage;
import studio.trc.bukkit.crazyauctionsplus.util.MarketGoods;
import studio.trc.bukkit.crazyauctionsplus.util.ItemMail;
import studio.trc.bukkit.crazyauctionsplus.api.events.AuctionSellEvent;
import studio.trc.bukkit.crazyauctionsplus.api.events.AuctionBuyEvent;
import studio.trc.bukkit.crazyauctionsplus.api.events.AuctionCancelledEvent;
import studio.trc.bukkit.crazyauctionsplus.api.events.AuctionNewBidEvent;
import studio.trc.bukkit.crazyauctionsplus.currency.CurrencyManager;
import studio.trc.bukkit.crazyauctionsplus.Main;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.InvocationTargetException;

public class GUIAction
    extends GUI
    implements Listener
{
    private final static Main plugin = Main.getInstance();
    
    public final static Map<UUID, Object[]> repricing = new HashMap();
    
    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        if (openingGUI.containsKey(e.getPlayer().getUniqueId())) {
            openingGUI.remove(e.getPlayer().getUniqueId());
        }
        ProtectedConfiguration config = Files.CONFIG.getFile();
        Inventory inv = e.getInventory();
        Player player = (Player) e.getPlayer();
        if (inv != null) {
            if (e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Bidding-On-Item")))) {
                bidding.remove(player.getUniqueId());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (!openingGUI.containsKey(e.getWhoClicked().getUniqueId())) {
            return;
        }
        ProtectedConfiguration config = Files.CONFIG.getFile();
        GlobalMarket market = GlobalMarket.getMarket();
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        if (inv != null) {
            if (e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Categories"))) || openingGUI.get(player.getUniqueId()).equals(GUIType.CATEGORY)) {
                e.setCancelled(true);
                if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
                    player.closeInventory();
                    return;
                }
                int slot = e.getRawSlot();
                if (slot <= inv.getSize()) {
                    if (e.getCurrentItem() != null) {
                        ItemStack item = e.getCurrentItem();
                        if (item.hasItemMeta()) {
                            if (item.getItemMeta().hasDisplayName()) {
                                for (String name : config.getConfigurationSection("Settings.GUISettings.Category-Settings.Custom-Category").getKeys(false)) {
                                    Category category = Category.getModule(config.getString("Settings.GUISettings.Category-Settings.Custom-Category." + name + ".Category-Module"));
                                    if (category == null) continue;
                                    if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.Category-Settings.Custom-Category." + name + ".Name")))) {
                                        openShop(player, shopType.get(player.getUniqueId()), category, 1);
                                        playClick(player);
                                        return;
                                    }
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Categories-Back.Name")))) {
                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.Category-Settings.ShopType-Category.Selling.Name")))) {
                                    openShop(player, ShopType.SELL, shopCategory.get(player.getUniqueId()), 1);
                                    shopType.put(player.getUniqueId(), ShopType.SELL);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.Category-Settings.ShopType-Category.Buying.Name")))) {
                                    openShop(player, ShopType.BUY, shopCategory.get(player.getUniqueId()), 1);
                                    shopType.put(player.getUniqueId(), ShopType.BUY);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.Category-Settings.ShopType-Category.Bidding.Name")))) {
                                    openShop(player, ShopType.BID, shopCategory.get(player.getUniqueId()), 1);
                                    shopType.put(player.getUniqueId(), ShopType.BID);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.Category-Settings.ShopType-Category.None.Name")))) {
                                    openShop(player, ShopType.ANY, shopCategory.get(player.getUniqueId()), 1);
                                    shopType.put(player.getUniqueId(), ShopType.ANY);
                                    playClick(player);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            if (e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Bidding-On-Item"))) || openingGUI.get(player.getUniqueId()).equals(GUIType.BIDDING_ITEM)) {
                e.setCancelled(true);
                if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
                    player.closeInventory();
                    return;
                }
                int slot = e.getRawSlot();
                if (slot <= inv.getSize()) {
                    if (e.getCurrentItem() != null) {
                        ItemStack item = e.getCurrentItem();
                        if (item.hasItemMeta()) {
                            if (item.getItemMeta().hasDisplayName()) {
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Bid.Name")))) {
                                    long ID = biddingID.get(player.getUniqueId());
                                    double bid = bidding.get(player.getUniqueId());
                                    MarketGoods mg = market.getMarketGoods(ID);
                                    if (mg == null) {
                                        e.setCancelled(true);
                                        player.closeInventory();
                                        return;
                                    }
                                    String topBidder = mg.getTopBidder();
                                    if (CurrencyManager.getMoney(player) < bid) {
                                        Map<String, String> placeholders = new HashMap();
                                        placeholders.put("%Money_Needed%", String.valueOf(bid - CurrencyManager.getMoney(player)));
                                        placeholders.put("%money_needed%", String.valueOf(bid - CurrencyManager.getMoney(player)));
                                        Messages.sendMessage(player, "Need-More-Money", placeholders);
                                        return;
                                    }
                                    if (mg.getPrice() > bid) {
                                        Messages.sendMessage(player, "Bid-More-Money");
                                        return;
                                    }
                                    if (mg.getPrice() >= bid && !topBidder.equalsIgnoreCase("None")) {
                                        Messages.sendMessage(player, "Bid-More-Money");
                                        return;
                                    }
                                    if (!topBidder.equalsIgnoreCase("None")) {
                                        String[] oldTopBidder = mg.getTopBidder().split(":");
                                        CurrencyManager.addMoney(Bukkit.getOfflinePlayer(UUID.fromString(oldTopBidder[1])), mg.getPrice());
                                    }
                                    Bukkit.getPluginManager().callEvent(new AuctionNewBidEvent(player, mg, bid));
                                    CurrencyManager.removeMoney(player, bid);
                                    mg.setPrice(bid);
                                    mg.setTopBidder(player.getName() + ":" + player.getUniqueId());
                                    Map<String, String> placeholders = new HashMap();
                                    placeholders.put("%Bid%", String.valueOf(bid));
                                    placeholders.put("%bid%", String.valueOf(bid));
                                    Messages.sendMessage(player, "Bid-Msg", placeholders);
                                    bidding.put(player.getUniqueId(), 0);
                                    player.closeInventory();
                                    playClick(player);
                                    return;
                                }
                                HashMap<String, Integer> priceEdits = new HashMap<>();
                                priceEdits.put("&a+1", 1);
                                priceEdits.put("&a+10", 10);
                                priceEdits.put("&a+100", 100);
                                priceEdits.put("&a+1000", 1000);
                                priceEdits.put("&c-1", -1);
                                priceEdits.put("&c-10", -10);
                                priceEdits.put("&c-100", -100);
                                priceEdits.put("&c-1000", -1000);
                                for (String price : priceEdits.keySet()) {
                                    if (item.getItemMeta().getDisplayName().equals(PluginControl.color(price))) {
                                        try {
                                            bidding.put(player.getUniqueId(), (bidding.get(player.getUniqueId()) + priceEdits.get(price)));
                                            inv.setItem(4, getBiddingItem(player, biddingID.get(player.getUniqueId())));
                                            inv.setItem(13, getBiddingGlass(player, biddingID.get(player.getUniqueId())));
                                            playClick(player);
                                            return;
                                        } catch (Exception ex) {
                                            player.closeInventory();
                                            Messages.sendMessage(player, "Item-Doesnt-Exist");
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Main-GUIName"))) ||
                e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Player-Viewer-GUIName"))) ||
                e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Sell-GUIName"))) ||
                e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Buy-GUIName"))) ||
                e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Bid-GUIName"))) ||
                    openingGUI.get(player.getUniqueId()).equals(GUIType.GLOBALMARKET_MAIN) ||
                    openingGUI.get(player.getUniqueId()).equals(GUIType.GLOBALMARKET_SELL) ||
                    openingGUI.get(player.getUniqueId()).equals(GUIType.GLOBALMARKET_BID) ||
                    openingGUI.get(player.getUniqueId()).equals(GUIType.GLOBALMARKET_BUY) ||
                    openingGUI.get(player.getUniqueId()).equals(GUIType.ITEM_VIEWER)) {
                e.setCancelled(true);
                if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
                    player.closeInventory();
                    return;
                }
                int slot = e.getRawSlot();
                if (slot <= inv.getSize()) {
                    if (e.getCurrentItem() != null) {
                        final ItemStack item = e.getCurrentItem();
                        if (item.hasItemMeta()) {
                            if (item.getItemMeta().hasDisplayName()) {
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.NextPage.Name")))) {
                                    PluginControl.updateCacheData();
                                    int page = Integer.parseInt(e.getView().getTitle().split("#")[1]);
                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), page + 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.PreviousPage.Name")))) {
                                    PluginControl.updateCacheData();
                                    int page = Integer.parseInt(e.getView().getTitle().split("#")[1]);
                                    if (page == 1) page++;
                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), page - 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Refesh.Name")))) {
                                    PluginControl.updateCacheData();
                                    int page = Integer.parseInt(e.getView().getTitle().split("#")[1]);
                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), page);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Shopping.Others.Name")))) {
                                    openShop(player, ShopType.SELL, shopCategory.get(player.getUniqueId()), 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Shopping.Selling.Name")))) {
                                    openShop(player, ShopType.BUY, shopCategory.get(player.getUniqueId()), 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Shopping.Buying.Name")))) {
                                    openShop(player, ShopType.BID, shopCategory.get(player.getUniqueId()), 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Shopping.Bidding.Name")))) {
                                    openShop(player, ShopType.ANY, shopCategory.get(player.getUniqueId()), 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Items-Mail.Name")))) {
                                    openPlayersMail(player, 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Commoditys.Name")))) {
                                    openPlayersCurrentList(player, 1);
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Category.Name")))) {
                                    openCategories(player, shopType.get(player.getUniqueId()));
                                    playClick(player);
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Custom.Name")))) {
                                    for (String commands : config.getStringList("Settings.GUISettings.OtherSettings.Custom.Commands")) {
                                        if (commands.toLowerCase().startsWith("server:")) {
                                            String[] command = commands.split(":");
                                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command[1].replace("%player%", player.getName()).replace("%player_uuid%", player.getUniqueId().toString()));
                                        } else if (commands.toLowerCase().startsWith("op:")) {
                                            String[] command = commands.split(":");
                                            if (!player.isOp()) {
                                                try {
                                                    player.setOp(true);
                                                    player.performCommand(command[1].replace("%player%", player.getName()).replace("%player_uuid%", player.getUniqueId().toString()));
                                                    player.setOp(false);
                                                } catch (Exception ex) {
                                                    player.setOp(false);
                                                    PluginControl.printStackTrace(ex);
                                                }
                                            } else {
                                                player.performCommand(command[1].replace("%player%", player.getName()).replace("%player_uuid%", player.getUniqueId().toString()));
                                            }
                                        } else if (commands.toLowerCase().startsWith("player:")) {
                                            String[] command = commands.split(":");
                                            player.performCommand(command[1].replace("%player%", player.getName()).replace("%player_uuid%", player.getUniqueId().toString()));
                                        }
                                    }
                                    playClick(player);
                                    if (config.getBoolean("Settings.GUISettings.OtherSettings.Custom.Close")) {
                                        player.closeInventory();
                                    }
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Your-Item.Name")))) {
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Cant-Afford.Name")))) {
                                    return;
                                }
                                if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Top-Bidder.Name")))) {
                                    return;
                                }
                            }
                            if (itemUID.containsKey(player.getUniqueId())) {
                                if (itemUID.get(player.getUniqueId()).size() >= slot) {
                                    long uid = itemUID.get(player.getUniqueId()).get(slot);
                                    for (MarketGoods mgs : market.getItems()) {
                                        if (uid == mgs.getUID()) {
                                            if (PluginControl.hasMarketPermission(player, "Cancelled-Item")) {
                                                if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                                                    UUID owner = mgs.getItemOwner().getUUID();
                                                    Player p = Bukkit.getPlayer(owner);
                                                    if (p != null) {
                                                        Messages.sendMessage(p, "Admin-Force-Cancelled-To-Player");
                                                    }
                                                    switch (mgs.getShopType()) {
                                                        case BID: {
                                                            AuctionCancelledEvent event = new AuctionCancelledEvent((p != null ? p : Bukkit.getOfflinePlayer(owner)), mgs, CancelledReason.ADMIN_FORCE_CANCEL, ShopType.BID);
                                                            Bukkit.getPluginManager().callEvent(event);
                                                            Storage playerdata = Storage.getPlayer(Bukkit.getOfflinePlayer(owner));
                                                            if (mgs.getTopBidder() != null && !mgs.getTopBidder().equalsIgnoreCase("None")) {
                                                                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(mgs.getTopBidder().split(":")[1]));
                                                                if (op != null) {
                                                                    CurrencyManager.addMoney(op, mgs.getPrice());
                                                                }
                                                            }
                                                            playerdata.addItem(new ItemMail(playerdata.makeUID(), Bukkit.getOfflinePlayer(owner), mgs.getItem(), mgs.getFullTime(), System.currentTimeMillis(),  false));
                                                            market.removeGoods(mgs.getUID());
                                                            break;
                                                        }
                                                        case BUY: {
                                                            AuctionCancelledEvent event = new AuctionCancelledEvent((p != null ? p : Bukkit.getOfflinePlayer(owner)), mgs, CancelledReason.ADMIN_FORCE_CANCEL, ShopType.BUY);
                                                            Bukkit.getPluginManager().callEvent(event);
                                                            CurrencyManager.addMoney(Bukkit.getOfflinePlayer(owner), mgs.getReward());
                                                            market.removeGoods(uid);
                                                            break;
                                                        }
                                                        case SELL: {
                                                            AuctionCancelledEvent event = new AuctionCancelledEvent((p != null ? p : Bukkit.getOfflinePlayer(owner)), mgs, CancelledReason.ADMIN_FORCE_CANCEL, ShopType.SELL);
                                                            Bukkit.getPluginManager().callEvent(event);
                                                            Storage playerdata = Storage.getPlayer(Bukkit.getOfflinePlayer(owner));
                                                            playerdata.addItem(new ItemMail(playerdata.makeUID(), Bukkit.getOfflinePlayer(owner), mgs.getItem(), mgs.getFullTime(), System.currentTimeMillis(), false));
                                                            market.removeGoods(mgs.getUID());
                                                            break;
                                                        }
                                                    }
                                                    Messages.sendMessage(player, "Admin-Force-Cancelled");
                                                    playClick(player);
                                                    int page = Integer.parseInt(e.getView().getTitle().split("#")[1]);
                                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), page);
                                                    return;
                                                }
                                            }
                                            Runnable runnable = () -> inv.setItem(slot, item);
                                            if (mgs.getItemOwner().getUUID().equals(player.getUniqueId())) {
                                                String it = config.getString("Settings.GUISettings.OtherSettings.Your-Item.Item");
                                                String name = config.getString("Settings.GUISettings.OtherSettings.Your-Item.Name");
                                                ItemStack I;
                                                if (config.contains("Settings.GUISettings.OtherSettings.Your-Item.Lore")) {
                                                    I = PluginControl.makeItem(it, 1, name, config.getStringList("Settings.GUISettings.OtherSettings.Your-Item.Lore"));
                                                } else {
                                                    I = PluginControl.makeItem(it, 1, name);
                                                }
                                                inv.setItem(slot, I);
                                                playClick(player);
                                                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 3 * 20);
                                                return;
                                            }
                                            double cost = mgs.getPrice();
                                            if (mgs.getShopType().equals(ShopType.BUY)) {
                                                cost = mgs.getReward();
                                            }
                                            if (CurrencyManager.getMoney(player) < cost && !mgs.getShopType().equals(ShopType.BUY)) {
                                                String it = config.getString("Settings.GUISettings.OtherSettings.Cant-Afford.Item");
                                                String name = config.getString("Settings.GUISettings.OtherSettings.Cant-Afford.Name");
                                                ItemStack I;
                                                if (config.contains("Settings.GUISettings.OtherSettings.Cant-Afford.Lore")) {
                                                    I = PluginControl.makeItem(it, 1, name, config.getStringList("Settings.GUISettings.OtherSettings.Cant-Afford.Lore"));
                                                } else {
                                                    I = PluginControl.makeItem(it, 1, name);
                                                }
                                                inv.setItem(slot, I);
                                                playClick(player);
                                                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 3 * 20);
                                                return;
                                            } else if (mgs.getShopType().equals(ShopType.BUY) && !PluginControl.itemExists(player, mgs.getItem())) {
                                                String it = config.getString("Settings.GUISettings.OtherSettings.Not-owned.Item");
                                                String name = config.getString("Settings.GUISettings.OtherSettings.Not-owned.Name");
                                                ItemStack I;
                                                if (config.contains("Settings.GUISettings.OtherSettings.Not-owned.Lore")) {
                                                    I = PluginControl.makeItem(it, 1, name, config.getStringList("Settings.GUISettings.OtherSettings.Not-owned.Lore"));
                                                } else {
                                                    I = PluginControl.makeItem(it, 1, name);
                                                }
                                                inv.setItem(slot, I);
                                                playClick(player);
                                                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 3 * 20);
                                                return;
                                            }
                                            switch (mgs.getShopType()) {
                                                case BID: {
                                                    if (!mgs.getTopBidder().equalsIgnoreCase("None") && UUID.fromString(mgs.getTopBidder().split(":")[1]).equals(player.getUniqueId())) {
                                                        String it = config.getString("Settings.GUISettings.OtherSettings.Top-Bidder.Item");
                                                        String name = config.getString("Settings.GUISettings.OtherSettings.Top-Bidder.Name");
                                                        ItemStack I;
                                                        if (config.contains("Settings.GUISettings.OtherSettings.Top-Bidder.Lore")) {
                                                            I = PluginControl.makeItem(it, 1, name, config.getStringList("Settings.GUISettings.OtherSettings.Top-Bidder.Lore"));
                                                        } else {
                                                            I = PluginControl.makeItem(it, 1, name);
                                                        }
                                                        inv.setItem(slot, I);
                                                        playClick(player);
                                                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 3 * 20);
                                                        return;
                                                    }
                                                    playClick(player);
                                                    openBidding(player, mgs.getUID());
                                                    biddingID.put(player.getUniqueId(), mgs.getUID());
                                                    break;
                                                }
                                                case BUY: {
                                                    playClick(player);
                                                    openSelling(player, mgs.getUID());
                                                    break;
                                                }
                                                case SELL: {
                                                    playClick(player);
                                                    openBuying(player, mgs.getUID());
                                                    break;
                                                }
                                            }
                                            return;
                                        }
                                    }
                                    playClick(player);
                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                    Messages.sendMessage(player, "Item-Doesnt-Exist");
                                    return;
                                }
                                playClick(player);
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Buying-Item"))) || openingGUI.get(player.getUniqueId()).equals(GUIType.BUYING_ITEM)) {
            e.setCancelled(true);
            if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
                player.closeInventory();
                return;
            }
            int slot = e.getRawSlot();
            if (slot <= inv.getSize()) {
                if (e.getCurrentItem() != null) {
                    ItemStack item = e.getCurrentItem();
                    if (item.hasItemMeta()) {
                        if (item.getItemMeta().hasDisplayName()) {
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Confirm.Name")))) {
                                long uid = IDs.get(player.getUniqueId());
                                MarketGoods mg = market.getMarketGoods(uid);
                                if (mg == null) {
                                    playClick(player);
                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                    Messages.sendMessage(player, "Item-Doesnt-Exist");
                                    return;
                                }
                                if (PluginControl.isInvFull(player)) {
                                    playClick(player);
                                    player.closeInventory();
                                    Messages.sendMessage(player, "Inventory-Full");
                                    return;
                                }
                                if (CurrencyManager.getMoney(player) < mg.getPrice()) {
                                    playClick(player);
                                    player.closeInventory();
                                    HashMap<String, String> placeholders = new HashMap();
                                    placeholders.put("%Money_Needed%", String.valueOf(mg.getPrice() - CurrencyManager.getMoney(player)));
                                    placeholders.put("%money_needed%", String.valueOf(mg.getPrice() - CurrencyManager.getMoney(player)));
                                    Messages.sendMessage(player, "Need-More-Money", placeholders);
                                    return;
                                }
                                UUID owner = mg.getItemOwner().getUUID();
                                Bukkit.getPluginManager().callEvent(new AuctionBuyEvent(player, mg, mg.getPrice()));
                                CurrencyManager.removeMoney(player, mg.getPrice());
                                CurrencyManager.addMoney(PluginControl.getOfflinePlayer(owner), mg.getPrice());
                                Map<String, String> placeholders = new HashMap();
                                placeholders.put("%Price%", String.valueOf(mg.getPrice()));
                                placeholders.put("%price%", String.valueOf(mg.getPrice()));
                                placeholders.put("%Player%", player.getName());
                                placeholders.put("%player%", player.getName());
                                Messages.sendMessage(player, "Bought-Item", placeholders);
                                if (PluginControl.isOnline(owner) && PluginControl.getPlayer(owner) != null) {
                                    Player p = PluginControl.getPlayer(owner);
                                    Messages.sendMessage(p, "Player-Bought-Item", placeholders);
                                }
                                player.getInventory().addItem(mg.getItem());
                                market.removeGoods(uid);
                                playClick(player);
                                openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                return;
                            }
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Cancel.Name")))) {
                                openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                playClick(player);
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Selling-Item"))) || openingGUI.get(player.getUniqueId()).equals(GUIType.SELLING_ITEM)) {
            e.setCancelled(true);
            if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
                player.closeInventory();
                return;
            }
            int slot = e.getRawSlot();
            if (slot <= inv.getSize()) {
                if (e.getCurrentItem() != null) {
                    ItemStack item = e.getCurrentItem();
                    if (item.hasItemMeta()) {
                        if (item.getItemMeta().hasDisplayName()) {
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Confirm.Name")))) {
                                long uid = IDs.get(player.getUniqueId());
                                MarketGoods mg = market.getMarketGoods(uid);
                                if (mg == null) {
                                    playClick(player);
                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                    Messages.sendMessage(player, "Item-Doesnt-Exist");
                                    return;
                                }
                                ItemStack i = mg.getItem();
                                if (!PluginControl.itemExists(player, i)) {
                                    playClick(player);
                                    Messages.sendMessage(player, "Item-Not-Found");
                                    return;
                                }
                                UUID owner = mg.getItemOwner().getUUID();
                                Bukkit.getPluginManager().callEvent(new AuctionSellEvent(player, mg, mg.getReward()));
                                HashMap<String, String> placeholders = new HashMap();
                                placeholders.put("%reward%", String.valueOf(mg.getReward()));
                                placeholders.put("%reward%", String.valueOf(mg.getReward()));
                                placeholders.put("%Player%", player.getName());
                                placeholders.put("%player%", player.getName());
                                PluginControl.takeItem(player, i);
                                CurrencyManager.addMoney(player, mg.getReward());
                                Storage playerdata = Storage.getPlayer(Bukkit.getOfflinePlayer(owner));
                                playerdata.addItem(new ItemMail(playerdata.makeUID(), Bukkit.getOfflinePlayer(owner), mg.getItem(), PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")), System.currentTimeMillis(), true));
                                market.removeGoods(uid);
                                Messages.sendMessage(player, "Sell-Item", placeholders);
                                if (PluginControl.isOnline(owner) && PluginControl.getPlayer(owner) != null) {
                                    Player p = PluginControl.getPlayer(owner);
                                    Messages.sendMessage(p, "Player-Sell-Item", placeholders);
                                }
                                playClick(player);
                                openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                return;
                            }
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Cancel.Name")))) {
                                openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                playClick(player);
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Player-Items-List"))) || openingGUI.get(player.getUniqueId()).equals(GUIType.ITEM_LIST)) {
            e.setCancelled(true);
            if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
                player.closeInventory();
                return;
            }
            int slot = e.getRawSlot();
            if (slot <= inv.getSize()) {
                if (e.getCurrentItem() != null) {
                    ItemStack item = e.getCurrentItem();
                    if (item.hasItemMeta()) {
                        if (item.getItemMeta().hasDisplayName()) {
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Player-Items-List-Back.Name")))) {
                                openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                playClick(player);
                                return;
                            }
                        }
                        if (itemUID.containsKey(player.getUniqueId())) {
                            if (itemUID.get(player.getUniqueId()).size() >= slot) {
                                long uid = itemUID.get(player.getUniqueId()).get(slot);
                                boolean Repricing = e.getClick().equals(ClickType.RIGHT) || e.getClick().equals(ClickType.SHIFT_RIGHT);
                                MarketGoods mg = market.getMarketGoods(uid);
                                if (mg == null) {
                                    playClick(player);
                                    openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                    Messages.sendMessage(player, "Item-Doesnt-Exist");
                                }
                                switch (mg.getShopType()) {
                                    case BID: {
                                        Map<String, String> placeholders = new HashMap();
                                        try {
                                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : (String) mg.getItem().getClass().getMethod("getI18NDisplayName").invoke(mg.getItem()));
                                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : mg.getItem().getType().toString().toLowerCase().replace("_", " "));
                                        }
                                        Messages.sendMessage(player, "Cancelled-Item-On-Bid", placeholders);
                                        AuctionCancelledEvent event = new AuctionCancelledEvent(player, mg, CancelledReason.PLAYER_FORCE_CANCEL, ShopType.BID);
                                        Bukkit.getPluginManager().callEvent(event);
                                        if (mg.getTopBidder() != null && !mg.getTopBidder().equalsIgnoreCase("None")) {
                                            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(mg.getTopBidder().split(":")[1]));
                                            if (op != null) {
                                                CurrencyManager.addMoney(op, mg.getPrice());
                                            }
                                        }
                                        Storage playerdata = Storage.getPlayer(mg.getItemOwner().getUUID());
                                        playerdata.addItem(new ItemMail(playerdata.makeUID(), mg.getItemOwner().getUUID(), mg.getItem(), PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")), System.currentTimeMillis(), false));
                                        market.removeGoods(uid);
                                        repricing.remove(player.getUniqueId());
                                        playClick(player);
                                        openPlayersCurrentList(player, 1);
                                        return;
                                    }
                                    case BUY: {
                                        if (Repricing) {
                                            repricing.put(player.getUniqueId(), new Object[] {mg, String.valueOf(System.currentTimeMillis() + (config.getInt("Settings.Repricing-Timeout") * 1000))});
                                            Map<String, String> placeholders = new HashMap();
                                            try {
                                                placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : (String) mg.getItem().getClass().getMethod("getI18NDisplayName").invoke(mg.getItem()));
                                            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                                placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : mg.getItem().getType().toString().toLowerCase().replace("_", " "));
                                            }
                                            placeholders.put("%timeout%", config.getString("Settings.Repricing-Timeout"));
                                            Messages.sendMessage(player, "Repricing", placeholders);
                                            playClick(player);
                                            player.closeInventory();
                                            return;
                                        }
                                        Map<String, String> placeholders = new HashMap();
                                        placeholders.put("%reward%", String.valueOf(mg.getReward()));
                                        placeholders.put("%reward%", String.valueOf(mg.getReward()));
                                        try {
                                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : (String) mg.getItem().getClass().getMethod("getI18NDisplayName").invoke(mg.getItem()));
                                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : mg.getItem().getType().toString().toLowerCase().replace("_", " "));
                                        }
                                        Messages.sendMessage(player, "Cancelled-Item-On-Buy", placeholders);
                                        AuctionCancelledEvent event = new AuctionCancelledEvent(player, mg, CancelledReason.PLAYER_FORCE_CANCEL, ShopType.BUY);
                                        Bukkit.getPluginManager().callEvent(event);
                                        CurrencyManager.addMoney(player, mg.getReward());
                                        market.removeGoods(uid);
                                        repricing.remove(player.getUniqueId());
                                        playClick(player);
                                        openPlayersCurrentList(player, 1);
                                        return;
                                    }
                                    case SELL: {
                                        if (Repricing) {
                                            repricing.put(player.getUniqueId(), new Object[] {mg, String.valueOf(System.currentTimeMillis() + (config.getInt("Settings.Repricing-Timeout") * 1000))});
                                            Map<String, String> placeholders = new HashMap();
                                            try {
                                                placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : (String) mg.getItem().getClass().getMethod("getI18NDisplayName").invoke(mg.getItem()));
                                            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                                placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : mg.getItem().getType().toString().toLowerCase().replace("_", " "));
                                            }
                                            placeholders.put("%timeout%", config.getString("Settings.Repricing-Timeout"));
                                            Messages.sendMessage(player, "Repricing", placeholders);
                                            playClick(player);
                                            player.closeInventory();
                                            return;
                                        }
                                        Map<String, String> placeholders = new HashMap();
                                        try {
                                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : (String) mg.getItem().getClass().getMethod("getI18NDisplayName").invoke(mg.getItem()));
                                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : mg.getItem().getType().toString().toLowerCase().replace("_", " "));
                                        }
                                        Messages.sendMessage(player, "Cancelled-Item-On-Sale", placeholders);
                                        AuctionCancelledEvent event = new AuctionCancelledEvent(player, mg, CancelledReason.PLAYER_FORCE_CANCEL, ShopType.SELL);
                                        Bukkit.getPluginManager().callEvent(event);
                                        Storage playerdata = Storage.getPlayer(mg.getItemOwner().getUUID());
                                        playerdata.addItem(new ItemMail(playerdata.makeUID(), mg.getItemOwner().getUUID(), mg.getItem(), PluginControl.convertToMill(FileManager.Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")), System.currentTimeMillis(), false));
                                        market.removeGoods(uid);
                                        repricing.remove(player.getUniqueId());
                                        playClick(player);
                                        openPlayersCurrentList(player, 1);
                                        return;
                                    }
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (e.getView().getTitle().contains(PluginControl.color(config.getString("Settings.Player-Items-Mail"))) || openingGUI.get(player.getUniqueId()).equals(GUIType.ITEM_MAIL)) {
            e.setCancelled(true);
            if (FileManager.isBackingUp() || FileManager.isRollingBack() || PluginControl.isWorldDisabled(player)) {
                player.closeInventory();
                return;
            }
            int slot = e.getRawSlot();
            if (slot <= inv.getSize()) {
                if (e.getCurrentItem() != null) {
                    Storage playerdata = Storage.getPlayer(openingMail.get(player.getUniqueId()));
                    ItemStack item = e.getCurrentItem();
                    if (item.hasItemMeta()) {
                        if (item.getItemMeta().hasDisplayName()) {
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Player-Items-Mail-Back.Name")))) {
                                PluginControl.updateCacheData();
                                playClick(player);
                                openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                return;
                            }
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.PreviousPage.Name")))) {
                                PluginControl.updateCacheData();
                                int page = Integer.parseInt(e.getView().getTitle().split("#")[1]);
                                if (page == 1) page++;
                                playClick(player);
                                openPlayersMail(player, (page - 1));
                                return;
                            }
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.Return.Name")))) {
                                PluginControl.updateCacheData();
                                int page = Integer.parseInt(e.getView().getTitle().split("#")[1]);
                                for (ItemMail im : playerdata.getMailBox()) {
                                    if (PluginControl.isInvFull(player)) {
                                        Messages.sendMessage(player, "Inventory-Full");
                                        playerdata.saveData();
                                        return;
                                    }
                                    im.giveItem();
                                }
                                playerdata.clearMailBox();
                                Messages.sendMessage(player, "Got-All-Item-Back");
                                playClick(player);
                                openPlayersMail(player, page);
                                return;
                            }
                            if (item.getItemMeta().getDisplayName().equals(PluginControl.color(config.getString("Settings.GUISettings.OtherSettings.NextPage.Name")))) {
                                PluginControl.updateCacheData();
                                int page = Integer.parseInt(e.getView().getTitle().split("#")[1]);
                                playClick(player);
                                openPlayersMail(player, (page + 1));
                                return;
                            }
                        }
                        if (mailUID.containsKey(player.getUniqueId())) {
                            if (mailUID.get(player.getUniqueId()).size() >= slot) {
                                long uid = mailUID.get(player.getUniqueId()).get(slot);
                                for (ItemMail im : playerdata.getMailBox()) {
                                    if (uid == im.getUID()) {
                                        if (!PluginControl.isInvFull(player)) {
                                            Messages.sendMessage(player, "Got-Item-Back");
                                            im.giveItem();
                                            playerdata.saveData();
                                            playClick(player);
                                            openPlayersMail(player, 1);
                                        } else {
                                            Messages.sendMessage(player, "Inventory-Full");
                                        }
                                        return;
                                    }
                                }
                                playClick(player);
                                openShop(player, shopType.get(player.getUniqueId()), shopCategory.get(player.getUniqueId()), 1);
                                Messages.sendMessage(player, "Item-Doesnt-Exist");
                            }
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRepricing(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (repricing.get(player.getUniqueId()) != null) {
            ProtectedConfiguration config = Files.CONFIG.getFile();
            if (!PluginControl.isNumber(e.getMessage())) {
                Map<String, String> placeholders = new HashMap();
                placeholders.put("%Arg%", e.getMessage());
                placeholders.put("%arg%", e.getMessage());
                Messages.sendMessage(player, "Not-A-Valid-Number", placeholders);
                repricing.remove(player.getUniqueId());
                e.setCancelled(true);
                return;
            }
            MarketGoods mg;
            try {
                mg = (MarketGoods) repricing.get(player.getUniqueId())[0];
            } catch (ClassCastException ex) {
                PluginControl.printStackTrace(ex);
                return;
            }
            if (mg != null && mg.getItem() != null) {
                double money = Double.valueOf(e.getMessage());
                switch (mg.getShopType()) {
                    case BUY: {
                        if (money < config.getDouble("Settings.Minimum-Buy-Reward")) {
                            Map<String, String> placeholders = new HashMap();
                            placeholders.put("%reward%", String.valueOf(config.getDouble("Settings.Minimum-Buy-Reward")));
                            Messages.sendMessage(player, "Buy-Reward-To-Low", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        if (money > config.getLong("Settings.Max-Beginning-Buy-Reward")) {
                            Map<String, String> placeholders = new HashMap();
                            placeholders.put("%reward%", String.valueOf(config.getDouble("Settings.Max-Beginning-Buy-Reward")));
                            Messages.sendMessage(player, "Buy-Reward-To-High", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        if (CurrencyManager.getMoney(player) < money) { 
                            HashMap<String, String> placeholders = new HashMap();
                            placeholders.put("%Money_Needed%", String.valueOf(money - CurrencyManager.getMoney(player)));
                            placeholders.put("%money_needed%", String.valueOf(money - CurrencyManager.getMoney(player)));
                            Messages.sendMessage(player, "Need-More-Money", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        double tax = 0;
                        if (!PluginControl.bypassTaxRate(player, ShopType.BUY)) {
                            tax = money * PluginControl.getTaxRate(player, ShopType.BUY);
                        }
                        if (CurrencyManager.getMoney(player) < money + tax) { 
                            HashMap<String, String> placeholders = new HashMap();
                            placeholders.put("%Money_Needed%", String.valueOf((money + tax) - CurrencyManager.getMoney(player)));
                            placeholders.put("%money_needed%", String.valueOf((money + tax) - CurrencyManager.getMoney(player)));
                            Messages.sendMessage(player, "Need-More-Money", placeholders);
                            e.setCancelled(true);
                            return;
                        }
                        CurrencyManager.removeMoney(player, money + tax);
                        CurrencyManager.addMoney(player, mg.getReward());
                        mg.setPrice(money);
                        Map<String, String> placeholders = new HashMap();
                        placeholders.put("%money%", String.valueOf(money));
                        placeholders.put("%tax%", String.valueOf(tax));
                        try {
                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : (String) mg.getItem().getClass().getMethod("getI18NDisplayName").invoke(mg.getItem()));
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : mg.getItem().getType().toString().toLowerCase().replace("_", " "));
                        }
                        Messages.sendMessage(player, "Repricing-Succeeded", placeholders);
                        repricing.remove(player.getUniqueId());
                        e.setCancelled(true);
                        break;
                    }
                    case SELL: {
                        if (money < config.getDouble("Settings.Minimum-Sell-Price")) {
                            Map<String, String> placeholders = new HashMap();
                            placeholders.put("%price%", String.valueOf(config.getDouble("Settings.Minimum-Sell-Price")));
                            Messages.sendMessage(player, "Sell-Price-To-Low", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        if (money > config.getLong("Settings.Max-Beginning-Sell-Price")) {
                            Map<String, String> placeholders = new HashMap();
                            placeholders.put("%price%", String.valueOf(config.getDouble("Settings.Max-Beginning-Sell-Price")));
                            Messages.sendMessage(player, "Sell-Price-To-High", placeholders);
                            repricing.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                        double tax = 0;
                        if (!PluginControl.bypassTaxRate(player, ShopType.SELL)) {
                            tax = money * PluginControl.getTaxRate(player, ShopType.SELL);
                        }
                        if (CurrencyManager.getMoney(player) < tax) {
                            HashMap<String, String> placeholders = new HashMap();
                            placeholders.put("%Money_Needed%", String.valueOf(tax - CurrencyManager.getMoney(player)));
                            placeholders.put("%money_needed%", String.valueOf(tax - CurrencyManager.getMoney(player)));
                            Messages.sendMessage(player, "Need-More-Money", placeholders);
                            e.setCancelled(true);
                            return;
                        }
                        CurrencyManager.removeMoney(player, tax);
                        mg.setPrice(money);
                        Map<String, String> placeholders = new HashMap();
                        placeholders.put("%money%", String.valueOf(money));
                        placeholders.put("%tax%", String.valueOf(tax));
                        try {
                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : (String) mg.getItem().getClass().getMethod("getI18NDisplayName").invoke(mg.getItem()));
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            placeholders.put("%item%", mg.getItem().getItemMeta().hasDisplayName() ? mg.getItem().getItemMeta().getDisplayName() : mg.getItem().getType().toString().toLowerCase().replace("_", " "));
                        }
                        Messages.sendMessage(player, "Repricing-Succeeded", placeholders);
                        repricing.remove(player.getUniqueId());
                        e.setCancelled(true);
                        break;
                    }
                }
            } else {
                Messages.sendMessage(player, "Repricing-Failed");
                repricing.remove(player.getUniqueId());
                e.setCancelled(true);
            }
        }
    }
}
