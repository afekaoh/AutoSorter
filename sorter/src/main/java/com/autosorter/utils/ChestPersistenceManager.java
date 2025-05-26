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

    public void saveChestsAndRouting(Set<SmartChest> inputChests, Set<SmartChest> receiverChests,
            Set<SmartChest> overflowChests, Map<Material, Set<SmartChest>> routingMap) throws IOException {
        YamlConfiguration config = new YamlConfiguration();

        config.set("input-chests", inputChests
                .stream()
                .map(c -> serializeLocation(c.getLocation()))
                .toList());

        config.set("receiver-chests", receiverChests.stream().map(c -> serializeLocation(c.getLocation())).toList());
        config.set("overflow-chests", overflowChests.stream().map(c -> serializeLocation(c.getLocation())).toList());

        Map<String, List<String>> routingSection = new HashMap<>();
        for (var entry : routingMap.entrySet()) {
            Material mat = entry.getKey();
            Set<SmartChest> chests = entry.getValue();
            List<String> locs = chests.stream().map(c -> serializeLocation(c.getLocation())).toList();
            routingSection.put(mat.name(), locs);
        }
        config.set("routing-map", routingSection);

        config.save(this.configFile);
    }

    public SetTransfer loadChestsAndRouting() throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(this.configFile);

        var inputChests = new HashSet<>(
                config.getStringList("input-chests")
                        .stream()
                        .map(s -> deserializeLocation(s))
                        .map(l -> new SmartChest(l.getBlock().getState()))
                        .toList());

        var receiverChests = new HashSet<>(
                config.getStringList("receiver-chests")
                        .stream()
                        .map(s -> deserializeLocation(s))
                        .map(l -> new SmartChest(l.getBlock().getState()))
                        .toList());

        var overflowChests = new HashSet<>(
                config.getStringList("overflow-chests")
                        .stream()
                        .map(s -> deserializeLocation(s))
                        .map(l -> new SmartChest(l.getBlock().getState()))
                        .toList());

        ConfigurationSection routing = config.getConfigurationSection("routing-map");
        Map<Material, Set<SmartChest>> routingMap = new HashMap<>();
        if (routing != null) {
            for (String matKey : routing.getKeys(false)) {
                Material mat = Material.valueOf(matKey);
                Set<SmartChest> chests = new HashSet<>();
                routing.getStringList(matKey)
                        .stream()
                        .map(s -> deserializeLocation(s))
                        .map(l -> new SmartChest(l.getBlock().getState()))
                        .forEach(chests::add);
                routingMap.put(mat, chests);
            }
        }
        return new SetTransfer(inputChests, receiverChests, overflowChests, routingMap);
    }

    public class SetTransfer {
        private final Set<SmartChest> inputChests;
        private final Set<SmartChest> receiverChests;
        private final Set<SmartChest> overflowChests;
        private final Map<Material, Set<SmartChest>> routingMap;

        public SetTransfer(Set<SmartChest> inputChests, Set<SmartChest> receiverChests,
                Set<SmartChest> overflowChests, Map<Material, Set<SmartChest>> routingMap) {
            this.inputChests = inputChests;
            this.receiverChests = receiverChests;
            this.overflowChests = overflowChests;
            this.routingMap = routingMap;
        }

        public Set<SmartChest> getInputChests() {
            return inputChests;
        }

        public Set<SmartChest> getReceiverChests() {
            return receiverChests;
        }

        public Set<SmartChest> getOverflowChests() {
            return overflowChests;
        }

        public Map<Material, Set<SmartChest>> getRoutingMap() {
            return routingMap;
        }

    }
}
