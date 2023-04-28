package daripher.worlddata.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;

import daripher.worlddata.WorldDataLoaderMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

@EventBusSubscriber(modid = WorldDataLoaderMod.MOD_ID)
public class WorldDataLoader extends DataResourceReloadListener {
	private static final Map<ResourceLocation, List<Pair<String, Resource>>> DATA_FILES = new HashMap<>();
	private static final Logger LOGGER = LogUtils.getLogger();

	private WorldDataLoader() {
		super("world");
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
		var dimensionName = location.getPath().split("/")[0];
		var dimensionId = new ResourceLocation(location.getNamespace(), dimensionName);
		addResource(location, resource, dimensionId);
	}

	private void addResource(ResourceLocation resourceLocation, Resource resource, ResourceLocation dimensionId) {
		initResourceListIfNeeded(dimensionId);
		var resourceName = resourceLocation.getPath().split("/")[1];
		DATA_FILES.get(dimensionId).add(Pair.of(resourceName, resource));
	}

	private void initResourceListIfNeeded(ResourceLocation dimensionId) {
		if (DATA_FILES.get(dimensionId) == null) {
			DATA_FILES.put(dimensionId, new ArrayList<>());
		}
	}

	@SubscribeEvent
	public static void copyWorldDataFiles(WorldEvent.Load event) {
		if (event.getWorld().isClientSide()) {
			return;
		}
		var serverLevel = (ServerLevel) event.getWorld();
		var dimension = serverLevel.dimension();
		var dataFolder = getDataFolder(serverLevel.getDataStorage());
		var resources = DATA_FILES.get(dimension.location());
		LOGGER.debug("Searching for data files for dimension {}", dimension.location());
		if (resources == null) {
			LOGGER.debug("Nothing found");
			return;
		}
		resources.forEach(pair -> copyWorldDataFile(dataFolder, pair.getFirst(), pair.getSecond()));
	}

	private static void copyWorldDataFile(File dataFolder, String fileName, Resource resource) {
		LOGGER.debug("Writing world data file {}", fileName);
		var dataFile = new File(dataFolder, fileName + ".dat");
		try {
			writeResourceIntoFile(resource, dataFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static File getDataFolder(DimensionDataStorage dataStorage) {
		return (File) ObfuscationReflectionHelper.getPrivateValue(DimensionDataStorage.class, dataStorage, "f_78146_");
	}

	private static void writeResourceIntoFile(Resource resource, File file) throws IOException {
		file.createNewFile();
		var resourceInputStream = resource.getInputStream();
		var fileOutputStream = new FileOutputStream(file, false);
		var byteBuffer = new byte[8192];
		var readByte = 0;
		while ((readByte = resourceInputStream.read(byteBuffer)) != -1) {
			fileOutputStream.write(byteBuffer, 0, readByte);
		}
		fileOutputStream.close();
	}

	@SubscribeEvent
	public static void register(AddReloadListenerEvent event) {
		event.addListener(new WorldDataLoader());
	}
}
