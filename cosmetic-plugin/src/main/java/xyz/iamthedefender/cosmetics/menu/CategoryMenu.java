package xyz.iamthedefender.cosmetics.menu;

import com.cryptomorin.xseries.XSound;
import com.hakan.core.utils.ColorUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.iamthedefender.cosmetics.Cosmetics;
import xyz.iamthedefender.cosmetics.api.CosmeticsAPI;
import xyz.iamthedefender.cosmetics.api.configuration.ConfigManager;
import xyz.iamthedefender.cosmetics.api.cosmetics.CosmeticsType;
import xyz.iamthedefender.cosmetics.api.cosmetics.RarityType;
import xyz.iamthedefender.cosmetics.api.event.CosmeticPurchaseEvent;
import xyz.iamthedefender.cosmetics.api.menu.ClickableItem;
import xyz.iamthedefender.cosmetics.api.menu.impl.ChestSystemGui;
import xyz.iamthedefender.cosmetics.api.util.ItemBuilder;
import xyz.iamthedefender.cosmetics.api.util.Utility;
import xyz.iamthedefender.cosmetics.category.deathcries.preview.DeathCryPreview;
import xyz.iamthedefender.cosmetics.category.finalkilleffects.preview.FinalKillEffectPreview;
import xyz.iamthedefender.cosmetics.category.glyphs.preview.GlyphPreview;
import xyz.iamthedefender.cosmetics.category.islandtoppers.preview.IslandTopperPreview;
import xyz.iamthedefender.cosmetics.category.killmessage.preview.KillMessagePreview;
import xyz.iamthedefender.cosmetics.category.shopkeeperskins.preview.ShopKeeperPreview;
import xyz.iamthedefender.cosmetics.category.sprays.preview.SprayPreview;
import xyz.iamthedefender.cosmetics.util.DebugUtil;
import xyz.iamthedefender.cosmetics.util.StringUtils;
import xyz.iamthedefender.cosmetics.util.VaultUtils;

import java.util.*;
import java.util.stream.Collectors;

public class CategoryMenu extends ChestSystemGui {

    ConfigManager config;
    CosmeticsType cosmeticsType;
    String title;
    List<Integer> slots;
    int page;

    public CategoryMenu(CosmeticsType type, String title, int page) {
        super(title, 6);
        this.config = type.getConfig();
        this.cosmeticsType = type;
        this.title = title;
        String list = config.getString("slots");
        list = list.replace("[", "").replace("]", "");
        List<Integer> integerList = new ArrayList<>();
        for (String s : list.split("\\s*,\\s*")) {
            integerList.add(Integer.parseInt(s));
        }
        slots = integerList;
        if (slots.isEmpty()){
            slots = Arrays.asList(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34);
        }
       this.page = page;
    }

    public CategoryMenu(CosmeticsType type, String title) {
        this(type, title, 1);
    }

    @Override
    public void onOpen(@NotNull Player player) {
        ConfigManager configManager = cosmeticsType.getConfig();
        ConfigurationSection section = config.getYml().getConfigurationSection(cosmeticsType.getSectionKey());

        if (section == null) return;

        clearInventory();
        Map<ClickableItem, RarityType> rarityMap = new HashMap<>();

        // Set up the items
        for(String id : section.getKeys(false)) {
            // set the variables
            String path = cosmeticsType.getSectionKey() + "." + id + ".";

            ItemStack stack = configManager.getItemStack(path + "item");
            int price = config.getInt(path + "price");
            RarityType rarity = RarityType.valueOf(config.getString(path + "rarity").toUpperCase());
            // From language file
            String formattedName = Utility.getMSGLang(player, "cosmetics." + path + "name");
            List<String> lore = Utility.getListLang(player ,"cosmetics." + path + "lore");
            lore = StringUtils.formatLore(lore, formattedName, price, getItemStatus(player, cosmeticsType, id, price), rarity.getChatColor() + rarity.toString());
            boolean disabled = config.getBoolean(path + "disabled");
            // Items
            ClickableItem item = null;
            List<String> lore1 = new ArrayList<>(lore);

            if (stack != null && !disabled) {
                String colorCode = "&a";
                int returnValue = onClick(player, cosmeticsType, price, id, true);
                if (returnValue == 2){
                    colorCode = "&c";
                }
                if (returnValue == -2 ){ // <- Selected
                    stack.addUnsafeEnchantment(Enchantment.LUCK, 1);
                }

                item = new ClickableItem(new ItemBuilder(stack)
                        .name(colorCode + formattedName)
                        .lore(lore1)
                        .itemFlag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
                        .build(), (e) -> {

                    if (e.getClick() == ClickType.RIGHT){
                        previewClick(player, cosmeticsType, id, price);
                    }else if (e.getClick() == ClickType.LEFT){
                        DebugUtil.addMessage("Left Clicked");
                        onClick(player, cosmeticsType, price, id, false);
                    }
                });
            }

            if (item != null) {
                rarityMap.put(item, rarity);
            }
        }

        if (Cosmetics.getInstance().getConfig().getBoolean("BackItemInCosmeticsMenu")) {
            setItem(49, new ItemBuilder().material(Material.ARROW).name("&aBack").build(), (e) -> new MainMenu((Player) e.getWhoClicked()).open((Player) e.getWhoClicked()));
        }

        createPages(rarityMap);
    }

    @Override
    public void onClose(Player player) {

    }

    public void createPages(Map<ClickableItem, RarityType> rarityMap) {
        List<ClickableItem> items = new ArrayList<>(rarityMap.keySet());

        int itemsPerPage = slots.size();
        int totalPages = items.size() / itemsPerPage;
        int itemStartIndex = (page - 1) * itemsPerPage;
        int itemEndIndex = Math.min(items.size(), itemStartIndex + itemsPerPage);

        List<ClickableItem> pageItems = items.subList(itemStartIndex, itemEndIndex);

        if(page < totalPages) {
            setItem(47, new ItemBuilder().material(Material.ARROW).name("&aNext page").build(), (e) -> new CategoryMenu(cosmeticsType, title, page + 1).open((Player) e.getWhoClicked()));
        }

        if(page > 1) {
            setItem(39, new ItemBuilder().material(Material.ARROW).name("&aPrevious page").build(), (e) -> new CategoryMenu(cosmeticsType, title, page - 1).open((Player) e.getWhoClicked()));
        }

        Map<ClickableItem, RarityType> rarityMapNew = new HashMap<>(
                pageItems.stream().collect(Collectors.toMap(c -> c, rarityMap::get))
        );

        addItemsAccordingToRarity(rarityMapNew);
    }


    public int findFirstEmptySlot(Inventory inventory) {
        for (Integer slot : slots) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return -1;
    }
    
    public boolean isFull(Inventory inventory){
        return findFirstEmptySlot(inventory) == -1;
    }

    public void addItemsAccordingToRarity(Map<ClickableItem, RarityType> rarityMap) {
        rarityMap.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().ordinal()))
                .sorted(Comparator.comparing(entry -> ChatColor.stripColor(entry.getKey().getItemStack().getItemMeta().getDisplayName())))
                .collect(Collectors.groupingBy(Map.Entry::getValue, LinkedHashMap::new, Collectors.mapping(Map.Entry::getKey, Collectors.toList())))
                .forEach((rarity, items) -> items.forEach(item -> {
                    if (!isFull(getInventory())) {
                        setItem(findFirstEmptySlot(getInventory()), item);
                    }
                }));

        String extrasPath = "Extras.fill-empty.";
        if (config.getBoolean(extrasPath + "enabled")) {
            ItemStack stack = ConfigManager.getItemStack(config.getYml(), extrasPath + "item");
            while (getInventory().firstEmpty() != -1) {
                setItem(getInventory().firstEmpty(), new ItemBuilder(stack).name("&r").build());
            }
        }
    }


    public String getItemStatus(Player p, CosmeticsType type, String unformattedName, int price){
        CosmeticsAPI api = Cosmetics.getInstance().getApi();
        String selected = api.getSelectedCosmetic(p, type);
        if (selected.equals(unformattedName)){
            return ColorUtil.colored(Utility.getMSGLang(p, "cosmetics.selected"));
        }

        if (p.hasPermission(type.getPermissionFormat() + "." + unformattedName)){
            return ColorUtil.colored(Utility.getMSGLang(p, "cosmetics.click-to-select"));
        }

        if (type.getConfig().getString(type.getSectionKey() + "." + unformattedName + ".purchase-able") != null){
            boolean purchaseAble = type.getConfig().getBoolean(type.getSectionKey() + "." + unformattedName + ".purchase-able");
            if (!purchaseAble){
                return ColorUtil.colored(Utility.getMSGLang(p, "cosmetics.not-purchase-able"));
            }
        }

        if (Cosmetics.getInstance().getEconomy().getBalance(p) >= price){
            return ColorUtil.colored(Utility.getMSGLang(p, "cosmetics.click-to-purchase"));
        }

        return ColorUtil.colored(Utility.getMSGLang(p, "cosmetics.no-coins"));
    }

    public int onClick(Player p, CosmeticsType type, int price, String id, boolean isOnlyForCheck) {
        CosmeticsAPI api = Cosmetics.getInstance().getApi();
        String selected = api.getSelectedCosmetic(p, type);
        String permissionFormat = type.getPermissionFormat();
        Economy eco = VaultUtils.getEconomy();
        Permission perm = VaultUtils.getPermissions();

        if (selected.equals(id)) return -2;

        if (!p.hasPermission(permissionFormat + "." + id)) {
            if (!type.getConfig().getBoolean(type.getSectionKey() + "." + id + ".purchase-able")) return 2;

            if (eco == null || eco.getBalance(Bukkit.getOfflinePlayer(p.getUniqueId())) < price) {
                if (isOnlyForCheck) return 2;
                p.playSound(p.getLocation(), XSound.ENTITY_ENDERMAN_TELEPORT.parseSound(), 1.0f, 1.0f);
                return -2;
            }

            if (isOnlyForCheck) return 1;

            CosmeticPurchaseEvent event = new CosmeticPurchaseEvent(p, type);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return -1;

            if (perm != null) perm.playerAdd(p, permissionFormat + "." + id);
            api.setSelectedCosmetic(p, type, id);
            eco.withdrawPlayer(p, price);
            p.playSound(p.getLocation(), XSound.ENTITY_VILLAGER_YES.parseSound(), 1.0f, 1.0f);
            new CategoryMenu(cosmeticsType, title, page).open(p);
            return -2;
        }

        if (isOnlyForCheck) return 0;

        DebugUtil.addMessage("Selected " + id + " for " + type);
        api.setSelectedCosmetic(p, type, id);
        XSound.ENTITY_VILLAGER_YES.play(p);
        new CategoryMenu(cosmeticsType, title, page).open(p);
        return -2;
    }


    public void previewClick(Player player, CosmeticsType type, String id, int price){
        switch (type){
            case KillMessage:
                new KillMessagePreview().sendPreviewMessage(player, id);
                break;
            case DeathCries:
                new DeathCryPreview().sendPreviewCry(player, id);
                break;
            case ShopKeeperSkin:
                new ShopKeeperPreview().sendPreviewShopKeeperSkin(player, id, this);
                break;
            case Sprays:
                new SprayPreview().sendSprayPreview(player, id, this);
                break;
            case Glyphs:
                new GlyphPreview().sendPreviewGlyph(player, id, this);
                break;
            case IslandTopper:
                new IslandTopperPreview().sendIslandTopperPreview(player, id, this);
                break;
            case FinalKillEffects:
                new FinalKillEffectPreview().sendPreviewKillEffect(player, id, this);
                break;
            default:
                onClick(player,type, price, id, false);
                break;
        }
    }
}