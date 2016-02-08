package net.mineguild.TreeDestroyage;

import com.flowpowered.math.vector.Vector3d;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.TreeType;
import org.spongepowered.api.world.Location;

import java.util.HashSet;
import java.util.Set;

public class TreeDetector {

    private BlockSnapshot startBlock;
    private int maxAmount;
    private ConfigurationNode config;
    private Set<BlockSnapshot> woodSnaps = null;
    private Set<Vector3d> locations = new HashSet<>();
    private TreeType treeType;
    private boolean inExtended = false;


    public TreeDetector(BlockSnapshot startBlock, int maxAmount, ConfigurationNode config) {
        this.startBlock = startBlock;
        this.maxAmount = maxAmount;
        this.config = config;
        if (!isWood(startBlock)) {
            throw new RuntimeException("Starting block has to be wood!");
        } else {
            treeType = startBlock.getState().get(Keys.TREE_TYPE).get();
        }
    }

    public static boolean isWood(BlockSnapshot loc) {
        return loc.getState().getType() == BlockTypes.LOG || loc.getState().getType() == BlockTypes.LOG2;
    }

    public Set<BlockSnapshot> getWoodLocations() {
        if (woodSnaps == null) {
            woodSnaps = new HashSet<>();
            getWoodLocations(startBlock);
        }
        return woodSnaps;
    }

    private void getWoodLocations(BlockSnapshot startBlock) {
        if (woodSnaps.size() < config.getNode("maxBlocks").getInt(200) && woodSnaps.size() < maxAmount && isWood(startBlock)) {
            if ((startBlock.getState().get(Keys.TREE_TYPE).get().equals(treeType))) {
                inExtended = false;
                woodSnaps.add(startBlock);
                locations.add(startBlock.getLocation().get().getPosition());
                //Set<Location> locationLocal;
                // Checking upwards
                Location nextBlock = startBlock.getLocation().get().add(Vector3d.UP);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                // Checking right
                nextBlock = startBlock.getLocation().get().add(Vector3d.RIGHT);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                // Checking left
                nextBlock = startBlock.getLocation().get().sub(Vector3d.RIGHT);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                //Checking forwards
                nextBlock = startBlock.getLocation().get().add(Vector3d.FORWARD);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                //Checking backwards
                nextBlock = startBlock.getLocation().get().sub(Vector3d.FORWARD);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                //Checking downwards
                nextBlock = startBlock.getLocation().get().sub(Vector3d.UP);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
            }
        } else {
            if (!inExtended) {
                extendedCheck(startBlock);
            }
        }
    }

    private void extendedCheck(BlockSnapshot startBlock) {
        inExtended = true;
        // Checking upwards
        Location nextBlock = startBlock.getLocation().get().add(Vector3d.UP);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
        // Checking right
        nextBlock = startBlock.getLocation().get().add(Vector3d.RIGHT);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
        // Checking left
        nextBlock = startBlock.getLocation().get().sub(Vector3d.RIGHT);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
        //Checking forwards
        nextBlock = startBlock.getLocation().get().add(Vector3d.FORWARD);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
        //Checking backwards
        nextBlock = startBlock.getLocation().get().sub(Vector3d.FORWARD);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
        //Checking downwards
        nextBlock = startBlock.getLocation().get().sub(Vector3d.UP);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
    }
}
