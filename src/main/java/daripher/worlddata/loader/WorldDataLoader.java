package daripher.worlddata.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import daripher.worlddata.WorldDataLoaderMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

@EventBusSubscriber(modid = WorldDataLoaderMod.MOD_ID)
public class WorldDataLoader extends DataResourceReloadListener {
	private static final Map<ResourceLocation, Map<String, Resource>> DATA_FILES = new HashMap<>();
	private static final Logger LOGGER = LogUtils.getLogger();

	private WorldDataLoader() {
		super("world");
	}

	@SubscribeEvent
	public static void register(AddReloadListenerEvent event) {
		event.addListener(new WorldDataLoader());
	}

	@SubscribeEvent
	public static void writeWorldDataFiles(LevelEvent.Load event) {
		if (event.getLevel().isClientSide()) return;
		var serverLevel = (ServerLevel) event.getLevel();
		ResourceKey<Level> dimension = serverLevel.dimension();
		File dataFolder = getDataFolder(serverLevel.getDataStorage());
		ResourceLocation dimensionId = dimension.location();
		Map<String, Resource> resources = DATA_FILES.get(dimensionId);
		LOGGER.debug("Searching for data files for dimension {}", dimensionId);
		if (resources == null) {
			LOGGER.debug("Nothing found");
			return;
		}
		resources.forEach((fileName, resource) -> writeWorldDataFile(dataFolder, fileName, resource));
	}

	@Override
	protected void apply(Map<ResourceLocation, Resource> resources, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
		DATA_FILES.clear();
		resources.forEach(this::readWorldDataFile);
	}

	private void readWorldDataFile(ResourceLocation location, Resource resource) {
		if (!location.getPath().contains("/")) {
			LOGGER.error("Couldn't read world data file {}, no dimension specified", location);
			return;
		}
		LOGGER.info("Reading world data file {}", location);
		String dimensionName = location.getPath().split("/")[0];
		var dimensionId = new ResourceLocation(location.getNamespace(), dimensionName);
		addResource(location, resource, dimensionId);
	}

	private void addResource(ResourceLocation resourceLocation, Resource resource, ResourceLocation dimensionId) {
		if (DATA_FILES.get(dimensionId) == null) DATA_FILES.put(dimensionId, new HashMap<>());
		String resourceName = resourceLocation.getPath().split("/")[1];
		DATA_FILES.get(dimensionId).put(resourceName, resource);
	}

	private static void writeWorldDataFile(File dataFolder, String fileName, Resource resource) {
		LOGGER.debug("Writing world data file {}", fileName);
		var dataFile = new File(dataFolder, fileName + ".dat");
		try {
			writeResourceIntoFile(resource, dataFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeResourceIntoFile(Resource resource, File file) throws IOException {
		file.createNewFile();
		InputStream input = resource.open();
		var output = new FileOutputStream(file, false);
		byte[] buffer = new byte[8192];
		for (int i = 0; (i = input.read(buffer)) != -1; output.write(buffer, 0, i));
		output.close();
	}

	private static File getDataFolder(DimensionDataStorage dataStorage) {
		return (File) ObfuscationReflectionHelper.getPrivateValue(DimensionDataStorage.class, dataStorage, "f_78146_");
	}
}
