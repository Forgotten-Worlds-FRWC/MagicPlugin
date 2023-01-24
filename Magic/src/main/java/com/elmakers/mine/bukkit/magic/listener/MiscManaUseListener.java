package com.elmakers.mine.bukkit.magic.listener;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.economy.ManaCurrency;
import com.elmakers.mine.bukkit.magic.MagicController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.geysermc.connector.common.ChatColor;

public class MiscManaUseListener implements Listener {
    private final MagicController magicController;
    private final ManaCurrency manaCurrency;

    public MiscManaUseListener(MagicController magicController, ManaCurrency manaCurrency) {
        this.magicController = magicController;
        this.manaCurrency = manaCurrency;
    }

    @EventHandler
    public void onHit(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            Mage mage = magicController.getMage(e.getPlayer());
            if (manaCurrency.has(mage, manaCurrency.getManaPerSwing())) {
                manaCurrency.deduct(mage, manaCurrency.getManaPerSwing());
            } else {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "You are out of mana");
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            Mage mage = magicController.getMage(p);
            if (manaCurrency.has(mage, manaCurrency.getManaPerSwing())) {
                manaCurrency.deduct(mage, manaCurrency.getManaPerSwing());
            } else {
                e.setCancelled(true);
                p.sendMessage(ChatColor.RED + "You are out of mana");
            }
        }
    }

    @EventHandler
    public void onSprint(PlayerMoveEvent e) {
        if (e.getPlayer().isSprinting() && e.getPlayer().getVehicle() == null && !e.getPlayer().isFlying() && !e.getPlayer().isGliding()) {
            Mage mage = magicController.getMage(e.getPlayer());
            double used = manaCurrency.getManaPerBlockSprinted() * e.getFrom().toVector().setY(0).distance(e.getTo().toVector().setY(0));

            if (manaCurrency.has(mage, used)) {
                manaCurrency.deduct(mage, used);
            } else {
                e.getPlayer().setSprinting(false);
                e.getPlayer().sendMessage(ChatColor.RED + "You are out of mana");
            }
        }
    }
}
