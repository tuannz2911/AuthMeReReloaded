package fr.xephi.authme.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static fr.xephi.authme.util.PlayerUtils.getPlayerIp;
import static fr.xephi.authme.util.PlayerUtils.isNpc;
import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

/**
 * This class handles ALL the GUI captcha features in the plugin.
 */
public class GuiCaptchaHandler implements Listener {
    //define AuthMeApi
    private final AuthMeApi authmeApi = AuthMeApi.getInstance();
    @Inject
    private BukkitService bukkitService;
    @Inject
    private AuthMe plugin;
    @Inject
    private Messages messages;
    @Inject
    private CommonService service;

    @Inject
    private Settings settings;


    private PacketAdapter chatPacketListener;
    private PacketAdapter chunkPacketListener;
    private PacketAdapter windowPacketListener;

    //define timesLeft
    private int timesLeft = 3;
    //Use ConcurrentHashMap to store player and their close reason
    /* We used many async tasks so there is concurrent**/
    public static HashMap<Player, String> closeReasonMap = new HashMap<>();
    //define randomStringSet
    String randomSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz!@#%&*()_+";
    String randomString = "";
    Random randomItemSet = new Random();
    Random howManyRandom = new Random();
    private boolean isPacketListenersActive = false;

    public GuiCaptchaHandler() {
    }

    private StringBuilder sb;
    private final List<String> whiteList = AuthMe.settings.getProperty(SecuritySettings.GUI_CAPTCHA_COUNTRY_WHITELIST);

    private boolean isBedrockPlayer(UUID uuid) {
        return settings.getProperty(HooksSettings.HOOK_FLOODGATE_PLAYER) && settings.getProperty(SecuritySettings.GUI_CAPTCHA_BE_COMPATIBILITY) && org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgateId(uuid) && getServer().getPluginManager().getPlugin("floodgate") != null;
    }


    private void initializePacketListeners() {
        if (!isPacketListenersActive) {
            ProtocolLibrary.getProtocolManager().addPacketListener(windowPacketListener);
            ProtocolLibrary.getProtocolManager().addPacketListener(chatPacketListener);
            ProtocolLibrary.getProtocolManager().addPacketListener(chunkPacketListener);
            isPacketListenersActive = true;
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack currentItem = event.getCurrentItem();
            if (!authmeApi.isRegistered(player.getName()) && !closeReasonMap.containsKey(player)) {
                if (isBedrockPlayer(player.getUniqueId())) {
                    return;
                }
                if (currentItem != null && currentItem.getType().equals(Material.REDSTONE_BLOCK)) {
                    event.setCancelled(true);
                    if (!closeReasonMap.containsKey(player)) {
                        closeReasonMap.put(player, "verified");
                    }
                    player.closeInventory();
                    messages.send(player, MessageKey.GUI_CAPTCHA_VERIFIED);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        bukkitService.runTaskAsynchronously(() -> {
            sb = new StringBuilder();
            int howLongIsRandomString = (howManyRandom.nextInt(3) + 1);
            for (int i = 0; i < howLongIsRandomString; i++) {
                //生成随机索引号
                int index = randomItemSet.nextInt(randomSet.length());

                // 从字符串中获取由索引 index 指定的字符
                char randomChar = randomSet.charAt(index);

                // 将字符追加到字符串生成器
                sb.append(randomChar);
            }
            if (!whiteList.isEmpty()) {
                String ip = getPlayerIp(player);
                if (whiteList.contains(authmeApi.getCountryCode(ip)) && ip != null) {
                    if (!closeReasonMap.containsKey(player)) {
                        closeReasonMap.put(player, "verified:whitelist");
                    }
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        randomString = "";
        Player playerunreg = event.getPlayer();
        String name = playerunreg.getName();
        if (!authmeApi.isRegistered(name) && !isNpc(playerunreg) && !closeReasonMap.containsKey(playerunreg)) {
            if (isBedrockPlayer(playerunreg.getUniqueId())) {
                if (!closeReasonMap.containsKey(playerunreg)) {
                    closeReasonMap.put(playerunreg, "verified:bedrock");
                }
                messages.send(playerunreg, MessageKey.GUI_CAPTCHA_VERIFIED_AUTO_BEDROCK);
                return;
            }
            bukkitService.runTaskAsynchronously(() -> {
                bukkitService.runTask(() -> {
                    randomString = sb.toString();
                    Random random_blockpos = new Random();
                    AtomicInteger random_num = new AtomicInteger(random_blockpos.nextInt(26));
                    Inventory menu = Bukkit.createInventory(playerunreg, 27, messages.retrieveSingle(playerunreg, MessageKey.GUI_CAPTCHA_WINDOW_NAME, randomString));
                    ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    try {
                        if (meta != null) {
                            meta.setDisplayName(messages.retrieveSingle(playerunreg, MessageKey.GUI_CAPTCHA_CLICKABLE_NAME, randomString));
                            item.setItemMeta(meta);
                        }
                    } catch (NullPointerException e) {
                        getLogger().log(Level.WARNING, "Unexpected error occurred while setting item meta.");
                    }
                    chunkPacketListener = new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.MAP_CHUNK) {
                        @Override
                        public void onPacketSending(PacketEvent event) {
                            // 获取数据包的接收者（玩家）
                            Player packetPlayer = event.getPlayer();
                            if (!closeReasonMap.containsKey(packetPlayer) && !authmeApi.isRegistered(packetPlayer.getName())) {
                                event.setCancelled(true);
                            }
                        }
                    };
                    windowPacketListener = new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.CLOSE_WINDOW) {
                        @Override
                        public void onPacketReceiving(PacketEvent event) {
                            Player packetPlayer = event.getPlayer();
                            if (!closeReasonMap.containsKey(packetPlayer) && !authmeApi.isRegistered(packetPlayer.getName())) {
                                if (timesLeft <= 0) {
                                    bukkitService.runTask(() -> {
                                        packetPlayer.kickPlayer(service.retrieveSingleMessage(packetPlayer, MessageKey.GUI_CAPTCHA_KICK_FAILED));
                                    });
                                    timesLeft = 3;
                                } else {
                                    --timesLeft;
                                    if (timesLeft <= 0) {
                                        bukkitService.runTask(() -> {
                                            packetPlayer.kickPlayer(service.retrieveSingleMessage(packetPlayer, MessageKey.GUI_CAPTCHA_KICK_FAILED));
                                        });
                                        timesLeft = 3;
                                        return;
                                    }
                                    messages.send(packetPlayer, MessageKey.GUI_CAPTCHA_RETRY_MESSAGE, String.valueOf(timesLeft));
                                    event.setCancelled(true);
                                    random_num.set(random_blockpos.nextInt(26));
                                    bukkitService.runTask(() -> {
                                        menu.clear();
                                        menu.setItem(random_num.get(), item);
                                        packetPlayer.openInventory(menu);
                                    });
                                }
                            }
                        }
                    };
                    chatPacketListener = new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.CHAT) {
                        @Override
                        public void onPacketReceiving(PacketEvent event) {
                            Player packetPlayer = event.getPlayer();
                            if (!closeReasonMap.containsKey(packetPlayer) && !authmeApi.isRegistered(packetPlayer.getName())) {
                                messages.send(packetPlayer, MessageKey.GUI_CAPTCHA_DENIED_MESSAGE);
                                event.setCancelled(true);
                            }
                        }
                    };
                    initializePacketListeners();
                    //Open captcha inventory
                    menu.setItem(random_num.get(), item);
                    playerunreg.openInventory(menu);
                    if (settings.getProperty(SecuritySettings.GUI_CAPTCHA_TIMEOUT) > 0) {
                        long timeOut = settings.getProperty(SecuritySettings.GUI_CAPTCHA_TIMEOUT);
                        if (settings.getProperty(SecuritySettings.GUI_CAPTCHA_TIMEOUT) > settings.getProperty(RestrictionSettings.TIMEOUT)) {
                            bukkitService.runTask(() -> {
                                getLogger().warning("AuthMe detected that your GUI captcha timeout seconds(" + settings.getProperty(SecuritySettings.GUI_CAPTCHA_TIMEOUT) + ") is bigger than the Login timeout seconds(" +
                                    settings.getProperty(RestrictionSettings.TIMEOUT) + "). To prevent issues, we will let the GUI captcha follow the Login timeout seconds, please check and modify your config.");
                            });
                            timeOut = settings.getProperty(RestrictionSettings.TIMEOUT);
                        }
                        long finalTimeOut = timeOut;
                        bukkitService.runTask(() -> {
                            bukkitService.runTaskLater(() -> {
                                if (!closeReasonMap.containsKey(playerunreg) && !authmeApi.isRegistered(playerunreg.getName())) {
                                    playerunreg.kickPlayer(service.retrieveSingleMessage(playerunreg, MessageKey.GUI_CAPTCHA_KICK_TIMEOUT));
                                    timesLeft = 3; // Reset the attempt counter
                                }
                            }, finalTimeOut * 20L);
                        });
                    }
                });
            });
        }
    }

    //This prevents players from unregistering by Admins
    @EventHandler
    public void onPlayerAuthMeLogin(LoginEvent event) {
        Player player = event.getPlayer();
        if (!closeReasonMap.containsKey(player)) {
            closeReasonMap.put(player, "verified:login");
        }
    }


    private void deletePlayerData(UUID playerUUID) {
        // 获取服务器的存储文件夹路径
        File serverFolder = Bukkit.getServer().getWorldContainer();
        String worldFolderName = settings.getProperty(SecuritySettings.DELETE_PLAYER_DATA_WORLD);
        // 构建playerdata文件夹路径
        File playerDataFolder = new File(serverFolder, File.separator + worldFolderName + File.separator + "playerdata");

        // 构建玩家数据文件路径
        File playerDataFile = new File(playerDataFolder, File.separator + playerUUID + ".dat");

        // 删除玩家数据文件
        if (playerDataFile.exists()) {
            playerDataFile.delete();
        }
    }

    private void deletePlayerStats(UUID playerUUID) {
        // 获取服务器的存储文件夹路径
        File serverFolder = Bukkit.getServer().getWorldContainer();
        String worldFolderName = settings.getProperty(SecuritySettings.DELETE_PLAYER_DATA_WORLD);
        // 构建stats文件夹路径
        File statsFolder = new File(serverFolder, File.separator + worldFolderName + File.separator + "stats");
        // 构建玩家统计数据文件路径
        File statsFile = new File(statsFolder, File.separator + playerUUID + ".json");
        // 删除玩家统计数据文件
        if (statsFile.exists()) {
            statsFile.delete();
        }
    }

    private void deleteAuthMePlayerData(UUID playerUUID) {
        File pluginFolder = plugin.getDataFolder();
        File path = new File(pluginFolder, File.separator + "playerdata" + File.separator + playerUUID);
        File dataFile = new File(path, File.separator + "data.json");
        if (dataFile.exists()) {
            dataFile.delete();
            path.delete();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        UUID playerUUID = event.getPlayer().getUniqueId();
        if (!authmeApi.isRegistered(name)) {
            if (settings.getProperty(SecuritySettings.DELETE_UNVERIFIED_PLAYER_DATA) && !closeReasonMap.containsKey(player)) {
                bukkitService.runTaskLater(() -> {
                    if (!player.isOnline()) {
                        deletePlayerData(playerUUID);
                        deletePlayerStats(playerUUID);
                        deleteAuthMePlayerData(playerUUID);
                    }
                }, 100L);
                return;
            }
            closeReasonMap.remove(player);
        }
    }
}




