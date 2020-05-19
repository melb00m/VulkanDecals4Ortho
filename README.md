# VulkanDecals4Ortho

This is a simple command-line tool that fixes ugly looking decals for Ortho4XP tiles in X-Plane 11 Vulkan.

It does so by adding the "NO_ALPHA" line to terrain-files that use decals as discussed in this [this thread on the X-Plane.org forums](https://forums.x-plane.org/index.php?/forums/topic/207760-better-ortho-textures-in-vulkan-xp150b1-than-xp141/).

## How to run

1. If you don't have one, download and install a Java 11 runtime (for example [AdoptOpenJDK](https://adoptopenjdk.net/?variant=openjdk11&jvmVariant=hotspot)).
2. [Download the latest release from the releases-page](https://github.com/melb00m/VulkanDecals4Ortho/releases) and unzip to any directory.
3. Run the script in your favorite command-shell: `VulkanDecals4Ortho -b <path-to-backup-directory> <path-to-ortho-scenery> [<path-to-more-ortho-sceneries>...]`. 
The ortho-scenery folder(s) you pass to the tool will be scanned for 

This is how a call of the script could look in Windows:  
`VulkanDecals4Ortho -b "L:\Decals-Backup" "L:\Ortho-Scenery\ForkboyUS" "L:\Ortho-Scenery\LyndimanNZ"`

You can also run `VulkanDecals4Ortho --help` for more usage information.

The script will automatically create backups of terrain-files it modifies in the given backup-path.

If you would like transparent roads along with your decals, check out [Transparency4Ortho](https://melb00m.github.io/Transparency4Ortho/).
