package net.mineguild.TreeDestroyage.commands;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import net.mineguild.TreeDestroyage.TreeDestroyage;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.translator.ConfigurateTranslator;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.persistence.DataSource;
import org.spongepowered.api.util.persistence.DataSourceFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
public class SetConfigCommand implements CommandExecutor {
    private TreeDestroyage plugin;

    public SetConfigCommand(TreeDestroyage plugin) {
        this.plugin = plugin;
    }


    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String setting = (String) args.getOne("setting").get();
        Optional<Object> value = args.getOne("value");
        if (plugin.getConfig().getNode(setting).isVirtual() || setting.equals("version")) {
            src.sendMessage(Text.of("Invalid value!"));
            return CommandResult.empty();
        } else {
            if (setting.equals("item") && !value.isPresent() && src instanceof Player) {
                Player p = (Player) src;
                Optional<ItemStack> item = p.getItemInHand();
                if (item.isPresent()) {
                    try {
                        List<String> items = plugin.getConfig().getNode("items").getList(TypeToken.of(String.class));
                        if (!items.contains(item.get().getItem().getName())) {
                            List<String> newItems = Lists.newArrayList(items);
                            newItems.add(item.get().getItem().getName());
                            plugin.getConfig().getNode("items").setValue(newItems);
                            src.sendMessage(Text.of(item.get().getItem().getName(), " added to items"));
                        } else {
                            src.sendMessage(Text.of("Already in list!"));
                        }
                    } catch (ObjectMappingException e) {
                        e.printStackTrace();
                    }
                    return CommandResult.success();
                } else {
                    src.sendMessage(Text.of("No item in hand!"));
                }
            } else if (setting.equals("item") && value.isPresent()) {
                ItemType item = (ItemType) value.get();
                plugin.getConfig().getNode(setting).setValue(item.getName());
                src.sendMessage(Text.of("Item was successfully changed to ", item.getName()));
            } else if (value.isPresent()) {
                if (plugin.getConfig().getNode(setting).getValue().getClass().equals(value.get().getClass())) {
                    plugin.getConfig().getNode(setting).setValue(value.get());
                    src.sendMessage(Text.of(setting, " succesfully changed to ", value.get()));
                    try {
                        plugin.getConfigManager().save(plugin.getConfig());
                    } catch (IOException e) {
                        plugin.getLogger().error("Unable to save config!", e);
                    }
                    return CommandResult.success();
                } else {
                    src.sendMessage(Text.of("Value of wrong type!"));
                }
            }
        }
        return CommandResult.empty();
    }
}
