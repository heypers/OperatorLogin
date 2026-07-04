package org.heypers.operatorLogin;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.heypers.operatorLogin.auth.AuthService;
import org.heypers.operatorLogin.config.OperatorLoginConfig;
import org.heypers.operatorLogin.manager.LoginManager;


@Mod(OperatorLogin.MOD_ID)
public final class OperatorLogin {
    public static final String MOD_ID = "operatorlogin";

    private final AuthService authService;
    private final LoginManager loginManager;
    private final OperatorLoginConfig config;

    public OperatorLogin() {
        this.config = OperatorLoginConfig.load();
        this.authService = new AuthService(config.passwordsFile());
        this.loginManager = new LoginManager(authService, config);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("login")
                .then(Commands.argument("password", StringArgumentType.greedyString())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            loginManager.tryLogin(player, StringArgumentType.getString(context, "password"));
                            return 1;
                        })));

        event.getDispatcher().register(Commands.literal("register")
                .then(Commands.argument("password", StringArgumentType.string())
                        .executes(context -> {

                            ServerPlayer player = context.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal("§cПожалуйста, введи повтор пароля. Используйте /register <пароль> <повтор пароля>."));
                            return 1;
                        }))
                .then(Commands.argument("password", StringArgumentType.string())
                        .then(Commands.argument("repeatPassword", StringArgumentType.string())
                                .executes(context -> {

                                    String password = StringArgumentType.getString(context, "password");
                                    String repeatPassword = StringArgumentType.getString(context, "repeatPassword");

                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    if (password.equals(repeatPassword)) {
                                        loginManager.tryRegister(player, password);
                                    } else {
                                        player.sendSystemMessage(Component.literal("§cПовтор пароля и пароль не совпадают, пожалуйста попробуйте ещё раз."));
                                    }

                                    return 1;
                                }))));
    }

    @SubscribeEvent
    public void onResetPasswordCommand(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("resetpassword")
                .then(Commands.argument("player", StringArgumentType.string())
                .executes(context ->{

                    ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "player"));

                    if (player != null) {
                        if (authService.hasPassword(player.getUUID())) {
                            authService.removePassword(player.getUUID());
                            loginManager.removeSession(player);

                            context.getSource().getPlayerOrException().sendSystemMessage(Component.literal("§aПароль был успешно сброшен!"));
                        }
                    }

                  return 1;
                })));
    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            loginManager.handleJoin(player);
        }
    }

    @SubscribeEvent
    public void onQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            loginManager.logout(player);
        }
    }

    @SubscribeEvent
    public void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            loginManager.tick(player);
        }
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        if (loginManager.shouldBlock(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onCommand(net.neoforged.neoforge.event.CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (loginManager.isLogged(player)) {
            return;
        }
        String command = event.getParseResults().getReader().getString().trim().toLowerCase();
        if (!command.startsWith("login") && !command.startsWith("register")) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cСначала авторизуйтесь: /login <пароль> или /register <пароль>"));
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && loginManager.shouldBlock(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelInteraction(event);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelInteraction(event);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelInteraction(event);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelInteraction(event);
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelInteraction(event);
    }

    private void cancelInteraction(PlayerInteractEvent event) {
        Player player = event.getEntity();
        if (event instanceof ICancellableEvent cancellableEvent
                && player instanceof ServerPlayer serverPlayer
                && loginManager.shouldBlock(serverPlayer)) {
            cancellableEvent.setCanceled(true);
        }
    }
}
