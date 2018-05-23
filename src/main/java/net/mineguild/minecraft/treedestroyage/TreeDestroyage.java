package net.mineguild.minecraft.treedestroyage;

import static org.spongepowered.api.command.args.GenericArguments.bool;
import static org.spongepowered.api.command.args.GenericArguments.catalogedElement;
import static org.spongepowered.api.command.args.GenericArguments.choices;
import static org.spongepowered.api.command.args.GenericArguments.firstParsing;
import static org.spongepowered.api.command.args.GenericArguments.integer;
import static org.spongepowered.api.command.args.GenericArguments.none;
import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import net.mineguild.minecraft.treedestroyage.commands.SetConfigCommand;
import net.mineguild.minecraft.treedestroyage.event.BlockPlaceHandler;
import net.mineguild.minecraft.treedestroyage.event.BreakBlockHandler;
import net.mineguild.minecraft.treedestroyage.event.SaplingProtectionHandler;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.ConfigRoot;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Plugin(id = "treedestroyage", description = "A plugin that allows to log trees quickly!", name = "TreeDestroyage", version = "0.14-DEV-API7.0.0")
public class TreeDestroyage {

  @Inject
  private PluginContainer container;

  @Inject
  @DefaultConfig(sharedRoot = false)
  private ConfigurationLoader<CommentedConfigurationNode> configManager;

  @Inject
  @DefaultConfig(sharedRoot = false)
  private File defaultConfig;

  @Inject
  private Logger logger;

  @Inject
  private Game game;

  @Inject
  private Injector injector;

  @Inject
  @ConfigDir(sharedRoot = false)
  private Path configDir;

  public File configDir() {
    return this.configDir.toFile();
  }

  private CommentedConfigurationNode config;

  private SaplingProtectionHandler saplingHandler;

  private BlockPlaceHandler blockPlaceHandler;

  public BlockPlaceHandler getBlockPlaceHandler() {
    return this.blockPlaceHandler;
  }

  public Game getGame() {
    return game;
  }


  @Listener
  public void onInitialization(GameInitializationEvent event) {
    saplingHandler = injector.getInstance(SaplingProtectionHandler.class);
    game.getEventManager().registerListeners(this, saplingHandler);
    BreakBlockHandler breakBlockHandler = injector.getInstance(BreakBlockHandler.class);
    game.getEventManager().registerListeners(this, breakBlockHandler);

    loadConfig();

    registerCommands();
  }

  @Listener
  public void onServerStart(GameStartingServerEvent event) {
    saplingHandler.activate();

    //need to wait load worlds
    if (config.getNode("logPlayerBlocks").getBoolean(true)) {
      blockPlaceHandler = new BlockPlaceHandler(this);
      game.getEventManager().registerListeners(this, blockPlaceHandler);
    }
  }

  @Listener
  public void onGameStopping(GameStoppingEvent event) {
    try {
      configManager.save(config);
      if (blockPlaceHandler != null) {
        blockPlaceHandler.Stop();
      }
    } catch (IOException e) {
      getLogger().error("Unable to save config!", e);
    }
  }

  @Listener
  public void onReload(GameReloadEvent event) {
    try {
      reload();
      logger.info("Config reloaded!");
    } catch (IOException e) {
      logger.error("Config couldn't be reloaded!");
    }
  }

  private void reload() throws IOException {
    config = configManager.load();
    if (!config.getNode("logPlayerBlocks").getBoolean(true) && blockPlaceHandler != null) {
      blockPlaceHandler.Stop();
      game.getEventManager().unregisterListeners(blockPlaceHandler);
      blockPlaceHandler = null;
    }
  }

  private void registerCommands() {
    Set<String> newSet = new HashSet<>();
    config.getChildrenMap().keySet().forEach(str -> newSet.add((String) str));
    Map<String, String> choices = new HashMap<>();
    for (Object obj : config.getChildrenMap().keySet()) {
      choices.put((String) obj, (String) obj);
    }

    CommandSpec setSpec = CommandSpec.builder()
        .arguments(onlyOne(choices(Text.of("setting"), choices)), optional(
            firstParsing(bool(Text.of("value")), integer(Text.of("value")),
                catalogedElement(Text.of("value"), ItemType.class))))
        .description(Text.of("Change config values on-the-fly"))
        .executor(new SetConfigCommand(this))
        .permission("treedestroyage.set")
        .build();

    CommandSpec reloadSpec = CommandSpec.builder()
        .executor((src, args) -> {
              try {
                reload();
                src.sendMessage(Text.of("Config reloaded!"));
                return CommandResult.success();
              } catch (IOException e) {
                src.sendMessage(Text.of("Config couldn't be reloaded!"));
                getLogger().error("Couldn't re-load config", e);
                return CommandResult.empty();
              }
            }
        )
        .permission("treedestroyage.reload")
        .build();

    CommandSpec mainSpec = CommandSpec.builder()
        .child(setSpec, "set")
        .arguments(none())
        .child(setSpec, "set")
        .child(reloadSpec, "reload")
        .build();

    Sponge.getCommandManager().register(this, mainSpec, "trds");
  }

  private void loadConfig() {
    config = null;
    try {
      if (!defaultConfig.exists()) {
        System.out.println("Creating new file.");
        if (!defaultConfig.createNewFile()) {
          getLogger().error("Couldn't create new config file! Check R/W access!");
        }
        ConfigRoot c = game.getConfigManager().getSharedConfig(this);
        if (c.getConfigPath().toFile().exists()) {
          getLogger().info("Old config file present. Migrating into new directory.");
          try {
            config = c.getConfig().load();
            getLogger().info("Successfully migrated!");
            if(!c.getConfigPath().toFile().delete()){
              getLogger().error("Couldn't remove old file! Please use the treedestroyage/treedestroyage.conf file!");
            }
          } catch (IOException e) {
            getLogger().error("Unable to load config!", e);
            config = configManager.load();
            config.getNode("version").setValue(1);
          }
        } else {
          config = configManager.load();
          config.getNode("version").setValue(1);
        }
        configManager.save(config);
      }
      config = configManager.load();

      // Migrations here
      boolean outOfDate;
      do {
        outOfDate = configMigration(config, false);
      } while (outOfDate);

      // Validation checks here
      List<String> items = config.getNode("items").getList(TypeToken.of(String.class));
      List<String> newItems = Lists.newArrayList(items);
      List<String> toRemove = new ArrayList<>();
      items.stream().filter(id -> !game.getRegistry().getType(ItemType.class, id).isPresent())
          .forEach(toRemove::add);
      toRemove.forEach(newItems::remove);
      config.getNode("items").setValue(newItems);
      config.getNode("logPlayerBlocks").setValue(true);
      config.getNode("purgeBlocksTime").setValue(-1);
      configManager.save(config);


    } catch (IOException | ObjectMappingException e) {
      logger.error("Couldn't save config! Plugin might malfunction!");
    }
  }

  private boolean configMigration(CommentedConfigurationNode config, boolean quiet) {

    ConfigurationNode versionNode = config.getNode("version");
    switch (versionNode.getInt()) {
      case 1:
        config.getNode("enabled").setComment("Enable/disable this plugin functionality")
            .setValue(true);
        config.getNode("maxBlocks")
            .setComment("Maximum amount of blocks that will be destroyed in one hit").setValue(200);
        break;
      case 2:
        config.getNode("item").setComment("The item that needs to be held.")
            .setValue("minecraft:golden_axe");
        config.getNode("consumeItem")
            .setComment("Whether to consume the item after usage. NOT WORKING YET").setValue(false);
        break;
      case 3:
        List<String> items = new ArrayList<>();
        items.add(config.getNode("item").getString());
        config.getNode("items").setComment("List of items that can be used as axe").setValue(items);
        config.removeChild("item");
        break;
      case 4:
        config.getNode("consumeDurability")
            .setComment("Whether to consume durability from tools that support it.").setValue(true);
        break;
      case 5:
        config.removeChild("consumeItem");
        config.getNode("baseOnly")
            .setComment("Only activate when the player hits the very bottom of the tree.")
            .setValue(false);
        config.getNode("placeSapling")
            .setComment("Place the according sapling after logging the tree.").setValue(false);
        config.getNode("saplingProtection")
            .setComment("Amount of time in seconds to protect the placed sapling. (0 to disable)")
            .setValue(60);
        break;
      case 6:
        config.getNode("breakDownwards")
            .setComment("Whether the axe should also log the tree downwards.").setValue(false);
        break;
      case 7:
        config.getNode("logPlayerBlocks")
            .setComment("Log placed blocks by player to prevent destroy players buildings.")
            .setValue(true);
        config.getNode("purgeBlocksTime")
            .setComment("Remove blocks from database after x days. (-1 to disable)").setValue(-1);
        break;
      default:
        return false;
    }
    if (!quiet) {
      getLogger().info(String.format("Migrated from config version %d to %d", versionNode.getInt(),
          versionNode.getInt() + 1));
    }
    versionNode.setValue(versionNode.getInt() + 1);
    return true;
  }

  public Logger getLogger() {
    return logger;
  }

  /**
   * Get the configuration. Only use after GameStartingServerEvent is over and config is
   * initialized.
   *
   * @return config, if loaded
   */
  public CommentedConfigurationNode getConfig() {
    if (config != null) {
      return config;
    } else {
      getLogger().error("Config is not initialized!");
      return null;
    }
  }

  public SaplingProtectionHandler getSaplingHandler() {
    return saplingHandler;
  }


  public ConfigurationLoader<CommentedConfigurationNode> getConfigManager() {
    return configManager;
  }
}
