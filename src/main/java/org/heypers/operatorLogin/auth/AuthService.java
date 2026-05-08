package org.heypers.operatorLogin.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;


public final class AuthService {
    private final Path passwordsFile;
    private final Properties passwords = new Properties();

    public AuthService(Path passwordsFile) {
        this.passwordsFile = passwordsFile;
        load();
    }

    public synchronized boolean hasPassword(UUID uuid) {
        return passwords.containsKey(uuid.toString());
    }


    public synchronized void setPassword(UUID uuid, String rawPassword) {
        passwords.setProperty(uuid.toString(), PasswordHasher.hash(rawPassword));
        save();
    }

    public synchronized boolean checkPassword(UUID uuid, String input) {
        String stored = passwords.getProperty(uuid.toString());
        boolean matches = PasswordHasher.matches(input, stored);
        if (matches && !PasswordHasher.isModernHash(stored)) {
            setPassword(uuid, input);
        }
        return matches;
    }


    private void load() {
        if (!Files.exists(passwordsFile)) {
            return;
        }
        try (InputStream input = Files.newInputStream(passwordsFile)) {
            passwords.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read OperatorLogin passwords", exception);
        }
    }


    private void save() {
        try {
            Files.createDirectories(passwordsFile.getParent());
            try (OutputStream output = Files.newOutputStream(passwordsFile)) {
                passwords.store(output, "OperatorLogin password hashes - do not edit or share");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save OperatorLogin passwords", exception);
        }
    }
}