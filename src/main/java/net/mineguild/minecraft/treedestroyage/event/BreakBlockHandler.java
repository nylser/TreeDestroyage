package net.mineguild.minecraft.treedestroyage.event;


import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import net.mineguild.minecraft.treedestroyage.TreeDestroyage;
import net.mineguild.minecraft.treedestroyage.TreeDetector;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.type.TreeType;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BreakBlockHandler {

  @Inject
  private TreeDestroyage plugin;

  @Inject
  private SaplingProtectionHandler protHandler;

  @Inject
  private Game game;

  private List<ChangeBlockEvent.Break> firedEvents = Lists.newArrayList();

  @Listener
  public void handle(ChangeBlockEvent.Break breakEvent, @First Player player) throws Exception {
    if (breakEvent.getTransactions().size() > 1) {
      return;
    }
    Transaction<BlockSnapshot> transaction = breakEvent.getTransactions().get(0);
    boolean isBase = !TreeDetector
        .isWood(transaction.getOriginal().getLocation().get().sub(Vector3d.UP).createSnapshot());
    if (getConfig().getNode("baseOnly").getBoolean()) {
        if (!isBase) {
            return;
        }
    }
    if (!firedEvents.contains(breakEvent) && getConfig().getNode("enabled").getBoolean(true)
        && !breakEvent.isCancelled() &&
        TreeDetector.isWood(transaction.getOriginal())) {
      TreeType treeType = transaction.getOriginal().getState().get(Keys.TREE_TYPE).get();
      Optional<ItemStack> inHand = player.getItemInHand(HandTypes.MAIN_HAND);
      List<String> items = getConfig().getNode("items").getList(TypeToken.of(String.class));
      final boolean consumeDurability = getConfig().getNode("consumeDurability").getBoolean();
      if (inHand.isPresent() && items.contains(inHand.get().getType().getName()) && player
          .hasPermission("treedestroyage.destroy")) {
        ItemStack item = inHand.get();
        int maxAmount = getConfig().getNode("maxBlocks").getInt();
        if (consumeDurability && item.get(Keys.ITEM_DURABILITY).isPresent()) {
          int durability = item.get(Keys.ITEM_DURABILITY).get();
          maxAmount = durability + 2; // Because durability=0 is the last hit
          if (durability == 0) {
            plugin.getLogger().debug("Cancelling here, durability is 0");
            return;
          }
        }
        TreeDetector dec = new TreeDetector(breakEvent.getTransactions().get(0).getOriginal(),
            maxAmount, getConfig());
        Vector3d playerPos = player.getLocation().getPosition();
        List<Transaction<BlockSnapshot>> transactions = new ArrayList<>(
            dec.getWoodLocations().size());
        dec.getWoodLocations().stream().sorted((snap1, snap2) -> {
          double distance1 = snap1.getPosition().toDouble().distance(playerPos);
          double distance2 = snap2.getPosition().toDouble().distance(playerPos);
          if (distance1 < distance2) {
            return -1;
          } else if (distance1 > distance2) {
            return 1;
          } else {
            return 0;
          }
        }).filter(snapshot -> !snapshot.equals(breakEvent.getTransactions().get(0).getOriginal()))
            .forEach(blockSnapshot -> {
              BlockState newState = BlockTypes.AIR.getDefaultState();
              BlockSnapshot newSnapshot = blockSnapshot.withState(newState)
                  .withLocation(new Location<>(player.getWorld(), blockSnapshot.getPosition()));
              Transaction<BlockSnapshot> t = new Transaction<>(blockSnapshot, newSnapshot);
              transactions.add(t);
            });
        transactions.forEach(blockSnapshotTransaction -> {
          Sponge.getCauseStackManager().pushCause(player);
          ChangeBlockEvent.Break event = SpongeEventFactory
              .createChangeBlockEventBreak(Sponge.getCauseStackManager().getCurrentCause(),
                  Collections.singletonList(blockSnapshotTransaction));
          Sponge.getCauseStackManager().popCause();
          firedEvents.add(event);
          if (!getGame().getEventManager().post(event)) {
            if (player.getGameModeData().get(Keys.GAME_MODE).get() != GameModes.CREATIVE) {
              BlockState state = blockSnapshotTransaction.getOriginal().getState();
              ItemStack itemStack = ItemStack.builder()
                  .itemType(state.getType().getDefaultState().getType().getItem().get())
                  .add(Keys.TREE_TYPE, state.get(Keys.TREE_TYPE).get()).build();
              Entity entity = player.getWorld().createEntity(EntityTypes.ITEM,
                  blockSnapshotTransaction.getOriginal().getPosition()); // 'cause' is the player
              entity.offer(Keys.REPRESENTED_ITEM, itemStack.createSnapshot());
              player.getWorld().spawnEntity(entity);
              if (consumeDurability && item.supports(Keys.ITEM_DURABILITY)) {
                if (item.get(Keys.ITEM_DURABILITY).get() == 0) {
                  player.getWorld()
                      .playSound(SoundTypes.ENTITY_ITEM_BREAK, player.getLocation().getPosition(),
                          1);
                  player.setItemInHand(HandTypes.MAIN_HAND, null);
                } else {
                  item.offer(Keys.ITEM_DURABILITY, item.get(Keys.ITEM_DURABILITY).get() - 1);
                  player.setItemInHand(HandTypes.MAIN_HAND, item);
                }
              }
            }
            blockSnapshotTransaction.getFinal().restore(true, BlockChangeFlags.ALL);
          } else {
            plugin.getLogger().debug("Event got canceled");
          }

        });
        firedEvents.clear();
        if ((isBase || getConfig().getNode("breakDownwards").getBoolean()) && getConfig()
            .getNode("placeSapling").getBoolean()) {
          placeSapling(player,
              breakEvent.getTransactions().get(0).getOriginal().getLocation().get(), treeType);
        }
      }
    }
  }

  private void placeSapling(Player c, Location<World> treeBlock, TreeType treeType) {
    Location<World> baseBlock = treeBlock.sub(Vector3d.UP);
    // Not yet implemented
    Optional<ItemStackSnapshot> saplingSnapshot = ItemTypes.SAPLING.getTemplate()
        .with(Keys.TREE_TYPE, treeType);
    if (saplingSnapshot.isPresent()) {
      Optional<Set<BlockType>> placeableBlocks = saplingSnapshot.get().get(Keys.PLACEABLE_BLOCKS);
      if (placeableBlocks.isPresent()) {
        if (!placeableBlocks.get().contains(baseBlock.getBlockType())) {
          plugin.getLogger().debug("Canceling placement -- not placeable");
          return;
        }

      } else {
        plugin.getLogger().error("Placeable blocks not present!");
      }
    }
    if (getConfig().getNode("breakDownwards").getBoolean()) {
      baseBlock = findBase(baseBlock); // Find baseBlock if not already found.
      treeBlock = baseBlock.add(Vector3d.UP);
    }

    if (baseBlock.getBlockType() == BlockTypes.DIRT
        || baseBlock.getBlockType() == BlockTypes.GRASS) {
      BlockSnapshot old = treeBlock.getBlock().snapshotFor(treeBlock);
      BlockSnapshot newBL = old.withState(
          BlockState.builder().blockType(BlockTypes.SAPLING).build().with(Keys.TREE_TYPE, treeType)
              .get());
      Transaction<BlockSnapshot> transaction = new Transaction<>(old, newBL);
      List<Transaction<BlockSnapshot>> transactions = Lists.newArrayList();
      transactions.add(transaction);
      Sponge.getCauseStackManager().pushCause(c);
      ChangeBlockEvent.Place event = SpongeEventFactory
          .createChangeBlockEventPlace(Sponge.getCauseStackManager().getCurrentCause(),
              transactions);
      Sponge.getCauseStackManager().popCause();
      if (!Sponge.getEventManager().post(event)) {
        transaction.getFinal().restore(true, BlockChangeFlags.ALL);
        if (getConfig().getNode("saplingProtection").getInt() > 0) {
          protHandler.addProtectedSapling(treeBlock);
        }
      }
    }

  }

  private Location<World> findBase(Location<World> startLocation) {
    while (!(startLocation.getBlockType() == BlockTypes.DIRT
        || startLocation.getBlockType() == BlockTypes.GRASS)) {
      startLocation = startLocation.sub(Vector3d.UP);
    }
    return startLocation;
  }

  private ConfigurationNode getConfig() {
    return plugin.getConfig();
  }

  @Inject
  private Game getGame() {
    return this.game;
  }
}