package com.elmakers.mine.bukkit.magic.listener;

import com.elmakers.mine.bukkit.magic.Mage;
import com.elmakers.mine.bukkit.magic.MagicController;
import com.forgottenrunes.levels.event.LevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LevelListener implements Listener {
    private final MagicController controller;

    public LevelListener(MagicController controller) {
        this.controller = controller;
    }

    @EventHandler
    public void onLevel(LevelUpEvent e) {
        Mage m = controller.getMage(e.getPlayer());
        m.upgradeCheck();
    }
}
