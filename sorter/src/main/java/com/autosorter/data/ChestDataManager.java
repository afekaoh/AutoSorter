package com.autosorter.data;

import com.autosorter.model.ChestType;
import com.autosorter.model.SmartChest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ChestDataManager{

    // Map of Material to Set of Chests that route to them
    private final Map<Material, Set<SmartChest>> routingMap;
    private final Map<SmartChest, Set<ItemStack>> receiverChestsFilterMap;
    private final Set<SmartChest> overflowChests;
    private final Set<SmartChest> inputChests;

    // Snapshots for GUI caching
    private final Map<SmartChest, List<ItemStack>> lastFiltersSnapshot = new HashMap<>();
    private final Map<SmartChest, ChestType> lastTypeSnapshot = new HashMap<>();

    public ChestDataManager(Set<SmartChest> inputChests,
                            Map<SmartChest, Set<ItemStack>> receiverChestsFilterMap, Set<SmartChest> overflowChests){
        this.overflowChests = overflowChests != null ? overflowChests : new HashSet<>();
        this.receiverChestsFilterMap = Objects.requireNonNullElseGet(receiverChestsFilterMap, HashMap::new);
        this.inputChests = inputChests != null ? inputChests : new HashSet<>();
        this.routingMap = new HashMap<>();
        // Initialize routing map with existing receiver chests
        receiverChestsFilterMap.keySet().forEach(this::updateRoutingForChest);

    }

    // --- Chest Type ---
    public void setChestType(SmartChest chest, ChestType type){
        this.removeChest(chest);
        switch(type){
            case INPUT:
                inputChests.add(chest);
                break;
            case RECEIVER:
                receiverChestsFilterMap.computeIfAbsent(chest, c -> new HashSet<>());
                break;
            case OVERFLOW:
                overflowChests.add(chest);
                break;
            case NONE:
                // Do nothing, just clear the type
                break;
        }
    }

    public ChestType getChestType(SmartChest chest){

        if(inputChests.contains(chest)){
            return ChestType.INPUT;
        }
        else if(receiverChestsFilterMap.containsKey(chest)){
            return ChestType.RECEIVER;
        }
        else if(overflowChests.contains(chest)){
            return ChestType.OVERFLOW;
        }
        else
            return ChestType.NONE; // Not found in any set
    }

    // --- List Chests ---
    public Map<Material, Set<SmartChest>> getRouteMap(){
        return this.routingMap;
    }

    public void setFilters(SmartChest chest, List<ItemStack> filters){

        var ourChestsfilterList = receiverChestsFilterMap.get(chest);
        if(ourChestsfilterList == null){
            ourChestsfilterList = new HashSet<>(filters);
        }
        else {
            ourChestsfilterList.clear(); // Clear existing filters
            ourChestsfilterList.addAll(filters); // Add new filters
        }
        // Update the map with the new filters
        receiverChestsFilterMap.put(chest, ourChestsfilterList);
        // Update routing for this chest
        updateRoutingForChest(chest);
    }

    public List<ItemStack> getFilters(SmartChest chest){

        var ourChestsfilterList = receiverChestsFilterMap.get(chest);
        if(ourChestsfilterList == null){
            return new ArrayList<>(); // No filters set
        }

        return new ArrayList<>(ourChestsfilterList); // Return a copy of the filters
    }

    public boolean isNotSorterChest(SmartChest chest){
        return getChestType(chest) == ChestType.NONE;
    }

    public void cacheGuiSnapshot(SmartChest chest){
        lastFiltersSnapshot.put(chest, cloneFilters(getFilters(chest)));
        lastTypeSnapshot.put(chest, getChestType(chest));
    }

    private List<ItemStack> cloneFilters(List<ItemStack> filters){
        return filters.stream()
                      .filter(Objects::nonNull)
                      .map(ItemStack::clone)
                      .toList();
    }

    public void updateRoutingIfChanged(SmartChest chest){

        List<ItemStack> currentFilters = cloneFilters(getFilters(chest));
        List<ItemStack> oldFilters = lastFiltersSnapshot.getOrDefault(chest, List.of());

        ChestType currentType = getChestType(chest);
        ChestType oldType = lastTypeSnapshot.getOrDefault(chest, ChestType.NONE);

        // Case: it was a RECEIVER but no longer is — remove it
        if(oldType == ChestType.RECEIVER && currentType != ChestType.RECEIVER){
            routingMap.values().forEach(set -> set.remove(chest));
            return;
        }

        // Case: now a RECEIVER but wasn't — add it
        if(oldType != ChestType.RECEIVER && currentType == ChestType.RECEIVER){

            updateRoutingForChest(chest);
            return;
        }

        // Case: both are RECEIVER, but filters changed — update it
        if(currentType == ChestType.RECEIVER && !currentFilters.equals(oldFilters)){
            updateRoutingForChest(chest);
        }
        // Otherwise, no update needed
    }

    public void updateRoutingForChest(SmartChest chest){
        routingMap.values().forEach(set -> set.remove(chest));

        if(getChestType(chest) != ChestType.RECEIVER)
            return;

        for(ItemStack filter : getFilters(chest)){
            if(filter == null || filter.getType().isAir())
                continue;

            Material mat = filter.getType();
            if(mat == Material.AIR){
                continue; // Skip air items
            }
            routingMap.computeIfAbsent(mat, k -> new HashSet<>()).add(chest);
        }

    }

    public SmartChest getBestRoutingTargetReciver(ItemStack item){
        var chests = routingMap.get(item.getType());
        if(chests == null || chests.isEmpty()){
            return null; // No chests available for this material
        }
        return chests.stream()
                     .filter(c -> hasEmptySpace(item, c))
                     .min(Comparator.comparing((SmartChest c) -> c.getLocation().getWorld().getName())
                                    .thenComparing(c -> c.getLocation().getBlockY())
                                    .thenComparing(c -> c.getLocation().getBlockX())
                                    .thenComparing(c -> c.getLocation().getBlockZ()))
                     .orElse(null);
    }

    private boolean hasEmptySpace(ItemStack itemToSort, SmartChest c){
        int firstEmpty = c.getInventory().firstEmpty();
        if(firstEmpty >= 0){
            return true; // Chest has space
        }
        var contents = c.getInventory().getContents();
        for(ItemStack item : contents){

            if(item != null && item.isSimilar(itemToSort) && item.getAmount() < item.getMaxStackSize()){
                return true; // Chest has space for this material
            }
        }
        return false; // No space for this material
    }

    public SmartChest getBestRoutingTargetOverFlow(ItemStack itemToSort){
        if(overflowChests.isEmpty()){
            return null; // No overflow chests available
        }
        return overflowChests.stream()
                             .filter(c -> hasEmptySpace(itemToSort, c))
                             .min(Comparator.comparing((SmartChest c) -> c.getLocation().getWorld().getName())
                                            .thenComparing(c -> c.getLocation().getBlockY())
                                            .thenComparing(c -> c.getLocation().getBlockX())
                                            .thenComparing(c -> c.getLocation().getBlockZ()))
                             .orElse(null);
    }

    public Set<SmartChest> getInputChests(){
        return inputChests;
    }

    public Map<SmartChest, Set<ItemStack>> getReceiverChestsFilterMap(){
        return this.receiverChestsFilterMap;
    }

    public Set<SmartChest> getOverflowChests(){
        return overflowChests;
    }

    public void removeChest(SmartChest chest){
        if(this.getChestType(chest) == ChestType.RECEIVER){
            routingMap.values().forEach(set -> set.remove(chest));
        }
        inputChests.remove(chest);
        overflowChests.remove(chest);
        receiverChestsFilterMap.remove(chest);
    }
}