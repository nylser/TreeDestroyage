package net.mineguild.minecraft.treedestroyage.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.mineguild.minecraft.treedestroyage.TreeDestroyage;

public class SaplingProtectionHandler {
    private TreeDestroyage plugin;
    private HashMap<Location<World>, Long> protectedSaplings;

    public SaplingProtectionHandler(TreeDestroyage plugin) {
        this.plugin = plugin;
        protectedSaplings = Maps.newHashMap();
    }

    public void activate(){
        Sponge.getScheduler().createTaskBuilder().interval(10, TimeUnit.SECONDS).execute(() -> {
            List<Location<World>> toRemove = Lists.newArrayListWithExpectedSize(protectedSaplings.size());
            for (Entry<Location<World>, Long> sapling : protectedSaplings.entrySet()) {
                if((System.currentTimeMillis() - (long)sapling.getValue()) / 1000 >= plugin.getConfig().getNode("saplingProtection").getInt()){
                    toRemove.add(sapling.getKey());
                }
            }
            toRemove.forEach(location -> protectedSaplings.remove(location));
        }).submit(plugin);
    }

    @Listener
    public void handle(ChangeBlockEvent.Break breakEvent) {
        if (breakEvent.getCause().containsType(Player.class) && breakEvent.getTransactions().size() == 1
                && breakEvent.getTransactions().get(0).getOriginal().getState().getType() == BlockTypes.SAPLING
                && protectedSaplings.containsKey(breakEvent.getTransactions().get(0).getDefault().getLocation().get())) {
            breakEvent.setCancelled(true);
            breakEvent.getCause().first(Player.class).ifPresent(player -> player.sendMessage(Text.of("This sapling is still protected!")));
        }
    }

    public void addProtectedSapling(Location<World> location) {
        protectedSaplings.put(location, System.currentTimeMillis());
    }


}
