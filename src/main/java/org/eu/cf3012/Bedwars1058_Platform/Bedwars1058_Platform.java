package org.eu.cf3012.Bedwars1058_Platform;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class Bedwars1058_Platform extends JavaPlugin implements Listener {
    private final Map<String, Long> cooldowns = new HashMap<>();
    private Material rescuePlatformItemType;
    private int cooldownTime;
    private final Map<Location, BlockState> originalBlocks = new HashMap<>();

    private void Logger(String raw, Integer level) {
        String message = getConfig().getString("messages.prefix", "[Bedwars1058_Platform] ") + raw;
        if (level == 0) {
            getLogger().info(message);
        } else if (level == 1) {
            getLogger().warning(message);
        } else if (level == 2) {
            getLogger().severe(message);
        } else {
            getLogger().info(message);
        }
    }

    @Override
    public void onEnable() {
        if (!pluginlistener()) {
            Logger(getConfig().getString("console.no_depend", "插件依赖 BedWars1058 未安装, 插件已禁用! "), 2);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        String itemMaterialName = getConfig().getString("platform.item.material", "BLAZE_ROD");
        rescuePlatformItemType = Material.getMaterial(itemMaterialName);
        if (rescuePlatformItemType == null) {
            Logger(getConfig().getString("console.invalid_item", "配置文件中的物品类型无法使用,请检查物品类型后重启服务器! "), 1);
            rescuePlatformItemType = Material.BLAZE_ROD;
        }

        cooldownTime = getConfig().getInt("platform.cooldown", 15);

    }

    private boolean pluginlistener() {
        return Bukkit.getPluginManager().getPlugin("BedWars1058") != null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!event.hasItem()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() == rescuePlatformItemType) {
            Player player = event.getPlayer();
            String playerName = player.getName();
            long currentTime = System.currentTimeMillis();

            if (cooldowns.containsKey(playerName) && (currentTime - cooldowns.get(playerName) < cooldownTime * 1000L)) {
                long remainingTime = cooldownTime * 1000L - (currentTime - cooldowns.get(playerName));
                player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.cooldown")).replace("%time%", String.valueOf(remainingTime / 1000)));
                return;
            }
            createRescuePlatform(player);
            cooldowns.put(playerName, currentTime);

            removeItem(player, item);
        }
    }

    private void createRescuePlatform(Player player) {
        Location location = player.getLocation();

        if (location.getBlock().getType() != Material.AIR) {
            player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.no_air", "§c你没有足够的空间放置平台! ")));
            return;
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location blockLocation = location.clone().add(x, -1, z);
                Block block = blockLocation.getBlock();

                originalBlocks.put(blockLocation, block.getState());

                block.setType(Material.SLIME_BLOCK);
            }
        }

        player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.created")));

        Bukkit.getScheduler().runTaskLater(this, () -> removeRescuePlatform(location), 300L);
    }

    private void removeItem(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }
    }

    private void removeRescuePlatform(Location location) {
        for (Location blockLocation : originalBlocks.keySet()) {
            BlockState originalBlockState = originalBlocks.get(blockLocation);
            blockLocation.getBlock().setType(originalBlockState.getType());
        }

        originalBlocks.clear();
        Bukkit.getScheduler().runTask(this, () -> {
            // 移除后消息
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.removed")));
            }
        });
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Location playerLocation = player.getLocation();
            Material blockType = playerLocation.add(0, -1, 0).getBlock().getType();
            if (blockType == Material.SLIME_BLOCK) {
                event.setCancelled(true);
                player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.damage_protection")));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.SLIME_BLOCK) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Objects.requireNonNull(getConfig().getString("messages.block_protection")));
        }
    }
}
