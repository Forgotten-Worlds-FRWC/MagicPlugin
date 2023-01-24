package com.elmakers.mine.bukkit.economy;

import com.elmakers.mine.bukkit.api.magic.CasterProperties;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.wand.Wand;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.geysermc.connector.common.ChatColor;

public class ManaCurrency extends BaseMagicCurrency {
    private final int refillHunger;
    private final float manaPerSwing;
    private final float manaPerBlockSprinted;

    public ManaCurrency(MageController controller, ConfigurationSection configuration) {
        super(controller, "mana", configuration);
        refillHunger = configuration.getInt("refill-hunger", 1);
        manaPerSwing = (float) configuration.getDouble("swing-mana", 10F);
        manaPerBlockSprinted = (float) configuration.getDouble("sprint-mana-per-block", 10F);
    }

    @Override
    public double getBalance(Mage mage, CasterProperties caster) {
        if (caster == null) {
            caster = mage.getActiveProperties();
        }

        return caster.getMana() + getManaFromHunger(mage, caster);
    }

    @Override
    public boolean has(Mage mage, CasterProperties caster, double amount) {
        if (caster == null) {
            caster = mage.getActiveProperties();
        }
        return caster.getMana() + getManaFromHunger(mage, caster) >= amount;
    }

    @Override
    public void deduct(Mage mage, CasterProperties caster, double amount) {
        if (caster == null) {
            caster = mage.getActiveProperties();
        }

        Player p = mage.getPlayer();
        if (p != null && caster.getMana() < amount) {
            double toPay = amount - caster.getMana();
            //int fromHunger = getManaFromHunger(mage, caster);
            int usedHunger = (int) Math.ceil(toPay / caster.getManaMax());
            int foodlevel = p.getFoodLevel();
            p.setFoodLevel(foodlevel - usedHunger);

            double leftOver = toPay % caster.getManaMax();
            caster.setMana((float) (caster.getManaMax() - leftOver));

            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1f);
            p.sendMessage(ChatColor.GREEN + "You used some food to restore your mana...");
        } else {
            caster.removeMana((float) amount);
        }

        Wand wand = mage.getActiveWand();
        if(wand == null) mage.updateActionBar();
    }

    @Override
    public boolean give(Mage mage, CasterProperties caster, double amount) {
        if (caster == null) {
            caster = mage.getActiveProperties();
        }
        if (caster.getMana() >= caster.getManaMax()) {
            return false;
        }
        float newMana = (float) Math.min(caster.getManaMax(), caster.getMana() + amount);
        caster.setMana(newMana);

        Wand wand = mage.getActiveWand();
        if(wand == null) {
            if(mage.getMana() < caster.getManaMax()) {
                mage.updateActionBar();
            } else {
                mage.resetSentExperience();
            }
        }

        return true;
    }

    public int getManaFromHunger(Mage mage, CasterProperties caster) {
        Player p = mage.getPlayer();
        if (p != null) return (p.getFoodLevel() / refillHunger) * caster.getManaMax();
        return 0;
    }

    public float getManaPerSwing() {
        return manaPerSwing;
    }

    public float getManaPerBlockSprinted() {
        return manaPerBlockSprinted;
    }
}
