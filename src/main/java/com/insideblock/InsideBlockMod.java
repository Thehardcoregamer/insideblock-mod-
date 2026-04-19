package com.insideblock;

import com.insideblock.dimension.InsideBlockDimension;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class InsideBlockMod implements ModInitializer {

    public static final String MOD_ID = "insideblock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // The teleporter item
    public static Item BLOCK_TELEPORTER;

    // Maps blocks to their interior structure name
    public static final Map<Block, String> BLOCK_STRUCTURES = new HashMap<>();

    static {
        // Stone & ores
        BLOCK_STRUCTURES.put(Blocks.STONE,                    "stone_room");
        BLOCK_STRUCTURES.put(Blocks.COBBLESTONE,              "stone_room");
        BLOCK_STRUCTURES.put(Blocks.IRON_ORE,                 "iron_room");
        BLOCK_STRUCTURES.put(Blocks.DEEPSLATE_IRON_ORE,       "iron_room");
        BLOCK_STRUCTURES.put(Blocks.GOLD_ORE,                 "gold_room");
        BLOCK_STRUCTURES.put(Blocks.DEEPSLATE_GOLD_ORE,       "gold_room");
        BLOCK_STRUCTURES.put(Blocks.DIAMOND_ORE,              "diamond_room");
        BLOCK_STRUCTURES.put(Blocks.DEEPSLATE_DIAMOND_ORE,    "diamond_room");
        BLOCK_STRUCTURES.put(Blocks.EMERALD_ORE,              "emerald_room");
        BLOCK_STRUCTURES.put(Blocks.DEEPSLATE_EMERALD_ORE,    "emerald_room");
        BLOCK_STRUCTURES.put(Blocks.COAL_ORE,                 "coal_room");
        BLOCK_STRUCTURES.put(Blocks.DEEPSLATE_COAL_ORE,       "coal_room");
        BLOCK_STRUCTURES.put(Blocks.ANCIENT_DEBRIS,           "debris_room");
        BLOCK_STRUCTURES.put(Blocks.NETHERITE_BLOCK,          "netherite_room");
        // Smelted ore blocks
        BLOCK_STRUCTURES.put(Blocks.IRON_BLOCK,               "iron_room");
        BLOCK_STRUCTURES.put(Blocks.GOLD_BLOCK,               "gold_room");
        BLOCK_STRUCTURES.put(Blocks.DIAMOND_BLOCK,            "diamond_room");
        BLOCK_STRUCTURES.put(Blocks.EMERALD_BLOCK,            "emerald_room");
        // Crafting / utility
        BLOCK_STRUCTURES.put(Blocks.CRAFTING_TABLE,           "crafting_room");
        BLOCK_STRUCTURES.put(Blocks.FURNACE,                  "furnace_room");
        BLOCK_STRUCTURES.put(Blocks.ENCHANTING_TABLE,         "enchanting_room");
        BLOCK_STRUCTURES.put(Blocks.ANVIL,                    "smith_room");
        BLOCK_STRUCTURES.put(Blocks.SMITHING_TABLE,           "smith_room");
        BLOCK_STRUCTURES.put(Blocks.BOOKSHELF,                "library_room");
        // Nature
        BLOCK_STRUCTURES.put(Blocks.GRASS_BLOCK,              "grass_room");
        BLOCK_STRUCTURES.put(Blocks.DIRT,                     "dirt_room");
        BLOCK_STRUCTURES.put(Blocks.OAK_LOG,                  "wood_room");
        BLOCK_STRUCTURES.put(Blocks.BIRCH_LOG,                "wood_room");
        BLOCK_STRUCTURES.put(Blocks.SPRUCE_LOG,               "wood_room");
        BLOCK_STRUCTURES.put(Blocks.JUNGLE_LOG,               "wood_room");
        BLOCK_STRUCTURES.put(Blocks.DARK_OAK_LOG,             "wood_room");
        BLOCK_STRUCTURES.put(Blocks.OAK_LEAVES,               "leaves_room");
        BLOCK_STRUCTURES.put(Blocks.SAND,                     "sand_room");
        BLOCK_STRUCTURES.put(Blocks.GRAVEL,                   "gravel_room");
        BLOCK_STRUCTURES.put(Blocks.SPONGE,                   "sponge_room");
        // Nether & End
        BLOCK_STRUCTURES.put(Blocks.NETHERRACK,               "nether_room");
        BLOCK_STRUCTURES.put(Blocks.MAGMA_BLOCK,              "magma_room");
        BLOCK_STRUCTURES.put(Blocks.NETHER_BRICKS,            "nether_room");
        BLOCK_STRUCTURES.put(Blocks.END_STONE,                "end_room");
        BLOCK_STRUCTURES.put(Blocks.END_STONE_BRICKS,         "end_room");
        BLOCK_STRUCTURES.put(Blocks.OBSIDIAN,                 "obsidian_room");
        BLOCK_STRUCTURES.put(Blocks.CRYING_OBSIDIAN,          "obsidian_room");
        // Special
        BLOCK_STRUCTURES.put(Blocks.BEDROCK,                  "bedrock_room");
        BLOCK_STRUCTURES.put(Blocks.CHEST,                    "treasure_room");
        BLOCK_STRUCTURES.put(Blocks.ENDER_CHEST,              "ender_room");
        BLOCK_STRUCTURES.put(Blocks.SPAWNER,                  "spawner_room");
        BLOCK_STRUCTURES.put(Blocks.DRAGON_EGG,               "dragon_room");
        BLOCK_STRUCTURES.put(Blocks.BEACON,                   "beacon_room");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("InsideBlock mod loaded!");

        // Register the teleporter item
        BLOCK_TELEPORTER = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "block_teleporter"),
            new Item(new Item.Settings().maxCount(1)) // single use — consumed on use
        );

        // Right-click block WITH the teleporter item in hand
        UseBlockCallback.EVENT.register(this::onUseBlock);

        // Tick: check if any player in the pocket dim is standing on the return plate
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                if (!world.getRegistryKey().equals(InsideBlockDimension.WORLD_KEY)) continue;
                for (PlayerEntity player : world.getPlayers()) {
                    BlockPos below = player.getBlockPos().down();
                    if (world.getBlockState(below).isOf(Blocks.WARPED_PRESSURE_PLATE)) {
                        InsideBlockDimension.returnPlayer((ServerPlayerEntity) player);
                    }
                }
            }
        });
    }

    private ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        // Must be holding the teleporter item
        ItemStack held = player.getStackInHand(hand);
        if (!held.isOf(BLOCK_TELEPORTER)) return ActionResult.PASS;
        // Don't trigger inside the pocket dim
        if (world.getRegistryKey().equals(InsideBlockDimension.WORLD_KEY)) return ActionResult.PASS;

        Block block = world.getBlockState(hit.getBlockPos()).getBlock();

        if (!BLOCK_STRUCTURES.containsKey(block)) {
            player.sendMessage(Text.literal("§eThis block has no interior."), true);
            return ActionResult.PASS;
        }

        // Consume the teleporter (one-use)
        if (!player.isCreative()) {
            held.decrement(1);
        }

        String room = BLOCK_STRUCTURES.get(block);
        InsideBlockDimension.teleportInside((ServerPlayerEntity) player, room, (ServerWorld) world);
        player.sendMessage(Text.literal("§6Entering the block... §7(Stand on §dwarped pressure plate§7 to return)"), true);
        return ActionResult.SUCCESS;
    }
}
