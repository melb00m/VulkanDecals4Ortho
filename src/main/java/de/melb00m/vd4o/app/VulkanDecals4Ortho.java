package de.melb00m.vd4o.app;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(
    name = "VulkanDecals4Ortho",
    mixinStandardHelpOptions = true,
    version = "VulkanDecals4Ortho v.0.1.0",
    header = "Fixes terrain decals for Ortho4XP 1.2 tiles in X-Plane 11 Vulkan",
    footer =
        "For information on how this application operates, please refer to the documentation at \nhttps://github.com/melb00m/VulkanDecals4Ortho")
public class VulkanDecals4Ortho implements Runnable {

  private static final Pattern TERRAIN_FILENAME_PATTERN =
      Pattern.compile("\\d{5,6}_\\d{5,6}_.+[.]ter", Pattern.CASE_INSENSITIVE);
  private static final String TERRAIN = "terrain";
  private static final String EARTH_NAV_DATA = "Earth nav data";
  private static final String DECAL_LIB = "DECAL_LIB";
  private static final String NO_ALPHA = "NO_ALPHA";

  @CommandLine.Parameters(
      index = "0",
      arity = "1..*",
      description = "Paths to ortho-scenery folder(s)")
  private Set<Path> orthoPaths;

  @CommandLine.Option(
      description = "Path to backup folder",
      required = true,
      names = {"-b", "--backupFolder"})
  private Path backupPath;

  public static void main(String[] args) {
    new CommandLine(new VulkanDecals4Ortho()).execute(args);
  }

  @Override
  public void run() {

    checkPathExistsOrThrow(orthoPaths);

    final var terrainFiles = collectTerrainFiles();
    if (terrainFiles.isEmpty()) {
      System.out.println("No terrain files need modifications.");
    } else {
      final var pbb =
          new ProgressBarBuilder()
              .setStyle(ProgressBarStyle.ASCII)
              .setUpdateIntervalMillis(100)
              .setTaskName("Updating Terrain-Files");
      ProgressBar.wrap(terrainFiles, pbb).forEach(this::backupAndModifyTerrainFileWithNoAlpha);
      System.out.println(
          String.format(
              "%d terrain-files have been updated with %s.", terrainFiles.size(), NO_ALPHA));
      System.out.println(
          String.format(
              "Backups of modified files have been created in %s.", backupPath.toAbsolutePath()));
    }
  }

  private static void checkPathExistsOrThrow(final Collection<Path> paths) {
    for (var path : paths) {
      if (!Files.exists(path)) {
        throw new IllegalArgumentException(String.format("Given path '%s' does not exist", path));
      }
    }
  }

  private Set<Path> collectTerrainFiles() {
    // search for potential terrain files by name
    System.out.println(
        "Searching for terrain-files in given ortho-scenery folders (this may take a while)");
    final var terrainFiles =
        orthoPaths
            .parallelStream()
            .flatMap(VulkanDecals4Ortho::uncheckedWalk)
            .filter(VulkanDecals4Ortho::isPotentialTerrainFile)
            .collect(Collectors.toUnmodifiableSet());

    // Look into files which need modification
    final var pbb =
        new ProgressBarBuilder()
            .setStyle(ProgressBarStyle.ASCII)
            .setUpdateIntervalMillis(100)
            .setTaskName("Analyzing terrain files");
    return ProgressBar.wrap(terrainFiles.parallelStream(), pbb)
        .filter(VulkanDecals4Ortho::needsAlphaAddition)
        .collect(Collectors.toUnmodifiableSet());
  }

  private void backupAndModifyTerrainFileWithNoAlpha(final Path terrainFile) {
    try {
      // create backup first
      final var backupFile =
          backupPath.resolve(getSceneryFolderParent(terrainFile).relativize(terrainFile));
      if (Files.exists(backupFile)) {
        throw new IllegalStateException(
            String.format(
                "Can't backup terrain-file '%s' at '%s': Backup target-file already exists",
                terrainFile, backupFile));
      }
      Files.createDirectories(backupFile.getParent());
      Files.copy(terrainFile, backupFile);

      // add NO_ALPHA line
      final var lines = Files.readAllLines(terrainFile);
      lines.add(NO_ALPHA);
      Files.write(terrainFile, lines);
    } catch (IOException e) {
      throw new IllegalStateException(
          String.format("Failed to modify terrain file: %s", terrainFile), e);
    }
  }

  private static Stream<Path> uncheckedWalk(final Path path) {
    try {
      return Files.walk(path);
    } catch (IOException e) {
      throw new IllegalStateException(
          String.format("Failed to traverse subdirectories of: %s", path), e);
    }
  }

  private static boolean isPotentialTerrainFile(final Path path) {
    return Files.isRegularFile(path)
        && TERRAIN_FILENAME_PATTERN.matcher(path.getFileName().toString()).matches()
        && path.getParent().getFileName().toString().equals(TERRAIN)
        && Files.isDirectory(path.getParent().getParent().resolve(EARTH_NAV_DATA));
  }

  private static boolean needsAlphaAddition(final Path terrainFile) {
    try {
      final var lines = Files.readAllLines(terrainFile);
      return !lines.contains(NO_ALPHA)
          && lines.stream().anyMatch(line -> line.startsWith(DECAL_LIB));
    } catch (IOException e) {
      throw new IllegalStateException(
          String.format("Failed to read terrain-file: %s", terrainFile), e);
    }
  }

  private static Path getSceneryFolderParent(final Path terFile) {
    var path = terFile;
    do {
      if (Files.exists(path.resolve(EARTH_NAV_DATA))) {
        return path.getParent().getParent();
      }
      path = path.getParent();
    } while (path != null);
    throw new IllegalStateException(
        String.format("No scenery base-folder found for terrain-file: %s", terFile));
  }
}
