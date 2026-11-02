package dev.myitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** Читает items.yml и создаёт ItemStack по id. */
public final class ItemRegistry {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final MyItemsPlugin plugin;
    private final NamespacedKey idKey;
    private final Map<String, CustomItem> items = new LinkedHashMap<>();

    public ItemRegistry(MyItemsPlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "custom_id");
    }

    public void load() {
        items.clear();
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("items");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) continue;
            try {
                Material material = Material.valueOf(s.getString("base_material", "PAPER").toUpperCase());
                NamespacedKey model = NamespacedKey.fromString(s.getString("model", "myitems:" + id));
                if (model == null) {
                    throw new IllegalArgumentException("неверный ключ model");
                }
                items.put(id, new CustomItem(
                        id,
                        material,
                        model,
                        s.getString("display_name", id),
                        s.getStringList("lore")
                ));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Не удалось загрузить предмет '" + id + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info("Загружено предметов: " + items.size());
    }

    public ItemStack create(String id, int amount) {
        CustomItem def = items.get(id);
        if (def == null) {
            return null;
        }
        ItemStack stack = new ItemStack(def.material(), amount);
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(MM.deserialize(def.displayName()).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : def.lore()) {
            lore.add(MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        // главное: модель берётся из ресурспака по этому ключу (1.21.2+)
        meta.setItemModel(def.model());
        // метка, по которой плагин потом узнаёт свой предмет
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);

        stack.setItemMeta(meta);
        return stack;
    }

    /** Возвращает id кастомного предмета или null, если это обычный предмет. */
    public String getId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public Iterable<String> ids() {
        return items.keySet();
    }

    public record CustomItem(String id, Material material, NamespacedKey model,
                             String displayName, List<String> lore) {
    }
}
