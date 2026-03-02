# OptiFrames

<!-- [![Modrinth](https://img.shields.io/badge/Modrinth-visit-blue)](https://modrinth.com/mod/optiframes) [![Downloads](https://img.shields.io/badge/downloads-%E2%89%A50-blue)](https://modrinth.com/mod/optiframes) [![License](https://img.shields.io/badge/license-MIT-lightgrey)](LICENSE) -->

A lightweight, client-side Fabric mod that makes large map displays render cleaner and smoother without changing their appearance.

## Performance Test with a 70x70 map (4900 item frames)

![performance img](screenshots/performance.png)

## Why use OptiFrames?

- Optimized rendering: hides redundant borders between adjacent maps.  
- FPS gain: expect up to **3.5x FPS increase** with large maps.  
- Lightweight: designed to work well alongside Sodium and Iris.

## Installation

1. Download the latest `optiframes-<version>.jar` from the Modrinth/Curseforge page.  
2. Place the JAR in your `mods` folder.  
3. Start Minecraft using a Fabric profile.

## Configuration

You can configure the mod with [Mod Menu](https://modrinth.com/mod/modmenu):
- Enable/Disable the mod
- Enable/Disable frames borders rendering (even more optimized)
- Enable/Disable frames borders texture rendering

## Compatibility

- Client-side only — safe to use on any server.  
- Works well with Sodium and Iris.  
- Tested on every available version.

## Troubleshooting

- Mod not appearing in Mod Menu: confirm the JAR is in `mods` and you launched the Fabric profile.  
- Crashes: please attach `latest.log` and any `crash-reports` when opening an issue.