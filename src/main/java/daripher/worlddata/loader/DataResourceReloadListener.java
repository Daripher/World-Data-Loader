package daripher.worlddata.loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class DataResourceReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, Resource>> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String PATH_SUFFIX = ".dat";
	private static final int PATH_SUFFIX_LENGTH = PATH_SUFFIX.length();
	private final String directory;

	public DataResourceReloadListener(String directory) {
		this.directory = directory;
	}

	@Override
	protected Map<ResourceLocation, Resource> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
		var preparedResources = new HashMap<ResourceLocation, Resource>();
		var resources = resourceManager.listResources(directory, this::isDataFile);
		resources.forEach(resourceLocation -> prepareResource(resourceManager, preparedResources, resourceLocation));
		return preparedResources;
	}

	private void prepareResource(ResourceManager resourceManager, HashMap<ResourceLocation, Resource> preparedResources, ResourceLocation resourceLocation) {
		var resourceId = getResourceId(resourceLocation);
		try {
			var resource = resourceManager.getResource(resourceLocation);
			var duplicateResource = preparedResources.put(resourceId, resource);
			if (duplicateResource != null) {
				throw new IllegalStateException("Duplicate data file ignored with ID " + resourceId);
			}
		} catch (IllegalArgumentException | IOException exception) {
			LOGGER.error("Couldn't parse data file {} from {}", resourceId, resourceLocation, exception);
		}
	}

	private ResourceLocation getResourceId(ResourceLocation resourceLocation) {
		var path = resourceLocation.getPath();
		var directoryLength = directory.length() + 1;
		return new ResourceLocation(resourceLocation.getNamespace(), path.substring(directoryLength, path.length() - PATH_SUFFIX_LENGTH));
	}

	private boolean isDataFile(String fileName) {
		return fileName.endsWith(PATH_SUFFIX);
	}
}
