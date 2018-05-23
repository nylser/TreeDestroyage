package net.mineguild.minecraft.treedestroyage.event;

import net.mineguild.minecraft.treedestroyage.TreeDestroyage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BlockPlaceHandler {

  public HashMap<Location<World>, Long> placedBlocks;
  private TreeDestroyage plugin;
  private Task task;
  private boolean save = true;

  public void setSave() {
    save = true;
  }

  public BlockPlaceHandler(TreeDestroyage plugin) {
    this.plugin = plugin;
    this.placedBlocks = new HashMap<>();

    File path = new File(plugin.configDir(), "blockLogs");
    if (!path.exists()) {
      path.mkdir();
    }

    File[] worlds = path.listFiles();
    for (File file : worlds) {
      Optional<World> worldOptional = Sponge.getServer().getWorld(file.getName().replace(".txt", ""));
      if (!worldOptional.isPresent()) {
        continue;
      }
      try {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.lines().filter(Objects::nonNull).forEach(line -> {
          String[] prop = line.split(";");

          long time = Long.valueOf(prop[0]);
          int x = Integer.valueOf(prop[1]);
          int y = Integer.valueOf(prop[2]);
          int z = Integer.valueOf(prop[3]);

          Location<World> loc = new Location<>(worldOptional.get(), x, y, z);
          //only load if the block exists
          if (loc.getBlockType() == BlockTypes.LOG || loc.getBlockType() == BlockTypes.LOG2) {
            placedBlocks.put(loc, time);
          }
        });
        reader.close();
        save(worldOptional.get().getName());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    //save
    task = Sponge.getScheduler().createAsyncExecutor(plugin).scheduleAtFixedRate(() -> {
      if (!save) {
        return;
      }
      Sponge.getServer().getWorlds().forEach(w -> save(w.getName()));
      save = false;
    }, 1, 1, TimeUnit.MINUTES).getTask();
  }

  private void save(String world) {
    try {
      File path = new File(plugin.configDir(), "blockLogs");
      File file = new File(path, world + ".txt");
      if (!file.exists()) {
        file.createNewFile();
      }

      PrintWriter pw = new PrintWriter(file);
      for (Location<World> loc : placedBlocks.keySet().stream()
          .filter(l -> l.getExtent().getName().equals(world)).collect(Collectors.toList())) {
        long now = Calendar.getInstance().getTime().getTime();
        long time = placedBlocks.get(loc);

        long days = TimeUnit.DAYS.convert(now - time, TimeUnit.MILLISECONDS);
        int purgeBlocksTime = plugin.getConfig().getNode("purgeBlocksTime").getInt(-1);
        if (purgeBlocksTime < 0 || days <= purgeBlocksTime) {
          int x = loc.getBlockX();
          int y = loc.getBlockY();
          int z = loc.getBlockZ();

          pw.println(time + ";" + x + ";" + y + ";" + z);
        }
      }
      pw.close();
    } catch (Exception ignored) {
    }
  }

  public void stop() {
    Sponge.getServer().getWorlds().forEach(w -> save(w.getName()));
    this.task.cancel();
  }

  @Listener
  public void handle(ChangeBlockEvent.Place event, @First Player player) {
    if (event.getTransactions().size() > 1) {
      return;
    }
    BlockSnapshot block = event.getTransactions().get(0).getFinal();
    if (block.getState().getType() == BlockTypes.LOG
        || block.getState().getType() == BlockTypes.LOG2) {
      Location<World> loc = event.getTransactions().get(0).getFinal().getLocation().get();
      placedBlocks.put(loc, Calendar.getInstance().getTime().getTime());
      save = true;
    }
  }
}
