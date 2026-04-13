package dev.demonz.redstonereboot.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.demonz.redstonereboot.common.RedstoneRebootCore;

/**
 * Shared Brigadier command registration for Fabric, Forge, and NeoForge.
 */
public class BrigadierCommand {

    private final RedstoneRebootCore core;
    private final CommandProcessor processor;

    public BrigadierCommand(RedstoneRebootCore core) {
        this.core = core;
        this.processor = new CommandProcessor(core);
    }

    public <S> void register(CommandDispatcher<S> dispatcher, CommandSourceFactory<S> factory) {
        LiteralArgumentBuilder<S> reboot = LiteralArgumentBuilder.literal("reboot");

        // status
        reboot.then(LiteralArgumentBuilder.<S>literal("status")
            .requires(src -> {
                CommandProcessor.CommandSender sender = factory.create(src);
                return sender.hasPermission("redstonereboot.status") || sender.hasPermission("redstonereboot.use");
            })
            .executes(ctx -> {
                processor.processStatus(factory.create(ctx.getSource()));
                return 1;
            }));

        // cancel
        reboot.then(LiteralArgumentBuilder.<S>literal("cancel")
            .requires(src -> factory.create(src).hasPermission("redstonereboot.restart.cancel"))
            .executes(ctx -> {
                processor.processCancel(factory.create(ctx.getSource()));
                return 1;
            }));

        // now [delay]
        reboot.then(LiteralArgumentBuilder.<S>literal("now")
            .requires(src -> factory.create(src).hasPermission("redstonereboot.restart.now"))
            .executes(ctx -> {
                processor.processNow(factory.create(ctx.getSource()), 60);
                return 1;
            })
            .then(RequiredArgumentBuilder.<S, Integer>argument("delay", IntegerArgumentType.integer(0, 3600))
                .executes(ctx -> {
                    int delay = IntegerArgumentType.getInteger(ctx, "delay");
                    processor.processNow(factory.create(ctx.getSource()), delay);
                    return 1;
                })));

        // schedule <delay>
        reboot.then(LiteralArgumentBuilder.<S>literal("schedule")
            .requires(src -> factory.create(src).hasPermission("redstonereboot.restart.schedule"))
            .then(RequiredArgumentBuilder.<S, Integer>argument("delay", IntegerArgumentType.integer(1, 86400))
                .executes(ctx -> {
                    int delay = IntegerArgumentType.getInteger(ctx, "delay");
                    processor.processSchedule(factory.create(ctx.getSource()), delay);
                    return 1;
                })));

        // reload
        reboot.then(LiteralArgumentBuilder.<S>literal("reload")
            .requires(src -> factory.create(src).hasPermission("redstonereboot.config.reload"))
            .executes(ctx -> {
                processor.processReload(factory.create(ctx.getSource()));
                return 1;
            }));

        // info
        reboot.then(LiteralArgumentBuilder.<S>literal("info")
            .requires(src -> {
                CommandProcessor.CommandSender sender = factory.create(src);
                return sender.hasPermission("redstonereboot.status") || sender.hasPermission("redstonereboot.use");
            })
            .executes(ctx -> {
                processor.processInfo(factory.create(ctx.getSource()));
                return 1;
            }));

        // doctor
        reboot.then(LiteralArgumentBuilder.<S>literal("doctor")
            .requires(src -> factory.create(src).hasPermission("redstonereboot.doctor"))
            .executes(ctx -> {
                processor.processDoctor(factory.create(ctx.getSource()));
                return 1;
            }));

        // help
        reboot.then(LiteralArgumentBuilder.<S>literal("help")
            .executes(ctx -> {
                processor.processHelp(factory.create(ctx.getSource()));
                return 1;
            }));

        dispatcher.register(reboot);
    }

    /**
     * Factory to wrap platform-specific command sources.
     */
    public interface CommandSourceFactory<S> {
        CommandProcessor.CommandSender create(S source);
    }
}
