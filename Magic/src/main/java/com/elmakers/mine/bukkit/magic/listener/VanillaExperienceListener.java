package com.elmakers.mine.bukkit.magic.listener;

import com.elmakers.mine.bukkit.api.event.EarnEvent;
import com.elmakers.mine.bukkit.api.magic.CasterProperties;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageContext;
import com.elmakers.mine.bukkit.api.magic.ProgressionPath;
import com.elmakers.mine.bukkit.item.Cost;
import com.elmakers.mine.bukkit.magic.MagicController;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class VanillaExperienceListener implements Listener {
    private final MagicController controller;
    private final double scaleFactor;

    public VanillaExperienceListener(MagicController controller, Configuration mainConfig) {
        this.controller = controller;
        this.scaleFactor = mainConfig.getDouble("experience-factors.vanilla-exp");
    }

    @EventHandler
    public void onPreXpGain(PlayerExpChangeEvent e) {
        if (e.getAmount() > 0) {
            Mage mage = controller.getMage(e.getPlayer());
            MageContext context = mage.getContext();
            CasterProperties activeProperties = context.getActiveProperties();
            ProgressionPath path = activeProperties.getPath();
            Cost earns = new Cost(controller, "sp", e.getAmount() * scaleFactor);

            if (path != null && path.earnsSP() && controller.isSPEnabled() && controller.isSPEarnEnabled()) {
                int scaledEarn = earns.getRoundedAmount();
                if (scaledEarn > 0) {
                    Cost earnCost = new Cost(earns);
                    earnCost.setAmount(Math.floor(mage.getEarnMultiplier("sp") * scaledEarn));

                    EarnEvent event = new EarnEvent(mage, earns.getType(), earnCost.getAmount(), EarnEvent.EarnCause.VANILLA_EXP);
                    Bukkit.getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        earnCost.give(mage, activeProperties);
                    }

                }
            }
        }
    }
}
