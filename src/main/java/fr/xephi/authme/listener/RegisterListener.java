package fr.xephi.authme.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class RegisterListener implements Listener {
    private final AuthMeApi authmeApi = AuthMeApi.getInstance();
    private final Plugin plugin;
    public static boolean enabled=true;
    // 创建一个储存玩家关闭箱子GUI的原因的HashMap
    public static HashMap<Player, String> closeReasonMap = new HashMap<>();
    // Add a timer to kick the player if they don't verify in time


    public RegisterListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            // 获取点击事件的容器
            Inventory inventory = event.getInventory();
            if (event.getView().getTitle().equals("请验证你是真人")&&!authmeApi.isRegistered(player.getName())) {
                if (Objects.requireNonNull(event.getCurrentItem()).getType().equals(Material.REDSTONE_BLOCK)) {
                    event.setCancelled(true);
                    closeReasonMap.put(player, "verified");
                    player.closeInventory();
                    player.sendMessage("§a验证完成");
                    //force to string

//                    ProtocolLibrary.getProtocolManager().removePacketListeners(this.plugin);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true,priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Random random_blockpos = new Random();
        int random_num = random_blockpos.nextInt(26);
        Inventory menu = Bukkit.createInventory(null, 27, "请验证你是真人");
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a我是真人");
        }
        item.setItemMeta(meta);
        menu.setItem(random_num, item);
        Player playerunreg = event.getPlayer();
        String name = playerunreg.getName();
        if (!authmeApi.isRegistered(name)) {
//            getLogger().log(Level.INFO, "TESTTEST");
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                playerunreg.openInventory(menu);
            }, 1L);
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.CLOSE_WINDOW) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    if (event.getPlayer() == playerunreg && !closeReasonMap.containsKey(playerunreg)) {
                        playerunreg.sendMessage("§c请先完成验证!");
                        event.setCancelled(true);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            playerunreg.openInventory(menu);
                            //playerunreg.kickPlayer("§c请先完成人机验证!");
                        });
                    }
                }
            });

            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.CHAT) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    if (event.getPlayer() == playerunreg && !closeReasonMap.containsKey(playerunreg)) {
                        playerunreg.sendMessage("§c请先完成验证!");
                        event.setCancelled(true);
                    }
                }
            });
//            chatListener = new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.CHAT) {
//                @Override
//                public void onPacketReceiving(PacketEvent event) {
//                    if (event.getPlayer() == player) {
//                        event.setCancelled(true);
//
//                    }
//                }
//            };
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        String name = player.getName();
        if (!authmeApi.isRegistered(name)){closeReasonMap.remove(player);}
    }


}


