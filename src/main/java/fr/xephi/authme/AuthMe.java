package fr.xephi.authme;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.DataSourceProvider;
import fr.xephi.authme.initialization.OnShutdownPlayerSaver;
import fr.xephi.authme.initialization.OnStartupTasks;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import fr.xephi.authme.initialization.SettingsProvider;
import fr.xephi.authme.initialization.TaskCloser;
import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.listener.EntityListener;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.listener.PlayerListener111;
import fr.xephi.authme.listener.PlayerListener19;
import fr.xephi.authme.listener.PlayerListener19Spigot;
import fr.xephi.authme.listener.DoubleLoginFixListener;
import fr.xephi.authme.listener.GuiCaptchaHandler;
import fr.xephi.authme.listener.PlayerListenerHigherThan18;
import fr.xephi.authme.listener.ServerListener;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.Sha256;
import fr.xephi.authme.service.BackupService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.MigrationService;
import fr.xephi.authme.service.bungeecord.BungeeReceiver;
import fr.xephi.authme.service.yaml.YamlParseException;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SettingsWarner;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.CleanupTask;
import fr.xephi.authme.task.purge.PurgeService;
import fr.xephi.authme.util.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_MINUTE;
import static fr.xephi.authme.util.Utils.isClassLoaded;

/**
 * The AuthMe main class.
 */
public class AuthMe extends JavaPlugin {

    // Constants
    private static final String PLUGIN_NAME = "AuthMeReloaded";
    private static final String LOG_FILENAME = "authme.log";
    private static final int CLEANUP_INTERVAL = 5 * TICKS_PER_MINUTE;

    // Version and build number values
    private static String pluginVersion = "5.6.0-Fork";
    private static final String pluginBuild = "b";
    private static String pluginBuildNumber = "25";
    protected final Boolean SHAEnabled = false;
    // Private instances
    private EmailService emailService;
    private CommandHandler commandHandler;
    @Inject
    public static Settings settings;
    private DataSource database;
    private BukkitService bukkitService;
    private Injector injector;
    private BackupService backupService;
    public static ConsoleLogger logger;
    /**
     * Constructor.
     */
    public AuthMe() {
    }


    /**
     * Get the plugin's name.
     *
     * @return The plugin's name.
     */
    public static String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Get the plugin's version.
     *
     * @return The plugin's version.
     */
    public static String getPluginVersion() {
        return pluginVersion;
    }

    /**
     * Get the plugin's build number.
     *
     * @return The plugin's build number.
     */
    public static String getPluginBuildNumber() {
        return pluginBuildNumber;
    }


    /**
     * Method called when the server enables the plugin.
     */
    @Override
    public void onEnable() {
        // Load the plugin version data from the plugin description file
        loadPluginInfo(getDescription().getVersion());

        // Set the Logger instance and log file path
        ConsoleLogger.initialize(getLogger(), new File(getDataFolder(), LOG_FILENAME));
        logger = ConsoleLoggerFactory.get(AuthMe.class);
        logger.info("You are running an unofficial fork version of AuthMe!");


        // Check server version
        if (!isClassLoaded("org.spigotmc.event.player.PlayerSpawnLocationEvent")
            || !isClassLoaded("org.bukkit.event.player.PlayerInteractAtEntityEvent")) {
            logger.warning("You are running an unsupported server version (" + getServerNameVersionSafe() + "). "
                + "AuthMe requires Spigot 1.8.X or later!");
            stopOrUnload();
            return;
        }

        // Prevent running AuthMeBridge due to major exploit issues
        if (getServer().getPluginManager().isPluginEnabled("AuthMeBridge")) {
            logger.warning("Detected AuthMeBridge, support for it has been dropped as it was "
                + "causing exploit issues, please use AuthMeBungee instead! Aborting!");
            stopOrUnload();
            return;
        }

        // Initialize the plugin
        try {
            initialize();
        } catch (Throwable th) {
            YamlParseException yamlParseException = ExceptionUtils.findThrowableInCause(YamlParseException.class, th);
            if (yamlParseException == null) {
                logger.logException("Aborting initialization of AuthMe:", th);
                th.printStackTrace();
            } else {
                logger.logException("File '" + yamlParseException.getFile() + "' contains invalid YAML. "
                    + "Please run its contents through http://yamllint.com", yamlParseException);
            }
            stopOrUnload();
            return;
        }

        // Show settings warnings
        injector.getSingleton(SettingsWarner.class).logWarningsForMisconfigurations();

        // Schedule clean up task
        CleanupTask cleanupTask = injector.getSingleton(CleanupTask.class);
        cleanupTask.runTaskTimerAsynchronously(this, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
        // Do a backup on start
        backupService.doBackup(BackupService.BackupCause.START);

        // Set up Metrics
        OnStartupTasks.sendMetrics(this, settings);
        if(settings.getProperty(SecuritySettings.SHOW_STARTUP_BANNER)) {
            String loadColorConfig = settings.getProperty(SecuritySettings.STARTUP_BANNER_COLOR);
            if (loadColorConfig.startsWith("§")) {
                logger.info("\n" + loadColorConfig + "    ___         __  __    __  ___   \n" +
                    loadColorConfig + "   /   | __  __/ /_/ /_  /  |/  /__ \n" +
                    loadColorConfig + "  / /| |/ / / / __/ __ \\/ /|_/ / _ \\\n" +
                    loadColorConfig + " / ___ / /_/ / /_/ / / / /  / /  __/\n" +
                    loadColorConfig + "/_/  |_\\__,_/\\__/_/ /_/_/  /_/\\___/ \n" +
                    loadColorConfig + "                                    ");
            }else {
                logger.info("\n"+"    ___         __  __    __  ___   \n" +
                    "   /   | __  __/ /_/ /_  /  |/  /__ \n" +
                    "  / /| |/ / / / __/ __ \\/ /|_/ / _ \\\n" +
                    " / ___ / /_/ / /_/ / / / /  / /  __/\n" +
                    "/_/  |_\\__,_/\\__/_/ /_/_/  /_/\\___/ \n" +
                    "                                    ");
            }
        }
        // Successful message
        //detect server brand with classloader
        checkServerType();
        logger.info("AuthMeReReloaded is enabled successfully!");
        // Purge on start if enabled
        PurgeService purgeService = injector.getSingleton(PurgeService.class);
        purgeService.runAutoPurge();
        // 注册玩家加入事件监听
//        if (settings.getProperty(SecuritySettings.ANTI_GHOST_PLAYERS) || settings.getProperty(SecuritySettings.SMART_ASYNC_TELEPORT)/* || settings.getProperty(SecuritySettings.GUI_CAPTCHA)*/) {
        if (settings.getProperty(SecuritySettings.ANTI_GHOST_PLAYERS)) {
            getServer().getPluginManager().registerEvents(new DoubleLoginFixListener((Plugin) this), this);
        }
        if (settings.getProperty(SecuritySettings.GUI_CAPTCHA) && getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            getServer().getPluginManager().registerEvents(new GuiCaptchaHandler((Plugin) this), this);
            logger.info("(Alpha4)GUICaptcha Feature is enabled successfully!");
            logger.info("These features are still in development, if you encountered any problem, please report.");
        } else if (settings.getProperty(SecuritySettings.GUI_CAPTCHA) && !getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            logger.warning("ProtocolLib is not loaded, we can't enable GUI Captcha.");
        }
        logger.info("GitHub: https://github.com/HaHaWTH/AuthMeReReloaded/");

        if (settings.getProperty(SecuritySettings.CHECK_FOR_UPDATES)) {
            checkForUpdates();
        }

        if (SHAEnabled){
            //shaChecker();
        }
    }
    public File pluginfile = getFile();
    /**
     * Load the version and build number of the plugin from the description file.
     *
     * @param versionRaw the version as given by the plugin description file
     */

    private static void loadPluginInfo(String versionRaw) {
        int index = versionRaw.lastIndexOf("-");
        if (index != -1) {
            pluginVersion = versionRaw.substring(0, index);
            pluginBuildNumber = versionRaw.substring(index + 1);
            if (pluginBuildNumber.startsWith("b")) {
                pluginBuildNumber = pluginBuildNumber.substring(1);
            }
        }
    }

    /**
     * Initialize the plugin and all the services.
     */
    private void initialize() {
        // Create plugin folder
        getDataFolder().mkdir();

        // Create injector, provide elements from the Bukkit environment and register providers
        injector = new InjectorBuilder()
            .addDefaultHandlers("fr.xephi.authme")
            .create();
        injector.register(AuthMe.class, this);
        injector.register(Server.class, getServer());
        injector.register(PluginManager.class, getServer().getPluginManager());
        injector.register(BukkitScheduler.class, getServer().getScheduler());
        injector.provide(DataFolder.class, getDataFolder());
        injector.registerProvider(Settings.class, SettingsProvider.class);
        injector.registerProvider(DataSource.class, DataSourceProvider.class);

        // Get settings and set up logger
        settings = injector.getSingleton(Settings.class);
        ConsoleLoggerFactory.reloadSettings(settings);
        OnStartupTasks.setupConsoleFilter(getLogger());

        // Set all service fields on the AuthMe class
        instantiateServices(injector);

        // Convert deprecated PLAINTEXT hash entries
        MigrationService.changePlainTextToSha256(settings, database, new Sha256());

        // If the server is empty (fresh start) just set all the players as unlogged
        if (bukkitService.getOnlinePlayers().isEmpty()) {
            database.purgeLogged();
        }

        // Register event listeners
        registerEventListeners(injector);

        // Start Email recall task if needed
        OnStartupTasks onStartupTasks = injector.newInstance(OnStartupTasks.class);
        onStartupTasks.scheduleRecallEmailTask();
    }

    /**
     * Instantiates all services.
     *
     * @param injector the injector
     */
    void instantiateServices(Injector injector) {
        database = injector.getSingleton(DataSource.class);
        bukkitService = injector.getSingleton(BukkitService.class);
        commandHandler = injector.getSingleton(CommandHandler.class);
        emailService = injector.getSingleton(EmailService.class);
        backupService = injector.getSingleton(BackupService.class);

        // Trigger instantiation (class not used elsewhere)
        injector.getSingleton(BungeeReceiver.class);

        // Trigger construction of API classes; they will keep track of the singleton
        injector.getSingleton(AuthMeApi.class);
    }

    /**
     * Registers all event listeners.
     *
     * @param injector the injector
     */
    void registerEventListeners(Injector injector) {
        // Get the plugin manager instance
        PluginManager pluginManager = getServer().getPluginManager();

        // Register event listeners
        pluginManager.registerEvents(injector.getSingleton(PlayerListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(BlockListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(EntityListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(ServerListener.class), this);

        // Try to register 1.8+ player listeners
        if (isClassLoaded("org.bukkit.event.entity.EntityPickupItemEvent") && isClassLoaded("org.bukkit.event.player.PlayerSwapHandItemsEvent")){
            pluginManager.registerEvents(injector.getSingleton(PlayerListenerHigherThan18.class), this);
        } else if (isClassLoaded("org.bukkit.event.player.PlayerSwapHandItemsEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener19.class), this);
        }

        // Try to register 1.9 player listeners(Moved to else-if)
//        if (isClassLoaded("org.bukkit.event.player.PlayerSwapHandItemsEvent")) {
//            pluginManager.registerEvents(injector.getSingleton(PlayerListener19.class), this);
//        }

        // Try to register 1.9 spigot player listeners
        if (isClassLoaded("org.spigotmc.event.player.PlayerSpawnLocationEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener19Spigot.class), this);
        }

        // Register listener for 1.11 events if available
        if (isClassLoaded("org.bukkit.event.entity.EntityAirChangeEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener111.class), this);
        }
    }

    /**
     * Stops the server or disables the plugin, as defined in the configuration.
     */
    public void stopOrUnload() {
        if (settings == null || settings.getProperty(SecuritySettings.STOP_SERVER_ON_PROBLEM)) {
            getLogger().warning("THE SERVER IS GOING TO SHUT DOWN AS DEFINED IN THE CONFIGURATION!");
            setEnabled(false);
            getServer().shutdown();
        } else {
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        // onDisable is also called when we prematurely abort, so any field may be null
        OnShutdownPlayerSaver onShutdownPlayerSaver = injector == null
            ? null
            : injector.createIfHasDependencies(OnShutdownPlayerSaver.class);
        if (onShutdownPlayerSaver != null) {
            onShutdownPlayerSaver.saveAllPlayers();
        }
        if (settings.getProperty(EmailSettings.SHUTDOWN_MAIL)){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy'.'MM'.'dd'.' HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            emailService.sendShutDown(settings.getProperty(EmailSettings.SHUTDOWN_MAIL_ADDRESS),dateFormat.format(date));
        }

        // Do backup on stop if enabled
        if (backupService != null) {
            backupService.doBackup(BackupService.BackupCause.STOP);
        }

        // Wait for tasks and close data source
        new TaskCloser(this, database).run();

        // Disabled correctly
        Consumer<String> infoLogMethod = logger == null ? getLogger()::info : logger::info;
        infoLogMethod.accept("AuthMe " + this.getDescription().getVersion() + " is unloaded successfully!");
        ConsoleLogger.closeFileWriter();
    }
    private static final String owner = "HaHaWTH";
    private static final String owner_gitee = "Shixuehan114514";
    private static final String repo = "AuthMeReReloaded";
    private void checkForUpdates() {
        logger.info("Checking for updates...");
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // 从南通集线器获取最新版本号

                URL url = new URL("https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000); // 设置连接超时为10秒
                conn.setReadTimeout(10000); // 设置读取超时为10秒
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\Z").next();
                scanner.close();

                // 处理JSON响应
                String latestVersion = response.substring(response.indexOf("tag_name") + 11);
                latestVersion = latestVersion.substring(0, latestVersion.indexOf("\""));
                if ((pluginBuild + pluginBuildNumber).equals(latestVersion)) {
                    getLogger().log(Level.INFO,"You are running the latest version.");
                }
                if (!(pluginBuild + pluginBuildNumber).equals(latestVersion)) {
                    // Display update message
                    String message = "New version available! Latest:" + latestVersion + " Current:" + pluginBuild + pluginBuildNumber;
                    getLogger().log(Level.INFO, message);
                    getLogger().log(Level.INFO,"Download from here:github.com/HaHaWTH/AuthMeReReloaded/releases/latest");
                }
            }catch (IOException e) {
                getLogger().log(Level.WARNING,"Error occurred while checking updates from GitHub. Reason: " + e.getMessage());
                }
        });
        }

    private void checkServerType() {
        if (isClassLoaded("com.destroystokyo.paper.PaperConfig")) {
            logger.info("AuthMeReReloaded is running on Paper");
        }else if (isClassLoaded("catserver.server.CatServerConfig")) {
            logger.info("AuthMeReReloaded is running on CatServer");
        } else if (isClassLoaded("org.spigotmc.SpigotConfig")) {
            logger.info("AuthMeReReloaded is running on Spigot");
        } else if (isClassLoaded("org.bukkit.craftbukkit.CraftServer")) {
            logger.info("AuthMeReReloaded is running on Bukkit");
        } else {
            logger.info("AuthMeReReloaded is running on Unknown*");
        }
    }
    // 其他方法和事件处理


    // 其他方法和事件处理
    private static final String SHA_URL = "https://raw.githubusercontent.com/"+ owner +"/"+ repo + "/master/"+pluginBuild +pluginBuildNumber+ ".sha";
    private static final String ALGORITHM = "SHA-256";
    private static final String PROXY_URL = "https://ghproxy.com/";
    private static final String SHA_URL_GITEE = "https://gitee.com/"+ owner_gitee +"/"+ repo + "/raw/master/"+pluginBuild+pluginBuildNumber+ ".sha";

//    public void shaChecker() {
//        // 请求SHA文件
//
//        String actualSha;
//        try {
//            URL url;
//            if(settings.getProperty(SecuritySettings.SHA_CHECK_METHOD).equals("github")) {
//                url = new URL(SHA_URL);
//                logger.info("正在检查文件完整性...(GitHub)");
//            } else if(settings.getProperty(SecuritySettings.SHA_CHECK_METHOD).equals("ghproxy")) {
//                url = new URL(PROXY_URL + SHA_URL);
//                logger.info("正在检查文件完整性...(GhProxy)");
//            } else if (settings.getProperty(SecuritySettings.SHA_CHECK_METHOD).equals("gitee")) {
//                url = new URL(SHA_URL_GITEE);
//                logger.info("正在检查文件完整性...(Gitee)");
//            }else {
//                logger.warning("未知的SHA检查方法,将从GitHub获取SHA文件");
//                url = new URL(SHA_URL);
//            }
//
//
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setConnectTimeout(10000);
//            conn.setReadTimeout(9000);
//            conn.setRequestMethod("GET");
//            InputStream stream = conn.getInputStream();
//            ByteArrayOutputStream result = new ByteArrayOutputStream();
//            byte[] buffer = new byte[1024];
//            int length;
//            while ((length = stream.read(buffer)) != -1) {
//                result.write(buffer, 0, length);
//            }
//            String expectedSha = result.toString().trim();
//            // 计算插件文件的SHA值
//            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
//            byte[] fileBytes = Files.readAllBytes(pluginfile.toPath());
//            byte[] hashBytes = md.digest(fileBytes);
//            StringBuilder sb = new StringBuilder();
//            for (byte b : hashBytes) {
//                sb.append(String.format("%02x", b));
//            }
//            actualSha = sb.toString();
//
//            // 比较SHA值并加载插件
//            if (expectedSha.equals(actualSha)) {
//                logger.info("SHA联网安全校验完毕");
//            } else {
//                // SHA值不匹配，插件可能被篡改
//                logger.warning("SHA值不匹配,插件被篡改");
//                stopOrUnload();
//            }
//        }catch (NoSuchAlgorithmException | IOException e){
//            logger.warning("SHA校验失败,请尝试切换校验API");
//            logger.warning("您当前请求的API为:" + settings.getProperty(SecuritySettings.SHA_CHECK_METHOD));
//            stopOrUnload();
//        }
//    }


    /**
     * Handle Bukkit commands.
     *
     * @param sender       The command sender (Bukkit).
     * @param cmd          The command (Bukkit).
     * @param commandLabel The command label (Bukkit).
     * @param args         The command arguments (Bukkit).
     * @return True if the command was executed, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String commandLabel, String[] args) {
        // Make sure the command handler has been initialized
        if (commandHandler == null) {
            getLogger().severe("AuthMe command handler is not available");
            return false;
        }

        // Handle the command
        return commandHandler.processCommand(sender, commandLabel, args);
    }

    private String getServerNameVersionSafe() {
        try {
            Server server = getServer();
            return server.getName() + " v. " + server.getVersion();
        } catch (Throwable ignore) {
            return "-";
        }
    }
}
