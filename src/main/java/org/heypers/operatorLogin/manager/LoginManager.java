package org.heypers.operatorLogin.manager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.heypers.operatorLogin.auth.AuthService;
import org.heypers.operatorLogin.config.OperatorLoginConfig;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public final class LoginManager {
    private final AuthService authService;
    private final OperatorLoginConfig config;
    private final Set<UUID> notLogged = new HashSet<>();
    private final Map<UUID, LoginState> loginStates = new HashMap<>();


    public LoginManager(AuthService authService, OperatorLoginConfig config) {
        this.authService = authService;
        this.config = config;
    }


    public boolean needsLogin(ServerPlayer player) {
        return !config.authOnlyOperators() || player.hasPermissions(2);
    }


    public void handleJoin(ServerPlayer player) {
        if (!needsLogin(player)) {
            return;
        }
        notLogged.add(player.getUUID());
        loginStates.put(player.getUUID(), new LoginState(Instant.now().getEpochSecond(), 0, 0));
        if (authService.hasPassword(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§eВведите пароль: /login <пароль>"));
        } else {
            player.sendSystemMessage(Component.literal("§eУстановите пароль: /register <пароль>"));
        }
    }


    public boolean isLogged(ServerPlayer player) {
        return !notLogged.contains(player.getUUID());
    }

    public boolean shouldBlock(ServerPlayer player) {
        return needsLogin(player) && !isLogged(player);
    }

    public void logout(ServerPlayer player) {
        notLogged.remove(player.getUUID());
        loginStates.remove(player.getUUID());
    }

    public void tick(ServerPlayer player) {
        if (!shouldBlock(player)) {
            return;
        }
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
        LoginState state = loginStates.get(player.getUUID());
        long now = Instant.now().getEpochSecond();
        if (state != null && now - state.joinedAt() >= config.kickTimeoutSeconds()) {
            player.connection.disconnect(Component.literal("Время авторизации вышло!"));
        }
    }


    public void tryRegister(ServerPlayer player, String password) {
        if (!shouldBlock(player)) {
            player.sendSystemMessage(Component.literal("§aВы уже вошли."));
            return;
        }
        if (authService.hasPassword(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы уже зарегистрированы. Используйте /login <пароль>."));
            return;
        }
        if (!isPasswordStrongEnough(player, password)) {
            return;
        }
        authService.setPassword(player.getUUID(), password);
        completeLogin(player, "§aПароль установлен! Вы вошли.");
    }


    public void tryLogin(ServerPlayer player, String password) {
        if (!shouldBlock(player)) {
            player.sendSystemMessage(Component.literal("§aВы уже вошли."));
            return;
        }
        if (!authService.hasPassword(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cВы не зарегистрированы. Используйте /register <пароль>."));
            return;
        }

        LoginState state = loginStates.computeIfAbsent(player.getUUID(), uuid -> new LoginState(Instant.now().getEpochSecond(), 0, 0));
        long now = Instant.now().getEpochSecond();
        if (state.lockedUntil() > now) {
            player.sendSystemMessage(Component.literal("§cСлишком много попыток. Подождите " + (state.lockedUntil() - now) + " сек."));
            return;
        }

        if (authService.checkPassword(player.getUUID(), password)) {
            completeLogin(player, "§aУспешный вход!");
            return;
        }

        int attempts = state.failedAttempts() + 1;
        long lockedUntil = attempts >= config.maxLoginAttempts() ? now + config.lockoutSeconds() : 0;
        loginStates.put(player.getUUID(), new LoginState(state.joinedAt(), attempts, lockedUntil));
        if (lockedUntil > 0) {
            player.sendSystemMessage(Component.literal("§cНеверный пароль. Вход заблокирован на " + config.lockoutSeconds() + " сек."));
        } else {
            player.sendSystemMessage(Component.literal("§cНеверный пароль! Осталось попыток: " + (config.maxLoginAttempts() - attempts)));
        }
    }


    private boolean isPasswordStrongEnough(ServerPlayer player, String password) {
        if (password.length() < config.minPasswordLength()) {
            player.sendSystemMessage(Component.literal("§cПароль должен быть не короче " + config.minPasswordLength() + " символов."));
            return false;
        }
        if (password.isBlank() || password.equalsIgnoreCase(player.getGameProfile().getName())) {
            player.sendSystemMessage(Component.literal("§cПароль слишком простой."));
            return false;
        }
        return true;
    }


    private void completeLogin(ServerPlayer player, String message) {
        notLogged.remove(player.getUUID());
        loginStates.remove(player.getUUID());
        player.sendSystemMessage(Component.literal(message));
    }

    private record LoginState(long joinedAt, int failedAttempts, long lockedUntil) {
    }
}