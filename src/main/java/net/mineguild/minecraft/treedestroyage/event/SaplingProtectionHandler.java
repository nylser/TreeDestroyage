package net.mineguild.minecraft.treedestroyage.event;

import static java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import net.mineguild.minecraft.treedestroyage.TreeDestroyage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SaplingProtectionHandler {

    @Inject
    private TreeDestroyage plugin;
    private HashMap<Location<World>, Long> protectedSaplings;

    public SaplingProtectionHandler() {
        protectedSaplings = Maps.newHashMap();
    }

    public void activate() {
        Sponge.getScheduler().createTaskBuilder().interval(10, TimeUnit.SECONDS).execute(() -> {
            List<Location> toRemove = Lists.newArrayListWithExpectedSize(protectedSaplings.size());
            toRemove.addAll(protectedSaplings.entrySet().stream().filter(sapling ->
                    (System.currentTimeMillis() - (long) sapling.getValue()) / 1000 >= plugin.getConfig().getNode("saplingProtection").getInt())
                    .map(Entry::getKey).collect(Collectors.toList()));
            toRemove.forEach(location -> protectedSaplings.remove(location));
        }).submit(plugin);
    }

    @Listener
    public void handle(ChangeBlockEvent.Break breakEvent, @First Player p) {
        if (breakEvent.getTransactions().get(0).getOriginal().getState().getType() == BlockTypes.SAPLING
                && protectedSaplings.containsKey(breakEvent.getTransactions().get(0).getDefault().getLocation().get())) {
            breakEvent.setCancelled(true);
            p.sendMessage(Text.of("This sapling is still protected!"));
        }
    }

    public void addProtectedSapling(Location<World> location) {
        protectedSaplings.put(location, System.currentTimeMillis());
    }


}
