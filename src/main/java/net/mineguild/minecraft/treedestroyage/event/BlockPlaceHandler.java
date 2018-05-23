package net.mineguild.minecraft.treedestroyage.event;

import java.sql.SQLException;
import net.mineguild.minecraft.treedestroyage.TreeDestroyage;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;


public class BlockPlaceHandler {

  private TreeDestroyage plugin;

  public BlockPlaceHandler(TreeDestroyage plugin) {
    this.plugin = plugin;
  }


  @Listener
  public void handle(ChangeBlockEvent.Place event, @First Player player) {
    if (plugin.getBlockLogger().isEnabled()) {
      if (event.getTransactions().size() > 1) {
        return;
      }
      BlockSnapshot block = event.getTransactions().get(0).getFinal();
      if (block.getState().getType() == BlockTypes.LOG
          || block.getState().getType() == BlockTypes.LOG2) {
        Location<World> loc = event.getTransactions().get(0).getFinal().getLocation().get();
        try {
          plugin.getBlockLogger().insertBlock(block, -1);
        } catch (SQLException e) {
          plugin.getLogger().error("Unable to log block", e);
        }
      }
    }
  }

}
