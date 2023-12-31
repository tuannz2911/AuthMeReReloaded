package fr.xephi.authme.listener;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;

//This fix is only for Minecraft 1.13-
public class AdvancedShulkerFixListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispenserActivate(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block.getFace(block) == BlockFace.DOWN) {
            //If the block is in y = 0
            if (block.getY() == 0) {
                event.setCancelled(true);
            }
        }
        if (block.getFace(block) == BlockFace.UP) {
            //If the block is in y = 255
            if (block.getY() == 255) {
                event.setCancelled(true);
            }
        }
    }

    //This implementation method will be available in my another plugin
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onDispenserBePlaced(BlockPlaceEvent event){
//        Block block = event.getBlock();
//        if (block.getType().equals(Material.DISPENSER)){
//            if (block.getFace(block) == BlockFace.DOWN && block.getY() == 0){
//                event.setCancelled(true);
//            }
//            if (block.getFace(block) == BlockFace.UP && block.getY() == 255){
//                event.setCancelled(true);
//            }
//        }
//    }
}
