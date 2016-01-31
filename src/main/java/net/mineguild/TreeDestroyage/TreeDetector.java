package net.mineguild.TreeDestroyage;

import com.flowpowered.math.vector.Vector3d;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.world.Location;

import java.util.HashSet;
import java.util.Set;

public class TreeDetector {

    private BlockSnapshot startBlock;
    private int maxAmount;
    private ConfigurationNode config;
    private Set<BlockSnapshot> woodSnaps = null;
    private Set<Vector3d> locations = new HashSet<>();
    private Comparable variant;
    private boolean inExtended = false;


    public TreeDetector(BlockSnapshot startBlock, int maxAmount, ConfigurationNode config) {
        this.startBlock = startBlock;
        this.maxAmount = maxAmount;
        this.config = config;
        if (!(startBlock.getState().getType() == BlockTypes.LOG || startBlock.getState().getType() == BlockTypes.LOG2)) {
            throw new RuntimeException("Starting block has to be wood!");

        } else {
            variant = (Comparable) startBlock.getState().getTraitValues().toArray()[1];
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
            if ((startBlock.getState().getTraitValues().toArray()[1]).equals(variant)) {
                inExtended = false;
                woodSnaps.add(startBlock);
                locations.add(startBlock.getLocation().get().getPosition());
                //Set<Location> locationLocal;
                // Checking upwards
                Location nextBlock = startBlock.getLocation().get().add(0, 1, 0);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                // Checking in x+1s
                nextBlock = startBlock.getLocation().get().add(1, 0, 0);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                // Checking in x-1
                nextBlock = startBlock.getLocation().get().add(-1, 0, 0);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                //Checking in z+1
                nextBlock = startBlock.getLocation().get().add(0, 0, 1);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                }
                //Checking in z-1
                nextBlock = startBlock.getLocation().get().add(0, 0, -1);
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
        // Checking in x+1s
        Location nextBlock = startBlock.getLocation().get().add(1, 0, 0);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
        // Checking in x-1
        nextBlock = startBlock.getLocation().get().add(-1, 0, 0);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
        //Checking in z+1
        nextBlock = startBlock.getLocation().get().add(0, 0, 1);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
        //Checking in z-1
        nextBlock = startBlock.getLocation().get().add(0, 0, -1);
        if (!locations.contains(nextBlock.getPosition())) {
            getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
        }
    }
}
