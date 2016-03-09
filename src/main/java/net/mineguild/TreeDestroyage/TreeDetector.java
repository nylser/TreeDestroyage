package net.mineguild.treedestroyage;

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

    public final Vector3d[] DIRECTIONS = {
            Vector3d.UP,
            Vector3d.RIGHT,
            Vector3d.RIGHT.mul(-1),
            Vector3d.FORWARD,
            Vector3d.FORWARD.mul(-1),
            Vector3d.UP.mul(-1)
    };

    private Vector3d lastDirection = Vector3d.ZERO;

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
        if (woodSnaps.size() < config.getNode("maxBlocks").getInt(200) && woodSnaps.size() < maxAmount) {
            if (isWood(startBlock) && (startBlock.getState().get(Keys.TREE_TYPE).get().equals(treeType))) {
                inExtended = false;
                woodSnaps.add(startBlock);
                locations.add(startBlock.getLocation().get().getPosition());
                // Checking all DIRECTIONS
                for (Vector3d dir : DIRECTIONS) {
                    Location nextBlock = startBlock.getLocation().get().add(dir);
                    if (!locations.contains(nextBlock.getPosition())) {
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
        for (Vector3d dir : DIRECTIONS) {
            if (!dir.equals(lastDirection)) { // Don't allow a ONE BLOCK gap between// primitive check..
                Location nextBlock = startBlock.getLocation().get().add(dir);
                if (!locations.contains(nextBlock.getPosition())) {
                    getWoodLocations(nextBlock.getBlock().snapshotFor(nextBlock));
                    lastDirection = dir;
                }
            }
        }
    }
}
