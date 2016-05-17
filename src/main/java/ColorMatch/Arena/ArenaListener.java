package main.java.ColorMatch.Arena;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockSignPost;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.SignChangeEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.TextFormat;
import main.java.ColorMatch.ColorMatch;
import main.java.ColorMatch.Utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class ArenaListener implements Listener {

    private Arena plugin;

    public ArenaListener(Arena plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        if (plugin.inArena(p)) {
            plugin.removeFromArena(p);
        } else if (plugin.isSpectator(p)) {
            plugin.removeSpectator(p);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e) {
        int action = e.getAction();

        if (e.isCancelled()) {
            return;
        }

        if (action != PlayerInteractEvent.RIGHT_CLICK_BLOCK && action != PlayerInteractEvent.LEFT_CLICK_BLOCK) {
            return;
        }

        Player p = e.getPlayer();
        Block b = e.getBlock();

        if (plugin.inArena(p)) {
            e.setCancelled();
            return;
        }

        if (plugin.isSpectator(p)) {
            e.setCancelled();

            if (b instanceof BlockSignPost) {
                BlockEntitySign sign = (BlockEntitySign) b.getLevel().getBlockEntity(b);

                if (sign == null) {
                    return;
                }

                String line1 = sign.getText()[0];
                String line2 = sign.getText()[1];

                if (TextFormat.clean(line1.toLowerCase()).trim().equals("[cm]") && TextFormat.clean(line2.toLowerCase().trim()).equals("leave")) {
                    plugin.removeSpectator(p);
                }
            }
            return;
        }

        if (b instanceof BlockSignPost) {
            BlockEntitySign sign = (BlockEntitySign) b.getLevel().getBlockEntity(b);

            if (sign == null) {
                return;
            }

            String line1 = sign.getText()[0];

            boolean isJoinSign = b.equals(plugin.getJoinSign());

            if (TextFormat.clean(line1.toLowerCase()).trim().equals("[cm]") || isJoinSign) {
                e.setCancelled();

                if (!p.hasPermission("colormatch.sign.use")) {
                    p.sendMessage("you do not have permission to perform this action");
                    return;
                }

                String name = TextFormat.clean(sign.getText()[1].trim().toLowerCase());
                if (name.equals(plugin.name) || isJoinSign) {
                    plugin.addToArena(p);
                    e.setCancelled();
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        if (plugin.inArena(p) || plugin.isSpectator(p)) {
            e.setCancelled();
            return;
        }

        if (b instanceof BlockSignPost) {
            BlockEntitySign sign = (BlockEntitySign) b.getLevel().getBlockEntity(b);

            if (sign == null) {
                return;
            }

            String line1 = sign.getText()[0];

            if (TextFormat.clean(line1.toLowerCase()).trim().equals("[cm]") || b.equals(plugin.getJoinSign())) {
                if (!p.hasPermission("colormatch.sign.break")) {
                    p.sendMessage("you do not have permission to perform this action");
                    e.setCancelled();
                }
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();

        if (plugin.inArena(p) || plugin.isSpectator(p)) {
            e.setCancelled();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(PlayerChatEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage();

        if (e.isCancelled()) {
            return;
        }

        Set<CommandSender> recipients = e.getRecipients();
        String prefix = "";

        if (plugin.inArena(p)) {
            prefix = TextFormat.GRAY + "[" + TextFormat.GREEN + "GAME" + TextFormat.GRAY + "] " + TextFormat.WHITE + TextFormat.RESET;
            /*String lastColor = "f";

            if(Utils.getLastColor(msg).toLowerCase().equals("f")){
                lastColor = Utils.getLastColor(p.getDisplayName().toLowerCase());
            }

            if(e.getMessage().lastIndexOf() !p.getDisplayName().toLowerCase().trim().substring(Math.max(0, p.getDisplayName().length() - 5)).contains(TextFormat.WHITE)) {
                e.setMessage(TextFormat.GRAY + e.getMessage());
            }*/
        } else if (plugin.isSpectator(p)) {
            prefix = TextFormat.GRAY + "[" + TextFormat.YELLOW + "SPECTATOR" + TextFormat.GRAY + "] " + TextFormat.WHITE + TextFormat.RESET;
        } else {
            for (CommandSender sender : recipients) {
                if (!(sender instanceof Player)) {
                    continue;
                }

                Player s = (Player) sender;

                if (!plugin.inArena(s) && !plugin.isSpectator(s)) {
                    continue;
                }

                recipients.remove(sender);
            }
            return;
        }

        e.setCancelled();
        plugin.messageArenaPlayers(prefix + " " + p.getDisplayName() + TextFormat.DARK_AQUA + " > " + TextFormat.GRAY + e.getMessage());
    }

    private static ArrayList<Integer> allowedCauses = new ArrayList<>(Arrays.asList(EntityDamageEvent.CAUSE_VOID, EntityDamageEvent.CAUSE_FALL, EntityDamageEvent.CAUSE_FIRE, EntityDamageEvent.CAUSE_FIRE_TICK, EntityDamageEvent.CAUSE_LAVA, EntityDamageEvent.CAUSE_CONTACT));

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Entity entity = e.getEntity();
        Player p;
        int cause = e.getCause();

        if (entity instanceof Player) {
            p = (Player) entity;

            if (plugin.isSpectator(p)) {
                e.setCancelled();
                return;
            }

            if (plugin.inArena(p)) {
                if (plugin.phase == Arena.PHASE_LOBBY) {
                    e.setCancelled();
                    return;
                } else if (!allowedCauses.contains(cause)) {
                    e.setCancelled();
                    return;
                } else if (e.getFinalDamage() >= p.getHealth()) {
                    e.setCancelled();
                    plugin.onDeath(p);
                    return;
                }
            }
        }

        if (e instanceof EntityDamageByEntityEvent) {
            Player damager = (Player) ((EntityDamageByEntityEvent) e).getDamager();

            if (damager != null && (plugin.inArena(damager) || plugin.isSpectator(damager))) {
                e.setCancelled();
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();
        String line1 = e.getLine(0);

        if (TextFormat.clean(line1.toLowerCase()).trim().equals("[cm]")) {
            if (!p.hasPermission("colormatch.sign.create")) {
                e.setCancelled();
                p.sendMessage(ColorMatch.getPrefix() + TextFormat.RED + "You do not have permissions to do that");
                return;
            }

            String line2 = e.getLine(1).toLowerCase();

            if (line2.equals("leave")) {
                e.setLine(0, "");
                e.setLine(1, ColorMatch.getPrefix());
                e.setLine(2, TextFormat.GRAY + "leave");
                e.setLine(3, "");
            } else {

                Arena arena = plugin.plugin.getArena(line2);

                if (arena == null) {
                    p.sendMessage(ColorMatch.getPrefix() + TextFormat.RED + "Arena doesn't exist");
                    e.setCancelled();
                    return;
                }

                e.setLine(0, ColorMatch.getPrefix());
                e.setLine(1, TextFormat.DARK_AQUA + arena.getName().substring(0, 1).toUpperCase() + arena.getName().substring(1).toLowerCase());
                e.setLine(2, TextFormat.BLUE + arena.getTypeString(arena.getType()));
                e.setLine(3, "");
            }

            p.sendMessage(ColorMatch.getPrefix() + TextFormat.GREEN + "Sign successfully created");
        }
    }

    @EventHandler
    public void onFoodChange(PlayerFoodLevelChangeEvent e) {
        Player p = e.getPlayer();

        if (plugin.inArena(p) || plugin.isSpectator(p)) {
            e.setCancelled();
        }
    }
}
