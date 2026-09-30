package dev.hoshno.neoforgeskyboxes.resource;

import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.mixin.resource.AbstractResourcePackAccessor;
import dev.hoshno.neoforgeskyboxes.mixin.resource.FallbackResourceManagerAccessor;
import dev.hoshno.neoforgeskyboxes.mixin.resource.SimpleReloadableResourceManagerAccessor;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

/** Finds skybox JSON files in active 1.12.2 packs, whose resource manager cannot enumerate IDs. */
public final class ResourcePackScanner {
    private static final String ASSET_PREFIX = "assets/" + NeoforgeSkyboxes.FABRIC_SKYBOXES_NAMESPACE + "/";
    private static final String SKY_PREFIX = ASSET_PREFIX + "sky/";

    private ResourcePackScanner() {
    }

    public static Set<ResourceLocation> findSkyboxResources(IResourceManager resourceManager) {
        if (!(resourceManager instanceof SimpleReloadableResourceManager)) {
            NeoforgeSkyboxes.getLogger().warn("Cannot list skybox files from resource manager {}", resourceManager.getClass().getName());
            return Collections.emptySet();
        }

        Map<String, FallbackResourceManager> domains =
                ((SimpleReloadableResourceManagerAccessor) resourceManager).getDomainResourceManagers();
        Set<IResourcePack> packs = new LinkedHashSet<>();
        for (FallbackResourceManager domain : domains.values()) {
            packs.addAll(((FallbackResourceManagerAccessor) domain).getResourcePacks());
        }

        Set<ResourceLocation> resources = new LinkedHashSet<>();
        for (IResourcePack pack : packs) {
            File packFile = getPackFile(pack);
            if (packFile == null) {
                continue;
            }
            if (packFile.isDirectory()) {
                scanDirectory(packFile, resources);
            } else if (packFile.isFile()) {
                scanZip(packFile, resources);
            }
        }
        return Collections.unmodifiableSet(resources);
    }

    private static File getPackFile(IResourcePack pack) {
        if (pack instanceof AbstractResourcePack) {
            return ((AbstractResourcePackAccessor) pack).getResourcePackFile();
        }
        NeoforgeSkyboxes.getLogger().debug("Skipping non-file resource pack type {} while listing skybox resources", pack.getClass().getName());
        return null;
    }

    private static void scanDirectory(File packFile, Set<ResourceLocation> resources) {
        Path root = packFile.toPath().resolve(SKY_PREFIX);
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace(File.separatorChar, '/'))
                    .filter(path -> path.endsWith(".json"))
                    .forEach(path -> resources.add(new ResourceLocation(NeoforgeSkyboxes.FABRIC_SKYBOXES_NAMESPACE, "sky/" + path)));
        } catch (IOException exception) {
            NeoforgeSkyboxes.getLogger().warn("Could not scan resource pack directory {}", root, exception);
        }
    }

    private static void scanZip(File packFile, Set<ResourceLocation> resources) {
        try (ZipFile zip = new ZipFile(packFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(SKY_PREFIX) && name.endsWith(".json")) {
                    String resourcePath = name.substring(ASSET_PREFIX.length());
                    resources.add(new ResourceLocation(NeoforgeSkyboxes.FABRIC_SKYBOXES_NAMESPACE, resourcePath));
                }
            }
        } catch (IOException exception) {
            NeoforgeSkyboxes.getLogger().warn("Could not scan resource pack archive {}", packFile, exception);
        }
    }
}
