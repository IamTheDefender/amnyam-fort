package me.defender.cosmetics.api.cosmetics.category;

import com.cryptomorin.xseries.XMaterial;
import me.defender.cosmetics.api.configuration.ConfigManager;
import me.defender.cosmetics.api.cosmetics.Cosmetics;
import me.defender.cosmetics.api.cosmetics.FieldsType;
import me.defender.cosmetics.api.cosmetics.RarityType;
import me.defender.cosmetics.category.islandtoppers.items.DummyTopper;
import me.defender.cosmetics.util.StartupUtils;
import me.defender.cosmetics.util.Utility;
import me.defender.cosmetics.util.config.ConfigType;
import me.defender.cosmetics.util.config.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.defender.cosmetics.util.Utility.saveIfNotExistsLang;
import static me.defender.cosmetics.util.config.ConfigUtils.get;
import static me.defender.cosmetics.util.config.ConfigUtils.saveIfNotFound;

public abstract class IslandTopper extends Cosmetics {

    /**
     * Display the topper
     *
     * @param player the player to display the topper
     * @param topperLocation the location to display the topper
     * @param selected the selected topper
     */
    public abstract void execute(Player player, Location topperLocation, String selected);

    /**
     * Register the topper.
     * This method should be called when the plugin is enabled.
     */
    @Override
    public void register(){
        // save to config
        String category = "island-topper";
        String configPath = category + "." + getIdentifier() + ".";
        ConfigType type = ConfigType.ISLAND_TOPPERS;
        saveIfNotFound(type, configPath + "price", getPrice());
        saveIfNotFound(type, configPath + "rarity", getRarity().toString());
        if(!XMaterial.matchXMaterial(getItem()).isSupported()) {
            Bukkit.getLogger().severe("The item is not supported! (Information: Category name is " + category + " and item name is " + getIdentifier());
            return;
        }
        if(XMaterial.matchXMaterial(getItem()).isSimilar(XMaterial.PLAYER_HEAD.parseItem())){
            get(type).setItemStack(configPath + "item", getItem(), base64());
        }else{
            get(type).setItemStack(configPath + "item", getItem());
        }

        // save to language file
        saveIfNotExistsLang("cosmetics." + configPath + "name", getDisplayName());
        // Format the lore
        List<String> finalLore = new ArrayList<>();
        finalLore.addAll(Arrays.asList("&8Island Topper", ""));
        finalLore.addAll(getLore());
        finalLore.addAll(Arrays.asList("", "&eRight-Click to preview!", "" ,"&7Rarity: {rarity}","&7Cost: &6{cost}", "", "{status}"));

        saveIfNotExistsLang("cosmetics." + configPath + "lore", finalLore);
        StartupUtils.islandTopperList.add(this);
    }

    /**
     * Get the topper's field
     * @param fields the field to get
     * @param p the player to get the field
     * @return the field
     */
    public Object getField(FieldsType fields, Player p){
        String category = "island-topper";
        String configPath = category + "." + getIdentifier() + ".";
        ConfigManager config = ConfigUtils.getIslandToppers();
        switch (fields){
            case NAME:
                return Utility.getMSGLang(p, "cosmetics." + configPath + "name");
            case PRICE:
                return config.getInt(configPath + "price");
            case LORE:
                return Utility.getListLang(p, "cosmetics." + configPath + "lore");
            case RARITY:
                return RarityType.valueOf(config.getString(configPath + "rarity"));
            case ITEM_STACK:
                return config.getItemStack(configPath + "item");
            default:
                return null;
        }
    }

    /**
     * Get the default topper
     * @param player the player to get the default topper
     * @return the default topper
     */
    public static @NotNull IslandTopper getDefault(Player player){
        for(IslandTopper islandTopper : StartupUtils.islandTopperList){
            if(islandTopper.getField(FieldsType.RARITY, player) == RarityType.NONE){
                return islandTopper;
            }
        }
        // This will never return null!
        return new DummyTopper();
    }

}

