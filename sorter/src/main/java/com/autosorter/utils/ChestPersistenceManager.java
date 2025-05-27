package com.autosorter.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import com.autosorter.AutoSorter;
import com.autosorter.model.SmartChest;

public class ChestPersistenceManager {
    private final static String CONFIG_FILE_NAME = "chest_data.yml";
    private final File configFile;

    public ChestPersistenceManager(AutoSorter plugin) {
        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private Location deserializeLocation(String s) {
        String[] parts = s.split(":");
        World world = Bukkit.getWorld(parts[0]);
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        return new Location(world, x, y, z);
    }

    public void saveChestsAndRouting(Set<SmartChest> inputChests,
            Map<SmartChest, Set<ItemStack>> receiverChestFilterMap,
            Set<SmartChest> overflowChests) throws IOException {
        YamlConfiguration config = new YamlConfiguration();

        config.set("input-chests", inputChests
                .stream()
                .map(c -> serializeLocation(c.getLocation()))
                .toList());

        config.set("overflow-chests", overflowChests.stream().map(c -> serializeLocation(c.getLocation())).toList());

        Map<String, List<String>> receivingSection = new HashMap<>();
        for (var entry : receiverChestFilterMap.entrySet()) {
            String chestLocation = serializeLocation(entry.getKey().getLocation());
            Set<ItemStack> filters = entry.getValue();
            List<String> filtersAsStrings = filters.stream().map(c -> serializeItemStack(c)).toList();
            receivingSection.put(chestLocation, filtersAsStrings);
        }
        config.set("receiving-map", receivingSection);

        config.save(this.configFile);
    }

    private String serializeItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "AIR";
        }
        return item.getType().name() + ":" + item.getAmount();
    }

    private ItemStack deserializeItemStack(String s) {
        if ("AIR".equals(s)) {
            return new ItemStack(Material.AIR);
        }
        String[] parts = s.split(":");
        Material material = Material.valueOf(parts[0]);
        int amount = Integer.parseInt(parts[1]);
        return new ItemStack(material, amount);
    }

    public SetTransfer loadChestsAndRouting() throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(this.configFile);

        var inputChests = new HashSet<>(
                config.getStringList("input-chests")
                        .stream()
                        .map(s -> deserializeLocation(s))
                        .map(l -> SmartChest.from(l.getBlock()).orElseGet(null))
                        .filter(c -> c != null)
                        .toList());

        var overflowChests = new HashSet<>(
                config.getStringList("overflow-chests")
                        .stream()
                        .map(s -> deserializeLocation(s))
                        .map(l -> SmartChest.from(l.getBlock()).orElseGet(null))
                        .filter(c -> c != null)
                        .toList());

        ConfigurationSection receivers = config.getConfigurationSection("receiving-map");
        Map<SmartChest, Set<ItemStack>> receiverChestFilterMap = new HashMap<>();
        if (receivers != null) {
            for (String locationKey : receivers.getKeys(false)) {
                Location loc = deserializeLocation(locationKey);
                // SmartChest chest = new SmartChest(loc.getBlock());
                var optionalChest = SmartChest.from(loc.getBlock());
                if (optionalChest.isEmpty()) {
                    continue; // Skip if not a valid SmartChest
                }
                var chest = optionalChest.get(); // Get the SmartChest instance
                Set<ItemStack> filters = new HashSet<>();
                receivers.getStringList(locationKey)
                        .stream()
                        .map(s -> deserializeItemStack(s))
                        .forEach(filters::add);
                receiverChestFilterMap.put(chest, filters);
            }
        }
        return new SetTransfer(inputChests, receiverChestFilterMap, overflowChests);
    }

    public class SetTransfer {
        private final Set<SmartChest> inputChests;
        private final Map<SmartChest, Set<ItemStack>> receiverChestsFilterMap;
        private final Set<SmartChest> overflowChests;

        public SetTransfer(Set<SmartChest> inputChests, Map<SmartChest, Set<ItemStack>> receiverChestsFilterMap,
                Set<SmartChest> overflowChests) {
            this.inputChests = inputChests;
            this.receiverChestsFilterMap = receiverChestsFilterMap;
            this.overflowChests = overflowChests;
        }

        public Set<SmartChest> getInputChests() {
            return inputChests;
        }

        public Map<SmartChest, Set<ItemStack>> getReceiverChestsFilterMap() {
            return receiverChestsFilterMap;
        }

        public Set<SmartChest> getOverflowChests() {
            return overflowChests;
        }
    }
}
