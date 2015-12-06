package net.mineguild.TreeDestroyage.event;


import com.google.common.collect.Lists;
import net.mineguild.TreeDestroyage.TreeDestroyage;
import net.mineguild.TreeDestroyage.TreeDetector;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.TreeTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BreakBlockHandler {


    private TreeDestroyage plugin;

    private List<ChangeBlockEvent.Break> firedEvents = Lists.newArrayList();

    public BreakBlockHandler(TreeDestroyage plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void handle(ChangeBlockEvent.Break breakEvent) throws Exception {
        if (!firedEvents.contains(breakEvent) && breakEvent.getCause().any(Player.class) && getConfig().getNode("enabled").getBoolean(true) && !breakEvent.isCancelled() && breakEvent.getTransactions().size() == 1 &&
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
                    ChangeBlockEvent.Break event = SpongeEventFactory.createChangeBlockEventBreak(getGame(), Cause.of(cause),
                            cause.getWorld(), Lists.newArrayList(blockSnapshotTransaction));
                    firedEvents.add(event);
                    if (!getGame().getEventManager().post(event)) {
                        if (cause.getGameModeData().get(Keys.GAME_MODE).get() != GameModes.CREATIVE) {
                            BlockState state = blockSnapshotTransaction.getOriginal().getState();
                            Object trait = state.getTraitValues().toArray()[1];
                            ItemStack.Builder builder = getGame().getRegistry().createBuilder(ItemStack.Builder.class);
                            ItemStack itemStack = null;
                            if (state.getType() == BlockTypes.LOG) {
                                itemStack = builder.itemType(ItemTypes.LOG).build();
                                if (trait.toString().equalsIgnoreCase("oak")) {
                                    itemStack.offer(Keys.TREE_TYPE, TreeTypes.OAK);
                                } else if (trait.toString().equalsIgnoreCase("spruce")) {
                                    itemStack.offer(Keys.TREE_TYPE, TreeTypes.SPRUCE);
                                } else if (trait.toString().equalsIgnoreCase("birch")) {
                                    itemStack.offer(Keys.TREE_TYPE, TreeTypes.BIRCH);
                                } else if (trait.toString().equalsIgnoreCase("jungle")) {
                                    itemStack.offer(Keys.TREE_TYPE, TreeTypes.JUNGLE);
                                }
                            } else if (state.getType() == BlockTypes.LOG2) {
                                itemStack = builder.itemType(ItemTypes.LOG2).build();
                                if (trait.toString().equalsIgnoreCase("acacia")) {
                                    itemStack.offer(Keys.TREE_TYPE, TreeTypes.ACACIA);
                                } else if (trait.toString().equalsIgnoreCase("dark_oak")) {
                                    itemStack.offer(Keys.TREE_TYPE, TreeTypes.DARK_OAK);
                                }
                            }
                            Entity entity = cause.getWorld().createEntity(EntityTypes.ITEM, blockSnapshotTransaction.getOriginal().getPosition()).get(); // 'cause' is the player
                            entity.offer(Keys.REPRESENTED_ITEM, itemStack.createSnapshot());
                            cause.getWorld().spawnEntity(entity, Cause.of(plugin));

                        }
                        blockSnapshotTransaction.getFinal().restore(true, true);
                    } else {
                        // Event got canceled
                        System.out.println("Event canceled.");
                    }

                });
                firedEvents.clear();
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