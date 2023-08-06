package com.elmakers.mine.bukkit.api.magic;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;

import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.api.wand.Wand;

public interface CasterProperties extends MagicConfigurable {
    MageController getController();
    boolean hasSpell(String spellKey);
    boolean hasBrush(String brushKey);
    Collection<String> getSpells();
    Collection<String> getBrushes();
    boolean addSpell(String spellKey);
    boolean addBrush(String key);
    int getMaxSpells();
    // Whoops
    @Deprecated
    boolean setSpelLLevel(String spellKey, int level);
    boolean setSpellLevel(String spellKey, int level);
    int getSpellLevel(String spellKey);
    Map<String, Integer> getBrushInventory();
    Map<String, Integer> getSpellInventory();
    void updateBrushInventory(Map<String, Integer> updateBrushes);
    void updateSpellInventory(Map<String, Integer> updateSpells);
    Mage getMage();
    void removeMana(float mana);
    float getMana();
    int getManaMax();
    int getEffectiveManaMax();
    void setMana(float mana);
    @Deprecated
    void setManaMax(int manaMax);
    void setManaMax(float manaMax);
    int getManaRegeneration();
    int getEffectiveManaRegeneration();
    @Deprecated
    void setManaRegeneration(int manaRegeneration);
    void setManaRegeneration(float manaRegeneration);
    @Nullable
    ProgressionPath getPath();
    int getLevel();
    void setPath(String path);
    boolean canProgress();
    @Nullable
    Double getAttribute(String attributeKey);
    void setAttribute(String attributeKey, Double attributeValue);
    boolean addItem(ItemStack item);
    boolean add(Wand other);
    boolean upgradesAllowed();
    /**
     * Adds a spell to an otherwise locked set of properties
     */
    boolean forceAddSpell(String key);

    /**
     * Returns a Spell for a given spell key, if this caster has the spell.
     *
     * <p>Will return the correct spell level for the given spell, regardless of the level requested.
     *
     * @param spellKey The spell key to request
     * @return the Spell, or null if not present here.
     */
    @Nullable
    Spell getSpell(String spellKey);

    /**
     * Returns the SpellTemplate representing the requested spell key at the correct level.
     *
     * @param spellKey The key of the spell being requested
     * @return The SpellTemplate requested, or null if this class doesn't have the requested spell
     */
    @Nullable
    SpellTemplate getSpellTemplate(String spellKey);

    /**
     * Checks if this caster can be upgraded to the next {@link ProgressionPath} and if so upgrades it.
     *
     * @return false if the player is blocked based on a path requirement
     */
    boolean checkAndUpgrade(boolean quiet);

    boolean isCostFree();
    boolean isConsumeFree();
    boolean isCooldownFree();
    int randomize(int totalLevels, boolean addSpells);
    @Nullable
    Color getEffectColor();
    @Nullable
    String getEffectParticleName();
    void sendMessageKey(String messageKey, String... parameters);
    void checkUpgrade();
}
