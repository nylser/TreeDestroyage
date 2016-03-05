package net.mineguild.TreeDestroyage;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import net.minecrell.mcstats.SpongeStatsLite;
import net.mineguild.TreeDestroyage.commands.SetConfigCommand;
import net.mineguild.TreeDestroyage.event.BreakBlockHandler;
import net.mineguild.TreeDestroyage.event.SaplingProtectionHandler;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.spongepowered.api.command.args.GenericArguments.*;

@Plugin(id = "TreeDestroyage", name = "TreeDestroyage", version = "0.8-DEV")
public class TreeDestroyage {

    @Inject
    private PluginContainer container;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private File defaultConfig;

    @Inject
    public SpongeStatsLite stats;

    @Inject
    private Logger logger;

    @Inject
    private Game game;
    private CommentedConfigurationNode config;

    private BreakBlockHandler breakBlockHandler;
    private SaplingProtectionHandler saplingHandler;

    public Game getGame() {
        return game;
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        this.stats.start();
    }

    @Listener
    public void onInitialization(GameInitializationEvent event) {
        breakBlockHandler = new BreakBlockHandler(this);
        Sponge.getGame().getEventManager().registerListeners(this, breakBlockHandler);
        saplingHandler = new SaplingProtectionHandler(this);
        Sponge.getGame().getEventManager().registerListeners(this, saplingHandler);
    }

    @Listener
    public void onServerStart(GameStartingServerEvent event) {
        config = null;
        try {
            if (!defaultConfig.exists()) {
                if (!defaultConfig.createNewFile()) {
                    throw new RuntimeException("Unable to create configuration file!");
                }
                config = configManager.load();
                config.getNode("version").setValue(1);
                configManager.save(config);
            }
            config = configManager.load();


            //Migrations here
            boolean outOfDate;
            do {
                outOfDate = configMigration();
            } while (outOfDate);

            //Validation checks here
            List<String> items = config.getNode("items").getList(TypeToken.of(String.class));
            List<String> newItems = Lists.newArrayList(items);
            List<String> toRemove = new ArrayList<>();
            items.stream().filter(id -> !game.getRegistry().getType(ItemType.class, id).isPresent()).forEach(toRemove::add);
            toRemove.forEach(newItems::remove);
            config.getNode("items").setValue(newItems);
            configManager.save(config);

        } catch (IOException e) {
            getLogger().error("Unable to load/create config!", e);
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
        Set<String> newSet = Sets.newHashSet();
        config.getChildrenMap().keySet().forEach(str -> newSet.add((String) str));
        Map<String, String> choices = new HashMap<>();
        for (Object obj : config.getChildrenMap().keySet()) {
            choices.put((String) obj, (String) obj);
        }

        
        CommandSpec setSpec = CommandSpec.builder().arguments(onlyOne(choices(Text.of("setting"), choices)), optional(firstParsing(bool(Text.of("value")), integer(Text.of("value")), catalogedElement(Text.of("value"), ItemType.class)))).description(Text.of("Change config values on-the-fly")).executor(new SetConfigCommand(this))
                .permission("TreeDestroyage.set").build();

        CommandSpec reloadSpec = CommandSpec.builder().executor((src, args) -> {
                    try {
                        config = configManager.load();
                        src.sendMessage(Text.of("Config reloaded!"));
                        return CommandResult.success();
                    } catch (IOException e) {
                        src.sendMessage(Text.of("Config couldn't be reloaded!"));
                        getLogger().error("Couldn't re-load config", e);
                        return CommandResult.empty();
                    }
                }
        ).permission("TreeDestroyage.reload").build();
        CommandSpec mainSpec = CommandSpec.builder().child(setSpec, "set").arguments(none()).build();
        Sponge.getCommandManager().register(this, setSpec, "trds");
        saplingHandler.activate();
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        try {
            configManager.save(config);
        } catch (IOException e) {
            getLogger().error("Unable to save config!", e);
        }
    }

    public boolean configMigration(){
        ConfigurationNode versionNode = config.getNode("version");
        switch (versionNode.getInt()) {
            case 1:
                config.getNode("enabled").setComment("Enable/disable this plugin functionality").setValue(true);
                config.getNode("maxBlocks").setComment("Maximum amount of blocks that will be destroyed in one hit").setValue(200);
                break;
            case 2:
                config.getNode("item").setComment("The item that needs to be held.").setValue("minecraft:golden_axe");
                config.getNode("consumeItem").setComment("Whether to consume the item after usage. NOT WORKING YET").setValue(false);
                break;
            case 3:
                List<String> items = new ArrayList<>();
                items.add(config.getNode("item").getString());
                config.getNode("items").setComment("List of items that can be used as axe").setValue(items);
                config.removeChild("item");
                break;
            case 4:
                config.getNode("consumeDurability").setComment("Whether to consume durability from tools that support it.").setValue(true);
                break;
            case 5:
                config.removeChild("consumeItem");
                config.getNode("baseOnly").setComment("Only activate when the player hits the very bottom of the tree.").setValue(false);
                config.getNode("placeSapling").setComment("Place the according sapling after logging the tree.").setValue(false);
                config.getNode("saplingProtection").setComment("Amount of time in seconds to protect the placed sapling. (0 to disable)").setValue(60);
                break;
            default:
                return false;
        }
        getLogger().info(String.format("Migrated from config version %d to %d", versionNode.getInt(), versionNode.getInt() + 1));
        versionNode.setValue(versionNode.getInt() + 1);
        return true;

    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Get the configuration. Only use after GameStartingServerEvent is over and config is initialized.
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

    public SaplingProtectionHandler getSaplingHandler(){
        return saplingHandler;
    }


    public ConfigurationLoader<CommentedConfigurationNode> getConfigManager() {
        return configManager;
    }
}
