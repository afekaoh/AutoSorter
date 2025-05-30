package com.autosorter.model;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.autosorter.AutoSorter;
import com.autosorter.data.ChestDataManager;

import java.util.*;

public class RouterManager{

    private final AutoSorter plugin;
    private final ChestDataManager dataManager;

    // Prevent routing duplicates
    private final Set<Location> activeRoutingTasks = new HashSet<>();

    public RouterManager(AutoSorter plugin, ChestDataManager dataManager){
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void startRoutingTaskIfNeeded(SmartChest chest){
        Location loc = chest.getLocation();
        if(activeRoutingTasks.contains(loc)){
            return;
        }

        activeRoutingTasks.add(loc);
        new BukkitRunnable(){
            @Override
            public void run(){
                Inventory inv = chest.getInventory();
                boolean routedSomething = false;

                for(ItemStack item : inv.getContents()){
                    if(item == null || item.getType().isAir())
                        continue;

                    // Try to route 1 item at a time
                    ItemStack single = item.clone();
                    single.setAmount(1);
                    boolean routed = tryRouteItemToReceiver(single);
                    if(routed){
                        item.setAmount(item.getAmount() - 1);
                        routedSomething = true;
                        break; // only route one item per tick to avoid spikes
                    }
                    else {
                        // If we can't route, we should move it to overflow or mark it for manual handling
                        boolean overflowed = tryRouteItemToOverFlow(single);
                        if(overflowed){
                            item.setAmount(item.getAmount() - 1);
                            routedSomething = true;
                            break; // only route one item per tick to avoid spikes
                        }
                    }
                }

                // Stop if inventory is empty or nothing routed
                if(inv.isEmpty() || !routedSomething){
                    activeRoutingTasks.remove(loc);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // adjust interval if needed
    }

    public boolean tryRouteItemToReceiver(ItemStack item){

        return tryRouteItemToTarget(item, dataManager.getBestRoutingTargetReceiver(item));
    }

    public boolean tryRouteItemToOverFlow(ItemStack item){
        return tryRouteItemToTarget(item, dataManager.getBestRoutingTargetOverFlow(item));
    }

    public boolean tryRouteItemToTarget(ItemStack item, SmartChest target){

        if(target == null){
            return false;
        }

        Inventory targetInv = target.getInventory();
        HashMap<Integer, ItemStack> leftover = targetInv.addItem(item.clone());
        return leftover.isEmpty(); // item successfully routed
    }
}
