# SpawnFly 1.0.2

Standalone Paper + WorldGuard plugin. LuckPerms is used through Bukkit permission inheritance, so no LuckPerms API dependency is needed.

## Requirements
- Paper 26.3 (Java 25)
- WorldGuard 7.0.19 (matches the version installed on the target server)
- LuckPerms (to assign the permission to the specified groups)

## Install
1. Put `SpawnFly-1.0.2.jar` in `plugins/`.
2. Ensure WorldGuard is installed and loads successfully.
3. Restart the server.
4. Check `plugins/SpawnFly/config.yml`; the region defaults to `spawn`.

## LuckPerms
Grant `spawnfly.use` to the allowed groups (or configure inheritance as appropriate):
```text
/lp group staffhelpernew permission set spawnfly.use true
/lp group staffmod permission set spawnfly.use true
/lp group snrmod permission set spawnfly.use true
/lp group staffadmin permission set spawnfly.use true
/lp group celestial permission set spawnfly.use true
/lp group divine permission set spawnfly.use true
/lp group everfrost permission set spawnfly.use true
```

## Behaviour
- Players with `spawnfly.use` automatically receive flight when they enter the WorldGuard region `spawn`.
- Players who join or respawn inside `spawn` also receive flight automatically.
- Flight is disabled when they leave the region. Re-entering automatically enables it again.
- There is no `/fly` command.
- Disconnect and plugin shutdown clear the plugin's temporary state.
- No repeating task, polling loop, or async Bukkit calls are used; region checks are event-driven.

## Build from source
Requires JDK 25 and Gradle 9.1.0 or newer with Java 25 runtime support:
```bash
gradle clean build
```
The build output is `build/libs/SpawnFly-1.0.2.jar`.

## Compatibility fixes
- Uses WorldGuard 7.0.19, matching the target server.
- Does not register the unavailable `PlayerDeathEvent` class from the earlier target API.

## Build on GitHub
This project includes a GitHub Actions workflow at `.github/workflows/build.yml`. Push the project to GitHub, open **Actions**, select **Build SpawnFly**, and download the **SpawnFly-1.0.2** artifact from a successful run. The workflow uses Java 25 and Gradle 9.1.0.
