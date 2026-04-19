package com.insideblock.dimension;

import com.insideblock.InsideBlockMod;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class InsideBlockDimension {

    public static final RegistryKey<World> WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            new Identifier(InsideBlockMod.MOD_ID, "inside_dimension")
    );

    private static final int ROOM_SPACING = 300;
    private static final int ROOM_Y = 5;
    private static final Set<UUID> BUILT_ROOMS = new HashSet<>();

    // ── Teleport in ──────────────────────────────────────────────────────────

    public static void teleportInside(ServerPlayerEntity player, String roomName, ServerWorld fromWorld) {
        ServerWorld dim = player.getServer().getWorld(WORLD_KEY);
        if (dim == null) {
            player.sendMessage(Text.literal("§cError: pocket dimension not loaded!"), false);
            InsideBlockMod.LOGGER.error("Inside dimension missing — check dimension JSON files.");
            return;
        }

        storeReturn(player, fromWorld, player.getBlockPos(), player.getYaw());

        int slot = Math.abs(player.getUuid().hashCode() % 1000);
        BlockPos origin = new BlockPos(slot * ROOM_SPACING, ROOM_Y, 0);

        String roomKey = player.getUuidAsString() + ":" + roomName;
        if (!BUILT_ROOMS.contains(UUID.nameUUIDFromBytes(roomKey.getBytes()))) {
            buildRoom(dim, origin, roomName);
            BUILT_ROOMS.add(UUID.nameUUIDFromBytes(roomKey.getBytes()));
        }

        BlockPos spawn = origin.add(5, 1, 5);
        player.teleport(dim, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYaw(), 0f);
    }

    // ── Room builder ─────────────────────────────────────────────────────────

    private static void buildRoom(ServerWorld world, BlockPos origin, String roomName) {
        // Try to load NBT structure first
        StructureTemplateManager mgr = world.getStructureTemplateManager();
        Optional<StructureTemplate> tmpl = mgr.getTemplate(new Identifier(InsideBlockMod.MOD_ID, roomName));
        if (tmpl.isEmpty()) tmpl = mgr.getTemplate(new Identifier(InsideBlockMod.MOD_ID, "default_room"));

        if (tmpl.isPresent()) {
            tmpl.get().place(world, origin, origin, new StructurePlacementData(), world.getRandom(), 2);
            fillChests(world, origin, roomName);
            return;
        }

        // Fallback: procedural room
        proceduralRoom(world, origin, roomName);
        fillChests(world, origin, roomName);
    }

    private static void proceduralRoom(ServerWorld world, BlockPos o, String roomName) {
        int W=13, H=8, D=13;

        // Choose wall/floor/ceil blocks by room type
        net.minecraft.block.Block wall  = wallBlock(roomName);
        net.minecraft.block.Block floor = floorBlock(roomName);
        net.minecraft.block.Block ceil  = ceilBlock(roomName);
        net.minecraft.block.Block light = lightBlock(roomName);

        for (int x=0;x<W;x++) for (int y=0;y<H;y++) for (int z=0;z<D;z++) {
            BlockPos p = o.add(x,y,z);
            boolean border = x==0||x==W-1||y==0||y==H-1||z==0||z==D-1;
            if (border) {
                net.minecraft.block.Block b = y==0 ? floor : (y==H-1 ? ceil : wall);
                world.setBlockState(p, b.getDefaultState());
            } else {
                world.setBlockState(p, Blocks.AIR.getDefaultState());
            }
        }
        // Glowstone lights on ceiling
        for (int[] lp : new int[][]{{2,H-1,2},{10,H-1,2},{2,H-1,10},{10,H-1,10},{6,H-1,6}})
            world.setBlockState(o.add(lp[0],lp[1],lp[2]), light.getDefaultState());

        // Decorative pillars in corners
        for (int[] pp : new int[][]{{2,1,2},{10,1,2},{2,1,10},{10,1,10}}) {
            for (int py=1;py<=3;py++)
                world.setBlockState(o.add(pp[0],py,pp[1]), wall.getDefaultState());
        }

        // Return pressure plate (warped = purple)
        world.setBlockState(o.add(6,1,6), Blocks.WARPED_PRESSURE_PLATE.getDefaultState());

        // 5 chests spread around the room
        int[][] chestPositions = {{3,1,3},{9,1,3},{3,1,9},{9,1,9},{6,1,3}};
        for (int[] cp : chestPositions)
            world.setBlockState(o.add(cp[0],cp[1],cp[2]), Blocks.CHEST.getDefaultState());
    }

    // ── Room theme helpers ────────────────────────────────────────────────────

    private static net.minecraft.block.Block wallBlock(String room) {
        return switch(room) {
            case "diamond_room"    -> Blocks.DIAMOND_BLOCK;
            case "iron_room"       -> Blocks.IRON_BLOCK;
            case "gold_room"       -> Blocks.GOLD_BLOCK;
            case "emerald_room"    -> Blocks.EMERALD_BLOCK;
            case "netherite_room"  -> Blocks.NETHERITE_BLOCK;
            case "debris_room"     -> Blocks.POLISHED_BLACKSTONE_BRICKS;
            case "nether_room",
                 "magma_room"      -> Blocks.NETHER_BRICKS;
            case "end_room"        -> Blocks.END_STONE_BRICKS;
            case "obsidian_room"   -> Blocks.OBSIDIAN;
            case "bedrock_room"    -> Blocks.BEDROCK;
            case "enchanting_room",
                 "library_room"    -> Blocks.BOOKSHELF;
            case "wood_room"       -> Blocks.OAK_PLANKS;
            case "crafting_room"   -> Blocks.OAK_PLANKS;
            case "grass_room",
                 "dirt_room"       -> Blocks.DIRT;
            case "dragon_room"     -> Blocks.PURPUR_BLOCK;
            case "beacon_room"     -> Blocks.SEA_LANTERN;
            case "ender_room"      -> Blocks.CRYING_OBSIDIAN;
            case "spawner_room"    -> Blocks.MOSSY_COBBLESTONE;
            default                -> Blocks.STONE_BRICKS;
        };
    }

    private static net.minecraft.block.Block floorBlock(String room) {
        return switch(room) {
            case "grass_room"      -> Blocks.GRASS_BLOCK;
            case "dirt_room"       -> Blocks.DIRT;
            case "sand_room"       -> Blocks.SAND;
            case "gravel_room"     -> Blocks.GRAVEL;
            case "nether_room"     -> Blocks.NETHERRACK;
            case "magma_room"      -> Blocks.MAGMA_BLOCK;
            case "end_room"        -> Blocks.END_STONE;
            case "wood_room"       -> Blocks.OAK_PLANKS;
            case "obsidian_room"   -> Blocks.OBSIDIAN;
            case "bedrock_room"    -> Blocks.BEDROCK;
            case "dragon_room"     -> Blocks.PURPUR_PILLAR;
            case "beacon_room"     -> Blocks.GLASS;
            default                -> Blocks.STONE_BRICKS;
        };
    }

    private static net.minecraft.block.Block ceilBlock(String room) {
        return switch(room) {
            case "magma_room"      -> Blocks.MAGMA_BLOCK;
            case "nether_room"     -> Blocks.NETHER_BRICKS;
            case "end_room"        -> Blocks.END_STONE_BRICKS;
            case "bedrock_room"    -> Blocks.BEDROCK;
            case "obsidian_room"   -> Blocks.CRYING_OBSIDIAN;
            case "dragon_room"     -> Blocks.PURPUR_BLOCK;
            case "ender_room"      -> Blocks.OBSIDIAN;
            default                -> wallBlock(room);
        };
    }

    private static net.minecraft.block.Block lightBlock(String room) {
        return switch(room) {
            case "end_room",
                 "beacon_room"     -> Blocks.SEA_LANTERN;
            case "nether_room",
                 "magma_room",
                 "debris_room",
                 "netherite_room"  -> Blocks.SHROOMLIGHT;
            case "enchanting_room",
                 "ender_room"      -> Blocks.CRYING_OBSIDIAN;
            case "diamond_room",
                 "water_room"      -> Blocks.SEA_LANTERN;
            default                -> Blocks.GLOWSTONE;
        };
    }

    // ── Chest loot ────────────────────────────────────────────────────────────

    private static void fillChests(ServerWorld world, BlockPos origin, String roomName) {
        int W=13, D=13;
        // Scan the 13x13 footprint for chests and fill them
        for (int x=0;x<W;x++) for (int z=0;z<D;z++) {
            for (int y=1;y<=3;y++) {
                BlockPos p = origin.add(x,y,z);
                BlockEntity be = world.getBlockEntity(p);
                if (be instanceof ChestBlockEntity chest) {
                    fillChestForRoom(chest, roomName, world.getRandom());
                }
            }
        }
    }

    private static void fillChestForRoom(ChestBlockEntity chest, String roomName, net.minecraft.util.math.random.Random rng) {
        // Every room gets a base set of cool items
        List<ItemStack> loot = new ArrayList<>();
        loot.addAll(baseLoot(rng));
        loot.addAll(roomSpecificLoot(roomName, rng));

        // Place items in chest slots
        int slot = 0;
        for (ItemStack stack : loot) {
            if (slot >= chest.size()) break;
            chest.setStack(slot++, stack);
        }
    }

    private static List<ItemStack> baseLoot(net.minecraft.util.math.random.Random rng) {
        List<ItemStack> items = new ArrayList<>();

        // OP Sword — Sharpness X, Fire Aspect III, Looting IV, Unbreaking V
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        enchant(sword, Enchantments.SHARPNESS, 10);
        enchant(sword, Enchantments.FIRE_ASPECT, 3);
        enchant(sword, Enchantments.LOOTING, 4);
        enchant(sword, Enchantments.UNBREAKING, 5);
        enchant(sword, Enchantments.MENDING, 1);
        sword.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§5§lSoul Reaper"));
        items.add(sword);

        // Mace — Density VII, Breach V
        ItemStack mace = new ItemStack(Items.MACE);
        enchant(mace, Enchantments.DENSITY, 7);
        enchant(mace, Enchantments.BREACH, 5);
        enchant(mace, Enchantments.UNBREAKING, 5);
        enchant(mace, Enchantments.MENDING, 1);
        mace.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§6§lThe Crusher"));
        items.add(mace);

        // OP Bow — Power X, Infinity, Flame II, Punch III
        ItemStack bow = new ItemStack(Items.BOW);
        enchant(bow, Enchantments.POWER, 10);
        enchant(bow, Enchantments.INFINITY, 1);
        enchant(bow, Enchantments.FLAME, 2);
        enchant(bow, Enchantments.PUNCH, 3);
        enchant(bow, Enchantments.UNBREAKING, 5);
        bow.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§b§lStormbow"));
        items.add(bow);

        // OP Pickaxe — Efficiency X, Fortune V, Unbreaking V, Mending
        ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
        enchant(pick, Enchantments.EFFICIENCY, 10);
        enchant(pick, Enchantments.FORTUNE, 5);
        enchant(pick, Enchantments.UNBREAKING, 5);
        enchant(pick, Enchantments.MENDING, 1);
        pick.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§a§lVein Splitter"));
        items.add(pick);

        // OP Armour — Protection VII, Unbreaking V, Mending
        for (Item armorPiece : new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
                                           Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}) {
            ItemStack armor = new ItemStack(armorPiece);
            enchant(armor, Enchantments.PROTECTION, 7);
            enchant(armor, Enchantments.UNBREAKING, 5);
            enchant(armor, Enchantments.MENDING, 1);
            if (armorPiece == Items.NETHERITE_BOOTS) {
                enchant(armor, Enchantments.FEATHER_FALLING, 10);
                enchant(armor, Enchantments.DEPTH_STRIDER, 5);
            }
            if (armorPiece == Items.NETHERITE_CHESTPLATE) {
                enchant(armor, Enchantments.THORNS, 5);
            }
            items.add(armor);
        }

        // Totem of Undying x2
        items.add(new ItemStack(Items.TOTEM_OF_UNDYING, 2));

        // Golden apples
        items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 5));

        return items;
    }

    private static List<ItemStack> roomSpecificLoot(String room, net.minecraft.util.math.random.Random rng) {
        List<ItemStack> items = new ArrayList<>();
        switch (room) {
            case "diamond_room" -> {
                items.add(new ItemStack(Items.DIAMOND, 32));
                items.add(new ItemStack(Items.DIAMOND_BLOCK, 5));
                ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
                enchant(axe, Enchantments.SHARPNESS, 10);
                enchant(axe, Enchantments.EFFICIENCY, 8);
                enchant(axe, Enchantments.UNBREAKING, 5);
                axe.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§b§lDiamond Fury"));
                items.add(axe);
            }
            case "netherite_room", "debris_room" -> {
                items.add(new ItemStack(Items.NETHERITE_INGOT, 8));
                items.add(new ItemStack(Items.ANCIENT_DEBRIS, 10));
                items.add(new ItemStack(Items.NETHERITE_BLOCK, 2));
            }
            case "gold_room" -> {
                items.add(new ItemStack(Items.GOLD_INGOT, 32));
                items.add(new ItemStack(Items.GOLDEN_APPLE, 10));
                items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 3));
            }
            case "iron_room" -> {
                items.add(new ItemStack(Items.IRON_INGOT, 64));
                items.add(new ItemStack(Items.IRON_BLOCK, 8));
            }
            case "emerald_room" -> {
                items.add(new ItemStack(Items.EMERALD, 64));
                items.add(new ItemStack(Items.EMERALD_BLOCK, 10));
            }
            case "enchanting_room", "library_room" -> {
                items.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 16));
                items.add(new ItemStack(Items.ENCHANTED_BOOK, 3));
                items.add(new ItemStack(Items.LAPIS_LAZULI, 64));
                // OP Trident
                ItemStack trident = new ItemStack(Items.TRIDENT);
                enchant(trident, Enchantments.RIPTIDE, 5);
                enchant(trident, Enchantments.CHANNELING, 1);
                enchant(trident, Enchantments.LOYALTY, 5);
                enchant(trident, Enchantments.UNBREAKING, 5);
                trident.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§3§lStorm Lance"));
                items.add(trident);
            }
            case "end_room", "dragon_room" -> {
                items.add(new ItemStack(Items.DRAGON_BREATH, 5));
                items.add(new ItemStack(Items.ELYTRA));
                items.add(new ItemStack(Items.END_CRYSTAL, 3));
                items.add(new ItemStack(Items.CHORUS_FRUIT, 16));
                // Elytra enchanted
                ItemStack elytra = new ItemStack(Items.ELYTRA);
                enchant(elytra, Enchantments.UNBREAKING, 5);
                enchant(elytra, Enchantments.MENDING, 1);
                elytra.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§d§lDragon Wings"));
                items.add(elytra);
            }
            case "nether_room", "magma_room" -> {
                items.add(new ItemStack(Items.BLAZE_ROD, 16));
                items.add(new ItemStack(Items.GHAST_TEAR, 8));
                items.add(new ItemStack(Items.MAGMA_CREAM, 16));
                items.add(new ItemStack(Items.FIRE_CHARGE, 16));
            }
            case "bedrock_room" -> {
                // The most OP loot — it's bedrock, hardest block!
                items.add(new ItemStack(Items.NETHERITE_BLOCK, 5));
                items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 10));
                items.add(new ItemStack(Items.TOTEM_OF_UNDYING, 5));
                // God axe
                ItemStack godAxe = new ItemStack(Items.NETHERITE_AXE);
                enchant(godAxe, Enchantments.SHARPNESS, 10);
                enchant(godAxe, Enchantments.EFFICIENCY, 10);
                enchant(godAxe, Enchantments.UNBREAKING, 10);
                enchant(godAxe, Enchantments.MENDING, 1);
                enchant(godAxe, Enchantments.LOOTING, 5);
                godAxe.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§4§lBedrock Breaker"));
                items.add(godAxe);
            }
            case "beacon_room" -> {
                items.add(new ItemStack(Items.BEACON));
                items.add(new ItemStack(Items.NETHER_STAR, 2));
                items.add(new ItemStack(Items.DIAMOND_BLOCK, 9));
            }
            case "spawner_room" -> {
                items.add(new ItemStack(Items.SPAWNER));
                items.add(new ItemStack(Items.SPIDER_EYE, 16));
                items.add(new ItemStack(Items.ROTTEN_FLESH, 32));
            }
            case "wood_room", "crafting_room" -> {
                items.add(new ItemStack(Items.OAK_LOG, 64));
                items.add(new ItemStack(Items.STICK, 32));
                // Axe special
                ItemStack woodAxe = new ItemStack(Items.NETHERITE_AXE);
                enchant(woodAxe, Enchantments.EFFICIENCY, 10);
                enchant(woodAxe, Enchantments.SILK_TOUCH, 1);
                woodAxe.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§2§lForest King"));
                items.add(woodAxe);
            }
            case "grass_room", "dirt_room" -> {
                items.add(new ItemStack(Items.BONE_MEAL, 32));
                items.add(new ItemStack(Items.PUMPKIN_SEEDS, 16));
                items.add(new ItemStack(Items.MELON_SEEDS, 16));
            }
            case "coal_room" -> {
                items.add(new ItemStack(Items.COAL_BLOCK, 16));
                items.add(new ItemStack(Items.COAL, 64));
            }
            case "treasure_room", "ender_room" -> {
                // Chest inside a chest — ultimate loot
                items.add(new ItemStack(Items.DIAMOND, 64));
                items.add(new ItemStack(Items.NETHERITE_INGOT, 16));
                items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 8));
                items.add(new ItemStack(Items.TOTEM_OF_UNDYING, 3));
            }
            default -> {
                items.add(new ItemStack(Items.DIAMOND, 8));
                items.add(new ItemStack(Items.GOLD_INGOT, 16));
            }
        }
        return items;
    }

    // ── Enchantment helper (works with 1.21.1 registry) ──────────────────────

    private static void enchant(ItemStack stack, RegistryKey<Enchantment> enchKey, int level) {
        // We add to item's enchantments tag directly using NBT
        // This approach works without needing a RegistryWrapper in static context
        NbtCompound tag = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
        // Use the enchantment ID string directly
        String id = enchKey.getValue().toString();
        net.minecraft.nbt.NbtList enchList;
        if (tag.contains("Enchantments")) {
            enchList = tag.getList("Enchantments", 10);
        } else {
            enchList = new net.minecraft.nbt.NbtList();
        }
        // Check if enchantment already present, replace if so
        for (int i = 0; i < enchList.size(); i++) {
            NbtCompound e = enchList.getCompound(i);
            if (e.getString("id").equals(id)) {
                e.putInt("lvl", level);
                tag.put("Enchantments", enchList);
                stack.set(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(tag));
                return;
            }
        }
        NbtCompound entry = new NbtCompound();
        entry.putString("id", id);
        entry.putInt("lvl", level);
        enchList.add(entry);
        tag.put("Enchantments", enchList);
        stack.set(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(tag));
    }

    // ── Return home ───────────────────────────────────────────────────────────

    public static void returnPlayer(ServerPlayerEntity player) {
        ReturnData data = RETURN_MAP.remove(player.getUuid());
        if (data == null) {
            ServerWorld ow = player.getServer().getOverworld();
            BlockPos sp = ow.getSpawnPos();
            player.teleport(ow, sp.getX()+0.5, sp.getY(), sp.getZ()+0.5, 0f, 0f);
            player.sendMessage(Text.literal("§eNo return point — sent to spawn."), true);
            return;
        }
        RegistryKey<World> rk = RegistryKey.of(RegistryKeys.WORLD, new Identifier(data.dim));
        ServerWorld rw = player.getServer().getWorld(rk);
        if (rw == null) rw = player.getServer().getOverworld();
        player.teleport(rw, data.x+0.5, data.y+1, data.z+0.5, data.yaw, 0f);
        player.sendMessage(Text.literal("§aBack to the overworld!"), true);
    }

    private static final Map<UUID, ReturnData> RETURN_MAP = new HashMap<>();

    private static void storeReturn(ServerPlayerEntity p, ServerWorld w, BlockPos pos, float yaw) {
        RETURN_MAP.put(p.getUuid(), new ReturnData(
            w.getRegistryKey().getValue().toString(), pos.getX(), pos.getY(), pos.getZ(), yaw));
    }

    public static class ReturnData {
        public final String dim; public final int x,y,z; public final float yaw;
        ReturnData(String dim,int x,int y,int z,float yaw){this.dim=dim;this.x=x;this.y=y;this.z=z;this.yaw=yaw;}
    }
}
