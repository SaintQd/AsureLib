package org.saintqd.asurelib.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.saintqd.asurelib.AsureLib;
import org.saintqd.asurelib.gui.data.CustomGUI;
import org.saintqd.asurelib.utils.AsureUtils;

import java.util.Arrays;
import java.util.HashSet;

public class AsureLibCommandsManager {

    public static void setupCommands(AsureLib plugin) {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("asurelib")
                            .executes(commandContext -> {
                                commandContext.getSource().getSender().sendMessage(AsureLib.inst().getLangManager().parseLangString(plugin,"not_enough_arguments"));
                                return Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.literal("reload")
                                    .requires(predicate -> predicate.getSender().hasPermission("asurelib.admin"))
                                    .executes(ctx -> {
                                        reloadCommand(ctx.getSource().getSender());
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(Commands.literal("debug")
                                    .requires(predicate -> predicate.getSender().hasPermission("asurelib.admin"))
                                    .executes(ctx -> {
                                        changeDebugLevelCommand(ctx.getSource().getSender(),0);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                            .requires(predicate -> predicate.getSender().hasPermission("asurelib.admin"))
                                            .executes(ctx -> {
                                                changeDebugLevelCommand(ctx.getSource().getSender(),ctx.getArgument("level", Integer.class));
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(Commands.literal("debugcategories")
                                    .requires(predicate -> predicate.getSender().hasPermission("asurelib.admin"))
                                    .executes(ctx -> {
                                        setDebugCategoriesCommand(ctx.getSource().getSender(),"");
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("categories", StringArgumentType.greedyString())
                                            .requires(predicate -> predicate.getSender().hasPermission("asurelib.admin"))
                                            .executes(ctx -> {
                                                setDebugCategoriesCommand(ctx.getSource().getSender(),ctx.getArgument("categories", String.class));
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(Commands.literal("opengui")
                                    .then(Commands.argument("name", ArgumentTypes.namespacedKey())
                                            .suggests((ctx,builder) -> {
                                                String partName = builder.getRemaining();
                                                AsureLib.inst().getCustomGUIManager().getGuiPaths().forEach((key, guiName) -> {
                                                    String keyString = key.asString();
                                                    if (keyString.startsWith(partName.toLowerCase()))
                                                        builder.suggest(keyString);
                                                });
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                openCustomGUICommand(
                                                        ctx.getSource().getSender(),
                                                        ctx.getArgument("name", NamespacedKey.class),
                                                        null
                                                );
                                                return Command.SINGLE_SUCCESS;
                                            })
                                            .then(Commands.argument("player", ArgumentTypes.player())
                                                    .requires(predicate -> predicate.getSender().hasPermission("asurelib.admin"))
                                                    .executes(ctx -> {
                                                        openCustomGUICommand(
                                                                ctx.getSource().getSender(),
                                                                ctx.getLastChild().getArgument("name",NamespacedKey.class),
                                                                ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst()
                                                        );
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                            .build(),
                    "Основная команда."

            );

        });
    }

    private static void reloadCommand(CommandSender sender) {
        AsureLib.inst().loadData();
        if (sender instanceof Player)
            sender.sendMessage(AsureLib.inst().getLangManager().parseLangString(AsureLib.inst(),"reloadMessage"));
    }

    private static void changeDebugLevelCommand(CommandSender sender, int level) {
        AsureLib.inst().setDebugLevel(level);
        sender.sendMessage(AsureUtils.parseString("<gray>Debug level set to <blue>"+level+"</blue>."));
    }

    private static void setDebugCategoriesCommand(CommandSender sender, String categories) {
        if (categories.isEmpty()) {
            AsureLib.inst().setDebugCategories(new HashSet<>());
            sender.sendMessage(AsureUtils.parseString("<gray>Debug categories cleared."));
            return;
        }
        String[] categoriesArray = categories.split(",");
        if (categoriesArray.length <= 1)
            categoriesArray = categories.split(" ");
        AsureLib.inst().setDebugCategories(new HashSet<>(Arrays.asList(categoriesArray)));
        sender.sendMessage(AsureUtils.parseString("<gray>Debug categories set to <blue>"+String.join(", ",categoriesArray) +"</blue>."));
    }

    private static void openCustomGUICommand(CommandSender sender, NamespacedKey menuKey, Player player) {

        player = AsureUtils.checkForPlayerPresent(sender, player);

        if (!AsureLib.inst().getCustomGUIManager().getGuiPaths().containsKey(menuKey)) {
            sender.sendMessage(AsureLib.inst().getLangManager().parseLangString(AsureLib.inst(),"open_menu_command_does_not_exist",menuKey.asString()));
            return;
        }
        CustomGUI customGUI = new CustomGUI(player,menuKey);
        if (!customGUI.getConfig().getBoolean("PlayerOpen") && !sender.hasPermission("asurecore.admin")) {
            sender.sendMessage(AsureLib.inst().getLangManager().parseLangString(AsureLib.inst(),"no_permission"));
            return;
        }
        if (sender != player) {
            sender.sendMessage(AsureLib.inst().getLangManager().parseLangString(AsureLib.inst(),"open_menu_command_for_player",menuKey.asString(), player.getName()));
        }
        customGUI.setMainMenu();
        customGUI.openInventory(player);
    }

}
