package com.killaura.command;

import com.killaura.config.KillAuraConfig;
import com.killaura.module.KillAura;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class KillAuraCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("killaura")
                        .then(ClientCommandManager.literal("toggle")
                                .executes(ctx -> {
                                    KillAura.getInstance().toggle();
                                    ctx.getSource().sendFeedback(Text.literal("KillAura toggled " +
                                            (KillAuraConfig.getInstance().enabled ? "ON" : "OFF")));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("range")
                                .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(1.0, 6.0))
                                        .executes(ctx -> {
                                            double val = DoubleArgumentType.getDouble(ctx, "value");
                                            KillAuraConfig.getInstance().range = val;
                                            KillAuraConfig.save();
                                            ctx.getSource().sendFeedback(Text.literal("Range set to " + val));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("cps")
                                .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(1.0, 20.0))
                                        .executes(ctx -> {
                                            double val = DoubleArgumentType.getDouble(ctx, "value");
                                            KillAuraConfig.getInstance().cps = val;
                                            KillAuraConfig.save();
                                            ctx.getSource().sendFeedback(Text.literal("CPS set to " + val));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("aimspeed")
                                .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0.1, 20.0))
                                        .executes(ctx -> {
                                            double val = DoubleArgumentType.getDouble(ctx, "value");
                                            KillAuraConfig.getInstance().aimSpeed = val;
                                            KillAuraConfig.save();
                                            ctx.getSource().sendFeedback(Text.literal("Aim speed set to " + val));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("autoblock")
                                .executes(ctx -> {
                                    boolean newVal = !KillAuraConfig.getInstance().autoBlock;
                                    KillAuraConfig.getInstance().autoBlock = newVal;
                                    KillAuraConfig.save();
                                    ctx.getSource().sendFeedback(Text.literal("AutoBlock " + (newVal ? "ON" : "OFF")));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("autocrit")
                                .executes(ctx -> {
                                    boolean newVal = !KillAuraConfig.getInstance().autoCrit;
                                    KillAuraConfig.getInstance().autoCrit = newVal;
                                    KillAuraConfig.save();
                                    ctx.getSource().sendFeedback(Text.literal("AutoCrit " + (newVal ? "ON" : "OFF")));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("autosprint")
                                .executes(ctx -> {
                                    boolean newVal = !KillAuraConfig.getInstance().autoSprint;
                                    KillAuraConfig.getInstance().autoSprint = newVal;
                                    KillAuraConfig.save();
                                    ctx.getSource().sendFeedback(Text.literal("AutoSprint " + (newVal ? "ON" : "OFF")));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("autototem")
                                .executes(ctx -> {
                                    boolean newVal = !KillAuraConfig.getInstance().autoTotem;
                                    KillAuraConfig.getInstance().autoTotem = newVal;
                                    KillAuraConfig.save();
                                    ctx.getSource().sendFeedback(Text.literal("AutoTotem " + (newVal ? "ON" : "OFF")));
                                    return 1;
                                }))
        );
    }
}
