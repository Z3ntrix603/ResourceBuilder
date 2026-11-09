package de.resourcebuilder.category;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum PvPItem {
    CRYSTALS("Crystals", "item/end_crystal"),
    RESPAWN_ANCHOR("Respawn Anchor", "block/respawn_anchor_top_off"),
    GLOWSTONE("Glowstone", "block/glowstone"),
    OBSIDIAN("Obsidian", "block/obsidian"),
    CRYING_OBSIDIAN("Crying Obsidian", "block/crying_obsidian"),
    TOTEM("Totem of Undying", "item/totem_of_undying"),
    GOLDEN_APPLE("Golden Apple", "item/golden_apple"),
    ENCHANTED_GOLDEN_APPLE("Enchanted Gapple", "item/enchanted_golden_apple"),
    ENDER_PEARL("Ender Pearl", "item/ender_pearl"),
    COBWEB("Cobweb", "block/cobweb"),
    TNT("TNT", "block/tnt_side"),
    FIRE("Fire", "block/fire_0"),
    WIND_CHARGE("Wind Charge", "item/wind_charge"),
    FIREWORK_ROCKET("Rockets", "item/firework_rocket"),
    SHIELD("Shield", "item/shield"),
    DIAMOND_SWORD("Diamond Sword", "item/diamond_sword"),
    NETHERITE_SWORD("Netherite Sword", "item/netherite_sword"),
    MACE("Mace", "item/mace"),
    DIAMOND_AXE("Diamond Axe", "item/diamond_axe"),
    NETHERITE_AXE("Netherite Axe", "item/netherite_axe"),
    DIAMOND_PICKAXE("Diamond Pickaxe", "item/diamond_pickaxe"),
    NETHERITE_PICKAXE("Netherite Pickaxe", "item/netherite_pickaxe"),
    BOW("Bow", "item/bow"),
    CROSSBOW("Crossbow", "item/crossbow_standby"),
    POTION("Potion", "item/potion"),
    SPLASH_POTION("Splash Potion", "item/splash_potion"),
    DIAMOND_ARMOR("Diamond Armor", "item/diamond_chestplate"),
    NETHERITE_ARMOR("Netherite Armor", "item/netherite_chestplate"),
    ELYTRA("Elytra", "item/elytra"),
    CRIT_PARTICLES("Crit Particles", "particle/critical_hit"),
    CRYSTAL_EXPLOSION("Crystal Explosion", "particle/explosion_0"),
    TOTEM_POPS("Totem Pop", "particle/glitter_0"),
    SOUND_MACE("Mace Hits Sound", "sounds/item/mace", true),
    SOUND_CRYSTAL("Crystal Blow Sound", "sounds/random/explode", true),
    SOUND_SWORD("Sword Hits Sound", "sounds/entity/player/attack", true),
    SOUND_EAT("Gapple Essen Sound", "sounds/random/eat", true),
    SOUND_TOTEM("Totem Pop Sound", "sounds/item/totem", true);

    private final String displayName;
    private final String primaryTexturePath;
    private final boolean sound;

    PvPItem(String displayName, String primaryTexturePath) {
        this(displayName, primaryTexturePath, false);
    }

    PvPItem(String displayName, String primaryTexturePath, boolean sound) {
        this.displayName = displayName;
        this.primaryTexturePath = primaryTexturePath;
        this.sound = sound;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrimaryTexturePath() {
        return primaryTexturePath;
    }

    public boolean isSound() {
        return sound;
    }

    public ItemStack getItemStack() {
        return switch (this) {
            case CRYSTALS -> Items.END_CRYSTAL.getDefaultInstance();
            case RESPAWN_ANCHOR -> Items.RESPAWN_ANCHOR.getDefaultInstance();
            case GLOWSTONE -> Items.GLOWSTONE.getDefaultInstance();
            case OBSIDIAN -> Items.OBSIDIAN.getDefaultInstance();
            case CRYING_OBSIDIAN -> Items.CRYING_OBSIDIAN.getDefaultInstance();
            case TOTEM -> Items.TOTEM_OF_UNDYING.getDefaultInstance();
            case GOLDEN_APPLE -> Items.GOLDEN_APPLE.getDefaultInstance();
            case ENCHANTED_GOLDEN_APPLE -> Items.ENCHANTED_GOLDEN_APPLE.getDefaultInstance();
            case ENDER_PEARL -> Items.ENDER_PEARL.getDefaultInstance();
            case COBWEB -> Items.COBWEB.getDefaultInstance();
            case TNT -> Items.TNT.getDefaultInstance();
            case FIRE -> Items.FIRE_CHARGE.getDefaultInstance();
            case WIND_CHARGE -> Items.WIND_CHARGE.getDefaultInstance();
            case FIREWORK_ROCKET -> Items.FIREWORK_ROCKET.getDefaultInstance();
            case SHIELD -> Items.SHIELD.getDefaultInstance();
            case DIAMOND_SWORD -> Items.DIAMOND_SWORD.getDefaultInstance();
            case NETHERITE_SWORD -> Items.NETHERITE_SWORD.getDefaultInstance();
            case MACE -> Items.MACE.getDefaultInstance();
            case DIAMOND_AXE -> Items.DIAMOND_AXE.getDefaultInstance();
            case NETHERITE_AXE -> Items.NETHERITE_AXE.getDefaultInstance();
            case DIAMOND_PICKAXE -> Items.DIAMOND_PICKAXE.getDefaultInstance();
            case NETHERITE_PICKAXE -> Items.NETHERITE_PICKAXE.getDefaultInstance();
            case BOW -> Items.BOW.getDefaultInstance();
            case CROSSBOW -> Items.CROSSBOW.getDefaultInstance();
            case POTION -> Items.POTION.getDefaultInstance();
            case SPLASH_POTION -> Items.SPLASH_POTION.getDefaultInstance();
            case DIAMOND_ARMOR -> Items.DIAMOND_CHESTPLATE.getDefaultInstance();
            case NETHERITE_ARMOR -> Items.NETHERITE_CHESTPLATE.getDefaultInstance();
            case ELYTRA -> Items.ELYTRA.getDefaultInstance();
            case CRIT_PARTICLES -> Items.NETHERITE_SWORD.getDefaultInstance();
            case CRYSTAL_EXPLOSION -> Items.TNT.getDefaultInstance();
            case TOTEM_POPS -> Items.TOTEM_OF_UNDYING.getDefaultInstance();
            case SOUND_MACE -> Items.MACE.getDefaultInstance();
            case SOUND_CRYSTAL -> Items.END_CRYSTAL.getDefaultInstance();
            case SOUND_SWORD -> Items.DIAMOND_SWORD.getDefaultInstance();
            case SOUND_EAT -> Items.ENCHANTED_GOLDEN_APPLE.getDefaultInstance();
            case SOUND_TOTEM -> Items.TOTEM_OF_UNDYING.getDefaultInstance();
        };
    }
}
