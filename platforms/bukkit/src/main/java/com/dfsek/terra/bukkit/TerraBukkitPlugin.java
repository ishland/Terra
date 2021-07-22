package com.dfsek.terra.bukkit;

import com.dfsek.tectonic.exception.LoadException;
import com.dfsek.tectonic.loading.TypeRegistry;
import com.dfsek.terra.api.Logger;
import com.dfsek.terra.api.TerraPlugin;
import com.dfsek.terra.api.addon.TerraAddon;
import com.dfsek.terra.api.addon.annotations.Addon;
import com.dfsek.terra.api.addon.annotations.Author;
import com.dfsek.terra.api.addon.annotations.Version;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.command.CommandManager;
import com.dfsek.terra.api.command.exception.MalformedCommandException;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.config.PluginConfig;
import com.dfsek.terra.api.event.EventManager;
import com.dfsek.terra.api.handle.ItemHandle;
import com.dfsek.terra.api.handle.WorldHandle;
import com.dfsek.terra.api.lang.Language;
import com.dfsek.terra.api.profiler.Profiler;
import com.dfsek.terra.api.registry.CheckedRegistry;
import com.dfsek.terra.api.registry.Registry;
import com.dfsek.terra.api.world.TerraWorld;
import com.dfsek.terra.api.world.World;
import com.dfsek.terra.api.world.biome.Biome;
import com.dfsek.terra.api.world.generator.TerraChunkGenerator;
import com.dfsek.terra.bukkit.command.BukkitCommandAdapter;
import com.dfsek.terra.bukkit.command.FixChunkCommand;
import com.dfsek.terra.bukkit.command.SaveDataCommand;
import com.dfsek.terra.bukkit.generator.BukkitChunkGeneratorWrapper;
import com.dfsek.terra.bukkit.handles.BukkitItemHandle;
import com.dfsek.terra.bukkit.handles.BukkitWorldHandle;
import com.dfsek.terra.bukkit.listeners.CommonListener;
import com.dfsek.terra.bukkit.listeners.PaperListener;
import com.dfsek.terra.bukkit.listeners.SpigotListener;
import com.dfsek.terra.bukkit.util.PaperUtil;
import com.dfsek.terra.bukkit.world.BukkitBiome;
import com.dfsek.terra.bukkit.world.BukkitWorld;
import com.dfsek.terra.commands.CommandUtil;
import com.dfsek.terra.commands.TerraCommandManager;
import com.dfsek.terra.config.GenericLoaders;
import com.dfsek.terra.config.PluginConfigImpl;
import com.dfsek.terra.config.lang.LangUtil;
import com.dfsek.terra.event.EventManagerImpl;
import com.dfsek.terra.profiler.ProfilerImpl;
import com.dfsek.terra.registry.CheckedRegistryImpl;
import com.dfsek.terra.registry.LockedRegistryImpl;
import com.dfsek.terra.registry.master.AddonRegistry;
import com.dfsek.terra.registry.master.ConfigRegistry;
import com.dfsek.terra.util.logging.DebugLogger;
import com.dfsek.terra.util.logging.JavaLogger;
import com.dfsek.terra.world.TerraWorldImpl;
import io.papermc.lib.PaperLib;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class TerraBukkitPlugin extends JavaPlugin implements TerraPlugin {
    public static final BukkitVersion BUKKIT_VERSION;

    static {
        String ver = Bukkit.getServer().getClass().getPackage().getName();
        if(ver.contains("1_17")) BUKKIT_VERSION = BukkitVersion.V1_17;
        else if(ver.contains("1_16")) BUKKIT_VERSION = BukkitVersion.V1_16;
        else if(ver.contains("1_15")) BUKKIT_VERSION = BukkitVersion.V1_15;
        else if(ver.contains("1_14")) BUKKIT_VERSION = BukkitVersion.V1_14;
        else if(ver.contains("1_13")) BUKKIT_VERSION = BukkitVersion.V1_13;
        else BUKKIT_VERSION = BukkitVersion.UNKNOWN;
    }

    private final Map<String, com.dfsek.terra.api.world.generator.TerraChunkGenerator> generatorMap = new HashMap<>();
    private final Map<World, TerraWorld> worldMap = new HashMap<>();
    private final Map<String, ConfigPack> worlds = new HashMap<>();
    private final Profiler profiler = new ProfilerImpl();
    private final ConfigRegistry registry = new ConfigRegistry();
    private final CheckedRegistry<ConfigPack> checkedRegistry = new CheckedRegistryImpl<>(registry);
    private final PluginConfig config = new PluginConfigImpl();
    private final ItemHandle itemHandle = new BukkitItemHandle();
    private final GenericLoaders genericLoaders = new GenericLoaders(this);
    private final EventManager eventManager = new EventManagerImpl(this);
    private final AddonRegistry addonRegistry = new AddonRegistry(new BukkitAddon(this), this);
    private final LockedRegistryImpl<TerraAddon> addonLockedRegistry = new LockedRegistryImpl<>(addonRegistry);
    private WorldHandle handle = new BukkitWorldHandle();
    private DebugLogger debugLogger;

    public boolean reload() {
        config.load(this);
        LangUtil.load(config.getLanguage(), this); // Load language.
        boolean succeed = registry.loadAll(this);
        Map<World, TerraWorld> newMap = new HashMap<>();
        worldMap.forEach((world, tw) -> {
            tw.getConfig().getSamplerCache().clear();
            String packID = tw.getConfig().getID();
            newMap.put(world, new TerraWorldImpl(world, registry.get(packID), this));
        });
        worldMap.clear();
        worldMap.putAll(newMap);
        return succeed;
    }

    @Override
    public ItemHandle getItemHandle() {
        return itemHandle;
    }

    @Override
    public String platformName() {
        return "Bukkit";
    }

    @Override
    public DebugLogger getDebugLogger() {
        return debugLogger;
    }

    @Override
    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public void runPossiblyUnsafeTask(Runnable task) {
        Bukkit.getScheduler().runTask(this, task);
    }

    @Override
    public Profiler getProfiler() {
        return profiler;
    }

    @Override
    public void onDisable() {
        BukkitChunkGeneratorWrapper.saveAll();
    }

    @Override
    public void onEnable() {
        debugLogger = new DebugLogger(logger());

        getLogger().info("Running on version " + BUKKIT_VERSION);
        if(BUKKIT_VERSION == BukkitVersion.UNKNOWN) {
            getLogger().warning("Terra is running on an unknown Bukkit version. Proceed with caution.");
        }

        saveDefaultConfig();

        Metrics metrics = new Metrics(this, 9017); // Set up bStats.
        metrics.addCustomChart(new Metrics.SingleLineChart("worlds", worldMap::size)); // World number chart.

        config.load(this); // Load master config.yml
        LangUtil.load(config.getLanguage(), this); // Load language.

        debugLogger.setDebug(config.isDebugLogging());
        if(config.isDebugProfiler()) profiler.start();

        if(!addonRegistry.loadAll()) {
            getLogger().severe("Failed to load addons. Please correct com.dfsek.terra.addon installations to continue.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        registry.loadAll(this); // Load all config packs.

        PluginCommand c = Objects.requireNonNull(getCommand("terra"));

        CommandManager manager = new TerraCommandManager(this);


        try {
            CommandUtil.registerAll(manager);
            manager.register("save-data", SaveDataCommand.class);
            manager.register("fix-chunk", FixChunkCommand.class);
        } catch(MalformedCommandException e) { // This should never happen.
            logger().severe("Errors occurred while registering commands.");
            e.printStackTrace();
            logger().severe("Please report this to Terra.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        BukkitCommandAdapter command = new BukkitCommandAdapter(manager);

        c.setExecutor(command);
        c.setTabCompleter(command);


        long save = config.getDataSaveInterval();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, BukkitChunkGeneratorWrapper::saveAll, save, save); // Schedule population data saving
        Bukkit.getPluginManager().registerEvents(new CommonListener(this), this); // Register master event listener
        PaperUtil.checkPaper(this);

        if(PaperLib.isPaper()) {
            try {
                Class.forName("io.papermc.paper.event.world.StructureLocateEvent"); // Check if user is on Paper version with event.
                Bukkit.getPluginManager().registerEvents(new PaperListener(this), this); // Register Paper events.
            } catch(ClassNotFoundException e) {
                registerSpigotEvents(true); // Outdated Paper version.
            }
        } else {
            registerSpigotEvents(false);
        }
    }

    private void registerSpigotEvents(boolean outdated) {
        if(outdated) {
            getLogger().severe("You are using an outdated version of Paper.");
            getLogger().severe("This version does not contain StructureLocateEvent.");
            getLogger().severe("Terra will now fall back to Spigot events.");
            getLogger().severe("This will prevent cartographer villagers from spawning,");
            getLogger().severe("and cause structure location to not function.");
            getLogger().severe("If you want these functionalities, update to the latest build of Paper.");
            getLogger().severe("If you use a fork, update to the latest version, then if you still");
            getLogger().severe("receive this message, ask the fork developer to update upstream.");
        } else {
            getLogger().severe("Paper is not in use. Falling back to Spigot events.");
            getLogger().severe("This will prevent cartographer villagers from spawning,");
            getLogger().severe("and cause structure location to not function.");
            getLogger().severe("If you want these functionalities (and all the other");
            getLogger().severe("benefits that Paper offers), upgrade your server to Paper.");
            getLogger().severe("Find out more at https://papermc.io/");
        }

        Bukkit.getPluginManager().registerEvents(new SpigotListener(this), this); // Register Spigot event listener
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return new BukkitChunkGeneratorWrapper(generatorMap.computeIfAbsent(worldName, name -> {
            if(!registry.contains(id)) throw new IllegalArgumentException("No such config pack \"" + id + "\"");
            ConfigPack pack = registry.get(id);
            worlds.put(worldName, pack);
            return pack.getGeneratorProvider().newInstance(pack);
        }));
    }

    @Override
    public Language getLanguage() {
        return LangUtil.getLanguage();
    }

    public CheckedRegistry<ConfigPack> getConfigRegistry() {
        return checkedRegistry;
    }

    public TerraWorld getWorld(World world) {
        BukkitWorld w = (BukkitWorld) world;
        if(!w.isTerraWorld())
            throw new IllegalArgumentException("Not a Terra world! " + w.getGenerator());
        if(!worlds.containsKey(w.getName())) {
            getLogger().warning("Unexpected world load detected: \"" + w.getName() + "\"");
            return new TerraWorldImpl(w, ((TerraChunkGenerator) w.getGenerator().getHandle()).getConfigPack(), this);
        }
        return worldMap.computeIfAbsent(w, w2 -> new TerraWorldImpl(w, worlds.get(w.getName()), this));
    }

    @Override
    public Logger logger() {
        return new JavaLogger(getLogger());
    }

    @NotNull
    @Override
    public PluginConfig getTerraConfig() {
        return config;
    }

    @Override
    public WorldHandle getWorldHandle() {
        return handle;
    }


    @Override
    public void register(TypeRegistry registry) {
        registry
                .registerLoader(BlockState.class, (t, o, l) -> handle.createBlockData((String) o))
                .registerLoader(Biome.class, (t, o, l) -> parseBiome((String) o))
                .registerLoader(EntityType.class, (t, o, l) -> EntityType.valueOf((String) o));
        genericLoaders.register(registry);
    }

    private BukkitBiome parseBiome(String id) throws LoadException {
        if(!id.startsWith("minecraft:")) throw new LoadException("Invalid biome identifier " + id);
        return new BukkitBiome(org.bukkit.block.Biome.valueOf(id.toUpperCase(Locale.ROOT).substring(10)));
    }

    @Override
    public Registry<TerraAddon> getAddons() {
        return addonLockedRegistry;
    }

    public enum BukkitVersion {
        V1_13(13),

        V1_14(14),

        V1_15(15),

        V1_16(16),

        V1_17(17),

        UNKNOWN(Integer.MAX_VALUE); // Assume unknown version is latest.

        private final int index;

        BukkitVersion(int index) {
            this.index = index;
        }

        /**
         * Gets if this version is above or equal to another.
         *
         * @param other Other version
         * @return Whether this version is equal to or later than other.
         */
        public boolean above(BukkitVersion other) {
            return this.index >= other.index;
        }
    }

    @Addon("Terra-Bukkit")
    @Version("1.0.0")
    @Author("Terra")
    private static final class BukkitAddon extends TerraAddon {
        private final TerraPlugin main;

        private BukkitAddon(TerraPlugin main) {
            this.main = main;
        }

        @Override
        public void initialize() {

        }
    }
}
