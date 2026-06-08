# Accesso operatore

OperatorLogin è una mod di autenticazione NeoForge lato server per Minecraft 1.21.1. Protegge i giocatori configurati con`/register <password>`E`/login <password>`prima che possano chattare, eseguire altri comandi, interagire con blocchi o attaccare entità.

## Obiettivo attuale

- Caricatore: NeoForge
-Minecraft: 1.21.1
- NeoForge: 21.1.228
-Giava: 21

## Note sulla sicurezza

- Le password vengono memorizzate in`config/operatorlogin-passwords.properties`come hash PBKDF2-HMAC-SHA256 salati.
- Gli hash legacy di testo normale e vecchi SHA-256 vengono migrati su PBKDF2 dopo un accesso riuscito.
- I giocatori non autenticati possono utilizzare solo`/login`E`/register`.
- I tentativi di accesso sono limitati con un blocco temporaneo.
- Le nuove password devono soddisfare la lunghezza minima configurata e non possono essere vuote o uguali al nome del giocatore.

##Configurazione

Il mod crea`config/operatorlogin.properties`alla prima esecuzione.

| Option | Default | Description |
| --- | --- | --- |
| `authOnlyOperators` | `true` | If `true`, only operators must authenticate. Set to `false` to protect every player. |
| `kickTimeoutSeconds` | `60` | How long an unauthenticated player may stay connected. |
| `minPasswordLength` | `8` | Minimum accepted password length. |
| `maxLoginAttempts` | `5` | Failed attempts before lockout. |
| `lockoutSeconds` | `60` | Lockout duration after too many failed attempts. |

## Costruire

```bash
./gradlew build
```

Il mod jar è prodotto in`build/libs/`.

## TUTTO

- [ ] Aggiungi alias di comando configurabili e messaggi localizzati.
- [ ] Aggiungi la registrazione opzionale della password in due passaggi (`/register <password> <repeatPassword>`).
- [] Aggiungi comandi di amministrazione per la reimpostazione e lo sblocco forzati della password.
- [ ] Aggiungi caching IP/sessione opzionale con un TTL breve e configurabile.
- [ ] Aggiungi GameTest o copertura di integrazione per registrazione, accesso, blocco e blocco dei comandi.
- [] Aggiungi la registrazione di controllo senza metriche per gli accessi non riusciti senza scrivere password grezze.