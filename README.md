# uSkyBlock

This is a continually updated and custom version of Talabrek's Ultimate SkyBlock plugin.

We are on [Spigot](https://www.spigotmc.org/resources/uskyblock-revived.66795/). Currently [Open Issues](https://github.com/uskyblock/uSkyBlock/issues)

# Installation

uSkyBlock depends on the following plugins:

* Spigot/Paper
* Vault
* WorldEdit
* WorldGuard

## Releases
https://www.spigotmc.org/resources/uskyblock-revived.66795/history

Pre-releases will end in `-SNAPSHOT`, and are considered **unsafe** for production servers.

Releases have a clean version number, have been tested, and should be safe for production servers.

# Artifact Dependencies (Maven/Gradle)
Starting with version 3.0.0-SNAPSHOT, we've changed our Maven groupId's for all submodules except uSkyBlock-API.

### Gradle
```kotlin
implementation("ovh.uskyblock:uSkyBlock-Core:3.2.0")
implementation("ovh.uskyblock:uSkyBlock-APIv2:3.2.0")
```

### Maven
```xml
<dependency>
    <groupId>ovh.uskyblock</groupId>
    <artifactId>uSkyBlock-Core</artifactId>
    <version>3.2.0</version>
</dependency>
```

We're moving new API features towards APIv2, which is available as:

```xml
<dependency>
    <groupId>ovh.uskyblock</groupId>
    <artifactId>uSkyBlock-APIv2</artifactId>
    <version>3.2.0</version>
</dependency>
```

Feel free to use any of the new APIv2 functions on servers running uSkyBlock 3.0.0+. The old API-methods will
be deprecated and removed in the upcoming plugin releases.

## Config-files

*Note*: Config files might change quite a bit, and upon activation, the plugin will try to merge the existing ones with the new ones. A backup is kept under the `uSkyBlock/backup` folder.

Please make sure, that the config files are as you expect them to be, before using the plugin or releasing it to "the public".

## Building/Compiling

To build the plugin, you need Java 21 and the project uses Gradle.

```bash
# To build the plugin
./gradlew build

# To update translations
./gradlew translation
```

The resulting JAR can be found in `uSkyBlock-Plugin/build/libs/uSkyBlock.jar`.

See also the [Wiki](https://github.com/uskyblock/uSkyBlock/wiki/Building) for more details.

# API
uSkyBlock has an API (since v2.0.1-RC1.65).

To use it, simply drop the api-jar to the classpath of your own plugin, and write some code along these lines:
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
For further details regarding the API, visit the Wiki page: https://github.com/uskyblock/uSkyBlock/wiki/uSkyBlock-API

## Contributing

Fork-away, and create pull-requests - we review and accept almost any changes.

But *please* conform with the (https://github.com/uskyblock/uSkyBlock/wiki/Coding-Guidelines)

## License

TL;DR - This is licensed under GPLv3

### History
Originally the uSkyBlock was a continuation of the skySMP plugin, which was licensed under GPLv3
(see http://dev.bukkit.org/bukkit-plugins/skysmp/).

Talabrek intended to share the code with the public, but simply didn't have the time available to do so.

Unfortunately, he had registered the plugin as `All rights reserved` on Bukkit, meaning the bukkit staff put the plugin under moderation - further increasing the work-load required to share the plugin.

Those trying to get hold on Talabrek, had a hard time, and eventually multiple developers got their hands on different versions of the uSkyBlock plugin, and tried to continue the work in various channels (wesley27 and wolfwork comes to mind).

On the very last day of 2014, we received the following e-mail from Talabrek:

> Recently, now that a stable 1.8 and the future of spigot is looking hopeful, I have gotten back to work on the plugin. There is much to be done though, and I just don't have the time to do it, so I finally decided to make it available for the public to work on. This is when I noticed the work you and others have done on the plugin.
>
> I don't have the time and energy to devote to actively developing this plugin anymore, but it is like a pet project to me so I would still like to have a role in it's development. You are making the best effort that I have seen, so I would like for you to continue.
>
> If you are interested, I can make my current code available to you (it's much different than what you currently have now, but some parts might be useful).
>
> -Talabrek

So, with Talabreks blessing, this repository will try to consolidate the many different "branches" out there.

## References

* [GPLv3](http://www.gnu.org/copyleft/gpl.html) - [tl;dr Legal](https://www.tldrlegal.com/l/gpl-3.0)
