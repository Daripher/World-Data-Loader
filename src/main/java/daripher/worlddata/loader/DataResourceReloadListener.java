package daripher.worlddata.loader;

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
		var resources = new HashMap<ResourceLocation, Resource>();
		resourceManager.listResources(directory, this::isDataFile)
			.forEach((resourceLocation, resource) -> prepareResource(resources, resourceLocation, resource));
		return resources;
	}

	private void prepareResource(HashMap<ResourceLocation, Resource> preparedResources, ResourceLocation resourceLocation, Resource resource) {
		ResourceLocation resourceId = getResourceId(resourceLocation);
		try {
			Resource duplicate = preparedResources.put(resourceId, resource);
			if (duplicate != null) throw new IllegalStateException("Duplicate data file ignored with ID " + resourceId);
		} catch (IllegalArgumentException exception) {
			LOGGER.error("Couldn't parse data file {} from {}", resourceId, resourceLocation, exception);
		}
	}

	private ResourceLocation getResourceId(ResourceLocation resourceLocation) {
		String path = resourceLocation.getPath();
		int directoryLength = directory.length() + 1;
		return new ResourceLocation(resourceLocation.getNamespace(), path.substring(directoryLength, path.length() - PATH_SUFFIX_LENGTH));
	}

	private boolean isDataFile(ResourceLocation resourceLocation) {
		return resourceLocation.getPath().endsWith(PATH_SUFFIX);
	}
}
