package net.mineguild.TreeDestroyage.event;


import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
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
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
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
        if (!firedEvents.contains(breakEvent) && breakEvent.getCause().containsType(Player.class) && getConfig().getNode("enabled").getBoolean(true) && !breakEvent.isCancelled() && breakEvent.getTransactions().size() == 1 &&
                TreeDetector.isWood(breakEvent.getTransactions().get(0).getOriginal())) {
            Player cause = breakEvent.getCause().first(Player.class).get();
            Optional<ItemStack> inHand = cause.getItemInHand();
            List<String> items = getConfig().getNode("items").getList(TypeToken.of(String.class));
            if (inHand.isPresent() && items.contains(inHand.get().getItem().getName()) && cause.hasPermission("TreeDestroyage.destroy")) {
                ItemStack item = inHand.get();
                int maxAmount = 0; // 0 for unlimited
                if (item.get(Keys.ITEM_DURABILITY).isPresent()) {
                    int durability = item.get(Keys.ITEM_DURABILITY).get();
                    maxAmount = durability + 2; // Because durability=0 is the last hit
                    if (durability == 0) {
                        plugin.getLogger().info("Cancelling here, durability is 0");
                        return;
                    }
                }
                TreeDetector dec = new TreeDetector(breakEvent.getTransactions().get(0).getOriginal(), maxAmount, getConfig());
                List<Transaction<BlockSnapshot>> transactions = new ArrayList<>(dec.getWoodLocations().size());
                dec.getWoodLocations().stream().filter(snapshot -> !snapshot.equals(breakEvent.getTransactions().get(0).getOriginal())).forEach(blockSnapshot -> {
                    BlockState newState = BlockTypes.AIR.getDefaultState();
                    BlockSnapshot newSnapshot = blockSnapshot.withState(newState).withLocation(new Location<World>(cause.getWorld(), blockSnapshot.getPosition()));
                    Transaction<BlockSnapshot> t = new Transaction<>(blockSnapshot, newSnapshot);
                    transactions.add(t);
                });
                transactions.forEach(blockSnapshotTransaction -> {
                    ChangeBlockEvent.Break event = SpongeEventFactory.createChangeBlockEventBreak(Cause.of(cause),
                            cause.getWorld(), Lists.newArrayList(blockSnapshotTransaction));
                    firedEvents.add(event);
                    if (!getGame().getEventManager().post(event)) {
                        if (cause.getGameModeData().get(Keys.GAME_MODE).get() != GameModes.CREATIVE) {
                            BlockState state = blockSnapshotTransaction.getOriginal().getState();
                            ItemStack.Builder builder = getGame().getRegistry().createBuilder(ItemStack.Builder.class);
                            ItemStack itemStack = builder.fromBlockState(state).build();
                            Entity entity = cause.getWorld().createEntity(EntityTypes.ITEM, blockSnapshotTransaction.getOriginal().getPosition()).get(); // 'cause' is the player
                            entity.offer(Keys.REPRESENTED_ITEM, itemStack.createSnapshot());
                            cause.getWorld().spawnEntity(entity, Cause.of(cause));
                            if (item.supports(Keys.ITEM_DURABILITY)) {
                                if(item.get(Keys.ITEM_DURABILITY).get() == 0){
                                    cause.setItemInHand(null);
                                } else {
                                    plugin.getLogger().info(item.offer(Keys.ITEM_DURABILITY, item.get(Keys.ITEM_DURABILITY).get() - 1).toString());

                                    cause.setItemInHand(item);
                                }
                            }
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

    private ConfigurationNode getConfig() {
        return plugin.getConfig();
    }

    private Game getGame() {
        return plugin.getGame();
    }
}