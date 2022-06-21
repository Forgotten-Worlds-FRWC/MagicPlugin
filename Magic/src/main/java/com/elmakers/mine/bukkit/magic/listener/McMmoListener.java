package com.elmakers.mine.bukkit.magic.listener;

import com.elmakers.mine.bukkit.action.CastContext;
import com.elmakers.mine.bukkit.api.event.EarnEvent;
import com.elmakers.mine.bukkit.api.magic.CasterProperties;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageContext;
import com.elmakers.mine.bukkit.api.magic.ProgressionPath;
import com.elmakers.mine.bukkit.item.Cost;
import com.elmakers.mine.bukkit.magic.MagicController;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerPreXpGainEvent;
import com.google.common.base.Enums;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class McMmoListener implements Listener {
    private final MagicController controller;
    private final Map<PrimarySkillType, Double> scaleFactors;

    public McMmoListener(MagicController controller, Configuration mainConfig) {
        this.controller = controller;
        this.scaleFactors = new HashMap<>();

        ConfigurationSection mcmmoSection = mainConfig.getConfigurationSection("experience-factors.mcmmo");
        if (mcmmoSection != null) {
            for (String skillKey : mcmmoSection.getKeys(false)) {
                PrimarySkillType skill = Enums.getIfPresent(PrimarySkillType.class, skillKey.toUpperCase()).orNull();
                if (skill == null) {
                    controller.getPlugin().getLogger().warning("Could not find mcmmo skill: " + skillKey);
                    continue;
                }
                scaleFactors.put(skill, mcmmoSection.getDouble(skillKey, 0.0));
            }
        }
    }

    @EventHandler
    public void onPreXpGain(McMMOPlayerPreXpGainEvent e) {
        if (e.getXpGained() > 0) {
            Mage mage = controller.getMage(e.getPlayer());
            MageContext context = mage.getContext();
            CasterProperties activeProperties = context.getActiveProperties();
            ProgressionPath path = activeProperties.getPath();
            Cost earns = new Cost(controller, "sp", e.getXpGained() * scaleFactors.get(e.getSkill()));

            if (path != null && path.earnsSP() && controller.isSPEnabled() && controller.isSPEarnEnabled()) {
                int scaledEarn = earns.getRoundedAmount();
                if (scaledEarn > 0) {
                    Cost earnCost = new Cost(earns);
                    earnCost.setAmount(Math.floor(mage.getEarnMultiplier("sp") * scaledEarn));

                    EarnEvent event = new EarnEvent(mage, earns.getType(), earnCost.getAmount(), EarnEvent.EarnCause.MCMMO_EXP);
                    Bukkit.getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        earnCost.give(mage, activeProperties);
                    }

                }
            }
        }
    }
}
