package net.mineguild.minecraft.treedestroyage;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.TreeType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TreeDetector {

  private final ArrayList<Vector3d> DIRECTIONS = Lists.newArrayList(Vector3d.UP,
      Vector3d.RIGHT,
      Vector3d.RIGHT.mul(-1),
      Vector3d.FORWARD,
      Vector3d.FORWARD.mul(-1));
  private BlockSnapshot startBlock;
  private int maxAmount;
  private ConfigurationNode config;
  private Set<BlockSnapshot> woodSnaps = null;
  private Set<Vector3d> locations = new HashSet<>();
  private TreeType treeType;
  private boolean inExtended = false;
  private Vector3d lastDirection = Vector3d.ZERO;
  private TreeDestroyage plugin;

  public TreeDetector(TreeDestroyage plugin, BlockSnapshot startBlock, int maxAmount,
      ConfigurationNode config) {
    this.plugin = plugin;
    this.startBlock = startBlock;
    this.maxAmount = maxAmount;
    this.config = config;
    if (config.getNode("breakDownwards").getBoolean()) {
      Vector3d downwards = Vector3d.UP.mul(-1);
      DIRECTIONS.add(downwards);
    }
    if (!isWood(startBlock)) {
      throw new RuntimeException("Starting block has to be wood!");
    } else {
      treeType = startBlock.getState().get(Keys.TREE_TYPE).get();
    }
  }

  public static boolean isWood(BlockSnapshot loc) {
    return loc.getState().getType() == BlockTypes.LOG
        || loc.getState().getType() == BlockTypes.LOG2;
  }

  public Set<BlockSnapshot> getWoodLocations() {
    if (woodSnaps == null) {
      woodSnaps = new HashSet<>();
      getWoodLocations(startBlock);
    }
    return woodSnaps;
  }

  private void getWoodLocations(BlockSnapshot startBlock) {
    if (woodSnaps.size() < config.getNode("maxBlocks").getInt(200)
        && woodSnaps.size() < maxAmount) {
      if (isWood(startBlock) && (startBlock.getState().get(Keys.TREE_TYPE).get()
          .equals(treeType))) {
        inExtended = false;
        woodSnaps.add(startBlock);
        locations.add(startBlock.getLocation().get().getPosition());
        // Checking all DIRECTIONS
        for (Vector3d dir : DIRECTIONS) {
          Location<World> nextBlock = startBlock.getLocation().get().add(dir);
          if (!locations.contains(nextBlock.getPosition()) && !plugin
              .getBlockPlaceHandler().placedBlocks.containsKey(nextBlock)) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
            lastDirection = dir;
          }
        }
      } else {
        if (!inExtended) {
          extendedCheck(startBlock);
        }
      }
    }
  }

  private void extendedCheck(BlockSnapshot startBlock) {
    inExtended = true;
    DIRECTIONS.stream().filter(dir -> !dir.equals(lastDirection))
        .forEach(dir -> { // Don't allow a ONE BLOCK gap between. primitive check..
          Location<World> nextBlock = startBlock.getLocation().get().add(dir);
          if (!locations.contains(nextBlock.getPosition()) && !plugin
              .getBlockPlaceHandler().placedBlocks.containsKey(nextBlock)) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
            lastDirection = dir;
          }
        });
  }
}
