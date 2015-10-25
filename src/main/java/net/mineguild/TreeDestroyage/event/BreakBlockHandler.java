package net.mineguild.TreeDestroyage.event;


import net.mineguild.TreeDestroyage.TreeDestroyage;
import net.mineguild.TreeDestroyage.TreeDetector;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BreakBlockHandler implements EventListener<ChangeBlockEvent.Break> {


    private TreeDestroyage plugin;

    public BreakBlockHandler(TreeDestroyage plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(ChangeBlockEvent.Break breakEvent) throws Exception {
        if (breakEvent.getCause().any(Player.class) && getConfig().getNode("enabled").getBoolean(true) && !breakEvent.isCancelled() && breakEvent.getTransactions().size() == 1 &&
                TreeDetector.isWood(breakEvent.getTransactions().get(0).getOriginal())) {
            Player cause = breakEvent.getCause().first(Player.class).get();
            Optional<ItemStack> inHand = cause.getItemInHand();
            if (inHand.isPresent() && inHand.get().getItem().getName().equals(getConfig().getNode("item").getString()) && cause.hasPermission("TreeDestroyage.destroy")) {
                TreeDetector dec = new TreeDetector(breakEvent.getTransactions().get(0).getOriginal(), getConfig());
                List<Transaction<BlockSnapshot>> transactions = new ArrayList<>(dec.getWoodLocations().size());
                dec.getWoodLocations().forEach(blockSnapshot -> {
                    if (!blockSnapshot.equals(breakEvent.getTransactions().get(0).getOriginal())) {
                        BlockState newState = BlockTypes.AIR.getDefaultState();
                        BlockSnapshot newSnapshot = blockSnapshot.withState(newState).withLocation(new Location<World>(cause.getWorld(), blockSnapshot.getPosition()));
                        Transaction<BlockSnapshot> t = new Transaction<>(blockSnapshot, newSnapshot);
                        transactions.add(t);
                    }
                });
                transactions.forEach(blockSnapshotTransaction -> {

                    if (cause.getGameModeData().get(Keys.GAME_MODE).get() != GameModes.CREATIVE) {
                        String id = "";
                        String damage = "";
                        BlockState state = blockSnapshotTransaction.getOriginal().getState();
                        Object trait = state.getTraitValues().toArray()[1];
                        Location loc = blockSnapshotTransaction.getOriginal().getLocation().get();

                        if (state.getType() == BlockTypes.LOG) {
                            id = "minecraft:log";
                            if (trait.toString().equalsIgnoreCase("oak")) {
                                damage = "0";
                            } else if (trait.toString().equalsIgnoreCase("spruce")) {
                                damage = "1";
                            } else if (trait.toString().equalsIgnoreCase("birch")) {
                                damage = "2";
                            } else if (trait.toString().equalsIgnoreCase("jungle")) {
                                damage = "3";
                            }
                        } else if (state.getType() == BlockTypes.LOG2) {
                            id = "minecraft:log2";
                            if (trait.toString().equalsIgnoreCase("acacia")) {
                                damage = "0";
                            } else if (trait.toString().equalsIgnoreCase("dark_oak")) {
                                damage = "1";
                            }
                        }

                        getGame().getCommandDispatcher().process(getGame().getServer().getConsole()
                                , String.format("summon Item %d %d %d {Item:{id:%s, Damage:%s,Count:%d}}",
                                loc.getBlockPosition().getX(), loc.getBlockPosition().getY(), loc.getBlockPosition().getZ(), id, damage, 1));
                    }
                    blockSnapshotTransaction.getFinal().restore(true, true);

                });
            }
        }
    }

    private ConfigurationNode getConfig(){
        return plugin.getConfig();
    }

    private Game getGame(){
        return plugin.getGame();
    }
}