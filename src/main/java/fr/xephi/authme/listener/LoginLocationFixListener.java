package fr.xephi.authme.listener;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class LoginLocationFixListener implements Listener{
    private final Plugin plugin;
    private final AuthMeApi authmeApi = AuthMeApi.getInstance();
    public LoginLocationFixListener(Plugin plugin) {
        this.plugin = plugin;
    }
    BlockFace[] faces = {BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST};
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!authmeApi.isRegistered(player.getName()) || !authmeApi.isAuthenticated(player)) {
            Location JoinLocation = player.getLocation().getBlock().getLocation().add(0.5, 0.1, 0.5);
            if (AuthMe.settings.getProperty(SecuritySettings.LOGIN_LOC_FIX_SUB_PORTAL)) {
                if (!JoinLocation.getBlock().getType().equals(Material.NETHER_PORTAL) && !JoinLocation.getBlock().getRelative(BlockFace.UP).getType().equals(Material.NETHER_PORTAL)) {
                    return;
                }
                Block JoinBlock = JoinLocation.getBlock();
                boolean solved = false;
                for (BlockFace face : faces) {
                    if (JoinBlock.getRelative(face).getType().equals(Material.AIR) && JoinBlock.getRelative(face).getRelative(BlockFace.UP).getType().equals(Material.AIR)) {
                        player.teleport(JoinBlock.getRelative(face).getLocation().add(0.5, 0.1, 0.5));
                        solved = true;
                        break;
                    }
                }
                if (!solved) {
                    JoinBlock.getRelative(BlockFace.UP).breakNaturally();
                    JoinBlock.breakNaturally();
                }
                player.sendMessage("§a你在登录时卡在了地狱门, 现已修正");
            } else if (AuthMe.settings.getProperty(SecuritySettings.LOGIN_LOC_FIX_SUB_UNDERGROUND)) {
                Material UpType = JoinLocation.getBlock().getRelative(BlockFace.UP).getType();
                World world = player.getWorld();
                int MaxHeight = world.getMaxHeight();
                int MinHeight = world.getMinHeight();
                if (!UpType.isOccluding() && !UpType.equals(Material.LAVA)) {
                    return;
                }
                for (int i = MinHeight; i <= MaxHeight; i++) {
                    JoinLocation.setY(i);
                    Block JoinBlock = JoinLocation.getBlock();
                    if ((JoinBlock.getRelative(BlockFace.DOWN).getType().isBlock())
                        && JoinBlock.getType().equals(Material.AIR)
                        && JoinBlock.getRelative(BlockFace.UP).getType().equals(Material.AIR)) {
                        if (JoinBlock.getRelative(BlockFace.DOWN).getType().equals(Material.LAVA)) {
                            JoinBlock.getRelative(BlockFace.DOWN).setType(Material.DIRT);
                        }
                        player.teleport(JoinBlock.getLocation().add(0.5, 0.1, 0.5));
                        player.sendMessage("§a你被埋住了, 坐标已修正, 下次下线之前请小心!");
                        break;
                    }
                    if (i == MaxHeight) {
                        player.teleport(JoinBlock.getLocation().add(0.5, 1.1, 0.5));
                        player.sendMessage("§a你被埋住了, 坐标无法修正, 只好送你去了最高点, 自求多福吧少年~");
                    }
                }
            }
        }
    }
}
