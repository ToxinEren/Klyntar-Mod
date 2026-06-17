package modKlyntar.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import modKlyntar.capability.PlayerPowerCapability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TransformCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("transform")
                .then(Commands.argument("toVenom", BoolArgumentType.bool())
                        .executes(context -> transformLegacy(context, BoolArgumentType.getBool(context, "toVenom"))))
                .then(Commands.argument("form", StringArgumentType.word())
                        .executes(context -> transformForm(context, StringArgumentType.getString(context, "form")))));
    }

    private static int transformLegacy(CommandContext<CommandSourceStack> context, boolean toVenom) {
        return toVenom ? transformForm(context, "venom") : transformForm(context, "off");
    }

    private static int transformForm(CommandContext<CommandSourceStack> context, String form) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("This command can only be executed by a player"));
            return 0;
        }

        String normalized = form.trim().toLowerCase();
        if (normalized.equals("off") || normalized.equals("false") || normalized.equals("revert")) {
            PlayerPowerCapability.revertPlayer(player);
            context.getSource().sendSuccess(() -> Component.literal("Reverted symbiote transformation"), true);
            return 1;
        }

        if (!normalized.equals("venom") && !normalized.equals("carnage")) {
            context.getSource().sendFailure(Component.literal("Use /transform venom, /transform carnage, or /transform off"));
            return 0;
        }

        PlayerPowerCapability.transformPlayer(player, normalized);
        context.getSource().sendSuccess(() -> Component.literal("Transformed into " + normalized), true);
        return 1;
    }
}
