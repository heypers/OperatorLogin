package org.heypers.operatorLogin.config;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record OperatorLoginConfig(
        boolean authOnlyOperators,
        int kickTimeoutSeconds,
        int minPasswordLength,
        int maxLoginAttempts,
        int lockoutSeconds,
        Path passwordsFile
) {
    private static final String CONFIG_FILE_NAME = "operatorlogin.properties";
    private static final String PASSWORDS_FILE_NAME = "operatorlogin-passwords.properties";

    public static OperatorLoginConfig load() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);
        Properties properties = new Properties();

        if (Files.exists(configFile)) {
            try (InputStream input = Files.newInputStream(configFile)) {
                properties.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to read OperatorLogin config", exception);
            }
        }

        OperatorLoginConfig config = new OperatorLoginConfig(
                booleanProperty(properties, "authOnlyOperators", true),
                intProperty(properties, "kickTimeoutSeconds", 60, 5, 600),
                intProperty(properties, "minPasswordLength", 8, 6, 128),
                intProperty(properties, "maxLoginAttempts", 5, 1, 20),
                intProperty(properties, "lockoutSeconds", 60, 5, 3600),
                configDir.resolve(PASSWORDS_FILE_NAME)
        );

        properties.setProperty("authOnlyOperators", Boolean.toString(config.authOnlyOperators()));
        properties.setProperty("kickTimeoutSeconds", Integer.toString(config.kickTimeoutSeconds()));
        properties.setProperty("minPasswordLength", Integer.toString(config.minPasswordLength()));
        properties.setProperty("maxLoginAttempts", Integer.toString(config.maxLoginAttempts()));
        properties.setProperty("lockoutSeconds", Integer.toString(config.lockoutSeconds()));

        try {
            Files.createDirectories(configDir);
            try (OutputStream output = Files.newOutputStream(configFile)) {
                properties.store(output, "OperatorLogin configuration");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write OperatorLogin config", exception);
        }

        return config;
    }

    private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
    }

    private static int intProperty(Properties properties, String key, int fallback, int min, int max) {
        String rawValue = properties.getProperty(key);
        if (rawValue == null) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(rawValue);
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
