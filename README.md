# SpawnFly 1.0.1

Standalone Paper + WorldGuard plugin. LuckPerms is used through Bukkit permission inheritance, so no LuckPerms API dependency is needed.

## Requirements
- Paper 26.3 (Java 25)
- WorldGuard 7.0.19 (matches the version installed on the target server)
- LuckPerms (to assign the permission to the specified groups)

## Install
1. Put `SpawnFly-1.0.1.jar` in `plugins/`.
2. Ensure WorldGuard is installed and loads successfully.
3. Restart the server.
4. Check `plugins/SpawnFly/config.yml`; the region defaults to `spawn`.

## LuckPerms
Run:
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
- `/fly` manually enables flight only while inside the WorldGuard region `spawn`.
- `/fly` toggles flight off again when already enabled.
- On leaving the region, flight and the plugin's temporary state are revoked during the movement/teleport event.
- Re-entering spawn never re-enables flight automatically.
- Respawn (after death), disconnect, and plugin shutdown clear the temporary state. The server 26.3 API used here does not expose the older PlayerDeathEvent class.
- No repeating task, polling loop, or async Bukkit calls are used.

## Build from source
Requires JDK 25 and Gradle 8.14+:
```bash
gradle clean build
```
The build output is `build/libs/SpawnFly-1.0.1.jar`.


## Compatibility fix in 1.0.1
- Compiles against WorldGuard 7.0.19, matching the target server. The previous build targeted 7.0.21 and failed at runtime because `RegionContainer` had a different binary type.
- Removes the unavailable `PlayerDeathEvent` listener that prevented Bukkit from registering the plugin's listeners. Temporary flight state is instead cleared on every respawn.
