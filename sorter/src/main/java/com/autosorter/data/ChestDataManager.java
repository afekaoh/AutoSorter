package com.autosorter.data;

import com.autosorter.AutoSorter;
import com.autosorter.model.ChestType;
import com.autosorter.model.SmartChest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public class ChestDataManager {

    private final AutoSorter plugin;

    // Persistent Data Keys
    public static NamespacedKey CHEST_TYPE_KEY;
    public static NamespacedKey CHEST_FILTERS_KEY;
    // public static NamespacedKey CHEST_PRIORITY_KEY;
    // Map of Material to Set of Chests that route to them
    private final Map<Material, Set<SmartChest>> routingMap;
    private final Set<SmartChest> overflowChests;
    private final Set<SmartChest> receiverChests;
    private final Set<SmartChest> inputChests;

    // Snapshots for GUI caching
    private final Map<SmartChest, List<ItemStack>> lastFiltersSnapshot = new HashMap<>();
    private final Map<SmartChest, ChestType> lastTypeSnapshot = new HashMap<>();

    public ChestDataManager(AutoSorter plugin, Set<SmartChest> inputChests,
            Set<SmartChest> receiverChests, Set<SmartChest> overflowChests, Map<Material, Set<SmartChest>> routingMap) {
        this.plugin = plugin;
        CHEST_TYPE_KEY = new NamespacedKey(plugin, "chest_type");
        CHEST_FILTERS_KEY = new NamespacedKey(plugin, "chest_filters");
        // CHEST_PRIORITY_KEY = new NamespacedKey(plugin, "chest_priority");
        this.overflowChests = overflowChests != null ? overflowChests : new HashSet<>();
        this.receiverChests = receiverChests != null ? receiverChests : new HashSet<>();
        this.inputChests = inputChests != null ? inputChests : new HashSet<>();
        this.routingMap = routingMap != null ? routingMap : new HashMap<>();
    }

    // --- Chest Type ---
    public void setChestType(SmartChest chest, ChestType type) {
        chest.setToPersistentDataContainerString(CHEST_TYPE_KEY, PersistentDataType.STRING, type.name());
        inputChests.remove(chest);
        overflowChests.remove(chest);
        receiverChests.remove(chest);
        switch (type) {
            case INPUT:
                inputChests.add(chest);
                break;
            case RECEIVER:
                receiverChests.add(chest);
                updateRoutingForChest(chest);
                break;
            case OVERFLOW:
                overflowChests.add(chest);
                break;
            case NONE:
                // Do nothing, just clear the type
                break;
        }
    }

    public List<Set<SmartChest>> getAllChestsSets() {
        return List.of(inputChests, receiverChests, overflowChests);
    }

    public ChestType getChestType(SmartChest chest) {

        var typeName = chest.getFromPersistentDataContainerString(CHEST_TYPE_KEY, PersistentDataType.STRING);
        return (typeName != null) ? ChestType.fromString(typeName) : ChestType.NONE;
    }

    // --- List Chests ---
    public Map<Material, Set<SmartChest>> getRoutMap() {
        return this.routingMap;
    }

    // --- Item Filters (Using Base64 Serialization) ---
    public void setFilters(SmartChest chest, List<ItemStack> filters) {
        try {
            ByteArrayOutputStream bio = new ByteArrayOutputStream();
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(bio);
            boos.writeInt(filters.size());
            for (ItemStack item : filters) {
                boos.writeObject(item);
            }
            boos.close();
            String encodedData = Base64.getEncoder().encodeToString(bio.toByteArray());
            chest.setToPersistentDataContainerString(CHEST_FILTERS_KEY, PersistentDataType.STRING, encodedData);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not serialize item filters for chest at " + chest.getLocation(),
                    e);
        }
    }

    public List<ItemStack> getFilters(SmartChest chest) {
        List<ItemStack> filters = new ArrayList<>();
        String encodedData = chest.getFromPersistentDataContainerString(CHEST_FILTERS_KEY, PersistentDataType.STRING);

        if (encodedData == null || encodedData.isEmpty()) {
            return filters;
        }

        try {
            byte[] rawData = Base64.getDecoder().decode(encodedData);
            ByteArrayInputStream bio = new ByteArrayInputStream(rawData);
            BukkitObjectInputStream bois = new BukkitObjectInputStream(bio);
            int size = bois.readInt();
            for (int i = 0; i < size; i++) {
                filters.add((ItemStack) bois.readObject());
            }
            bois.close();
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not deserialize item filters for chest at " + chest.getLocation(), e);
        }
        return filters;
    }

    public boolean isSorterChest(SmartChest chest) {
        return getChestType(chest) != ChestType.NONE;
    }

    public void cacheGuiSnapshot(SmartChest chest) {
        lastFiltersSnapshot.put(chest, cloneFilters(getFilters(chest)));
        lastTypeSnapshot.put(chest, getChestType(chest));
    }

    private List<ItemStack> cloneFilters(List<ItemStack> filters) {
        return filters.stream()
                .filter(Objects::nonNull)
                .map(ItemStack::clone)
                .toList();
    }

    public void updateRoutingIfChanged(SmartChest chest) {
        Location loc = chest.getLocation();

        List<ItemStack> currentFilters = cloneFilters(getFilters(chest));
        List<ItemStack> oldFilters = lastFiltersSnapshot.getOrDefault(loc, List.of());

        ChestType currentType = getChestType(chest);
        ChestType oldType = lastTypeSnapshot.getOrDefault(loc, ChestType.NONE);

        // Case: it was a RECEIVER but no longer is — remove it
        if (oldType == ChestType.RECEIVER && currentType != ChestType.RECEIVER) {
            plugin.getLogger().info("[AutoSorter] Removing routing for " + loc + " as it is no longer a RECEIVER.");
            routingMap.values().forEach(set -> set.remove(chest));
            return;
        }

        // Case: now a RECEIVER but wasn't — add it
        if (oldType != ChestType.RECEIVER && currentType == ChestType.RECEIVER) {
            plugin.getLogger()
                    .info("[AutoSorter] Adding routing for " + loc + " as it changed from " + oldType
                            + " to RECEIVER.");
            updateRoutingForChest(chest);
            return;
        }

        // Case: both are RECEIVER, but filters changed — update it
        if (currentType == ChestType.RECEIVER && !currentFilters.equals(oldFilters)) {
            plugin.getLogger().info("[AutoSorter] Updating routing for " + loc + " as filters changed.");
            updateRoutingForChest(chest);
        }
        // Otherwise, no update needed
    }

    public void updateRoutingForChest(SmartChest chest) {
        routingMap.values().forEach(list -> list.remove(chest));

        if (getChestType(chest) != ChestType.RECEIVER)
            return;

        for (ItemStack filter : getFilters(chest)) {
            if (filter == null || filter.getType().isAir())
                continue;

            Material mat = filter.getType();
            if (mat == Material.AIR) {
                continue; // Skip air items
            }
            routingMap.computeIfAbsent(mat, k -> new HashSet<>()).add(chest);
        }
        Bukkit.getLogger().info(
                "[AutoSorter] Routing updated for " + chest.getLocation() + " with filters: " + getFilters(chest));
    }

    public SmartChest getBestRoutingTargetReciver(Material material) {
        var chests = routingMap.get(material);
        if (chests == null || chests.isEmpty()) {
            return null; // No chests available for this material
        }
        return chests.stream()
                .filter(c -> c.getInventory().firstEmpty() != -1)
                .sorted(Comparator.comparing((SmartChest c) -> c.getLocation().getWorld().getName())
                        .thenComparing(c -> c.getLocation().getBlockY())
                        .thenComparing(c -> c.getLocation().getBlockX())
                        .thenComparing(c -> c.getLocation().getBlockZ()))
                .findFirst().orElse(null);
    }

    public SmartChest getBestRoutingTargetOverFlow(Material material) {
        if (overflowChests.isEmpty()) {
            return null; // No overflow chests available
        }
        return overflowChests.stream()
                .filter(c -> c.getInventory().firstEmpty() != -1)
                .sorted(Comparator.comparing((SmartChest c) -> c.getLocation().getWorld().getName())
                        .thenComparing(c -> c.getLocation().getBlockY())
                        .thenComparing(c -> c.getLocation().getBlockX())
                        .thenComparing(c -> c.getLocation().getBlockZ()))
                .findFirst().orElse(null);
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