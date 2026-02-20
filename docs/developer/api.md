# API

uSkyBlock exposes APIs for plugin integrations.

## Legacy API example

```java
Plugin plugin = Bukkit.getPluginManager().getPlugin("uSkyBlock");
if (plugin instanceof uSkyBlockAPI && plugin.isEnabled()) {
  uSkyBlockAPI usb = (uSkyBlockAPI) plugin;
  player.sendMessage(String.format(
    "\u00a79Your island score is \u00a74%5.2f!",
    usb.getIslandLevel(player)
  ));
}
```

## Dependency coordinates

Use the modern artifacts from Maven Central:

- `ovh.uskyblock:uSkyBlock-Core`
- `ovh.uskyblock:uSkyBlock-APIv2`

## Migration note

API documentation is being migrated from the wiki and updated for APIv2-first usage.
