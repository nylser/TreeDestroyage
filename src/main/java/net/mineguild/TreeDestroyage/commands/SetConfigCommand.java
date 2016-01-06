package net.mineguild.TreeDestroyage.commands;

import net.mineguild.TreeDestroyage.TreeDestroyage;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.io.IOException;
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
                    plugin.getConfig().getNode(setting).setValue(item.get().getItem().getName());
                    src.sendMessage(Text.of("Item was changed to ", item.get().getItem().getName()));
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
