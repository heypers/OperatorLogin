# OperatorLogin

OperatorLogin is a server-side NeoForge authentication mod for Minecraft 1.21.1. It protects configured players with `/register <password>` and `/login <password>` before they can chat, run other commands, interact with blocks, or attack entities.

## Current target

- Loader: NeoForge
- Minecraft: 1.21.1
- NeoForge: 21.1.228
- Java: 21

## Security notes

- Passwords are stored in `config/operatorlogin-passwords.properties` as salted PBKDF2-HMAC-SHA256 hashes.
- Legacy plain-text and old SHA-256 hashes are migrated to PBKDF2 after a successful login.
- Unauthenticated players can only use `/login` and `/register`.
- Login attempts are rate-limited with a temporary lockout.
- New passwords must satisfy the configured minimum length and cannot be blank or equal to the player name.

## Configuration

The mod creates `config/operatorlogin.properties` on first run.

| Option | Default | Description |
| --- | --- | --- |
| `authOnlyOperators` | `true` | If `true`, only operators must authenticate. Set to `false` to protect every player. |
| `kickTimeoutSeconds` | `60` | How long an unauthenticated player may stay connected. |
| `minPasswordLength` | `8` | Minimum accepted password length. |
| `maxLoginAttempts` | `5` | Failed attempts before lockout. |
| `lockoutSeconds` | `60` | Lockout duration after too many failed attempts. |

## Build

```bash
./gradlew build
```

The mod jar is produced in `build/libs/`.

## TODO

- [ ] Add configurable command aliases and localized messages.
- [ ] Add optional two-step password registration (`/register <password> <repeatPassword>`).
- [ ] Add admin commands for forced password reset and unlock.
- [ ] Add optional IP/session caching with a short, configurable TTL.
- [ ] Add GameTest or integration coverage for registration, login, lockout, and command blocking.
- [ ] Add metrics-free audit logging for failed login bursts without writing raw passwords.
