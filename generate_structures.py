"""
generate_structures.py
Run this ONCE to generate all .nbt structure files for InsideBlock mod.
Place the output .nbt files into:
  src/main/resources/data/insideblock/structures/

Requirements: pip install nbtlib
"""

import nbtlib
from nbtlib import Compound, List, Int, Short, String, Byte
import os, struct, gzip

OUTPUT_DIR = os.path.join(os.path.dirname(__file__),
    "src", "main", "resources", "data", "insideblock", "structures")
os.makedirs(OUTPUT_DIR, exist_ok=True)


def block_state(name, props=None):
    entry = Compound({"Name": String(name)})
    if props:
        entry["Properties"] = Compound({k: String(v) for k, v in props.items()})
    return entry


def build_room(palette_map, floor_block, wall_block, ceil_block, light_block,
               chest_positions, extra_blocks=None, width=11, height=7, depth=11):
    """
    Builds a generic room NBT structure.
    palette_map: {block_name: index} - supply your own ordering
    Returns (size, palette, blocks) ready for NBT
    """
    AIR   = "minecraft:air"
    FLOOR = floor_block
    WALL  = wall_block
    CEIL  = ceil_block
    LIGHT = light_block
    PRESS = "minecraft:warped_pressure_plate"
    CHEST = "minecraft:chest"

    # Build a palette list in order
    all_blocks_needed = {AIR, FLOOR, WALL, CEIL, LIGHT, PRESS, CHEST}
    if extra_blocks:
        for b in extra_blocks.values():
            all_blocks_needed.add(b)
    palette_list = sorted(all_blocks_needed)
    idx = {b: i for i, b in enumerate(palette_list)}

    blocks = []
    for y in range(height):
        for z in range(depth):
            for x in range(width):
                is_floor = (y == 0)
                is_ceil  = (y == height - 1)
                is_wall  = (x == 0 or x == width-1 or z == 0 or z == depth-1)
                pos = (x, y, z)

                if is_floor:
                    state_idx = idx[FLOOR]
                elif is_ceil:
                    state_idx = idx[CEIL]
                elif is_wall:
                    state_idx = idx[WALL]
                else:
                    state_idx = idx[AIR]

                # Overrides
                if pos in chest_positions:
                    state_idx = idx[CHEST]
                if pos == (5, 1, 5):
                    state_idx = idx[PRESS]  # Return pressure plate at center
                # Lights on ceiling corners
                if pos in [(2, height-1, 2),(8, height-1, 2),(2, height-1, 8),(8, height-1, 8),(5, height-1, 5)]:
                    state_idx = idx[LIGHT]
                if extra_blocks and pos in extra_blocks:
                    state_idx = idx[extra_blocks[pos]]

                blocks.append(Compound({
                    "pos": List[Int]([Int(x), Int(y), Int(z)]),
                    "state": Int(state_idx)
                }))

    nbt_palette = List[Compound]([
        block_state(b) for b in palette_list
    ])

    return (
        List[Int]([Int(width), Int(height), Int(depth)]),
        nbt_palette,
        List[Compound](blocks)
    )


def save_structure(filename, size, palette, blocks):
    nbt_data = nbtlib.File({
        "": Compound({
            "DataVersion": Int(3953),  # 1.21.1 data version
            "size": size,
            "palette": palette,
            "blocks": blocks,
            "entities": List[Compound]([])
        })
    })
    path = os.path.join(OUTPUT_DIR, filename + ".nbt")
    nbt_data.save(path, gzipped=True)
    print(f"  Saved {filename}.nbt")


def make_structures():
    chest_std = {(2,1,2), (8,1,2), (2,1,8), (8,1,8), (5,1,8)}

    structures = {
        "default_interior": {
            "floor": "minecraft:stone_bricks",
            "wall":  "minecraft:stone_bricks",
            "ceil":  "minecraft:stone_bricks",
            "light": "minecraft:glowstone",
        },
        "stone_interior": {
            "floor": "minecraft:stone",
            "wall":  "minecraft:stone_bricks",
            "ceil":  "minecraft:stone_bricks",
            "light": "minecraft:glowstone",
        },
        "grass_interior": {
            "floor": "minecraft:grass_block",
            "wall":  "minecraft:oak_planks",
            "ceil":  "minecraft:oak_leaves",
            "light": "minecraft:glowstone",
        },
        "dirt_interior": {
            "floor": "minecraft:dirt",
            "wall":  "minecraft:dirt",
            "ceil":  "minecraft:dirt",
            "light": "minecraft:glowstone",
        },
        "log_interior": {
            "floor": "minecraft:oak_planks",
            "wall":  "minecraft:oak_log",
            "ceil":  "minecraft:oak_planks",
            "light": "minecraft:shroomlight",
        },
        "diamond_interior": {
            "floor": "minecraft:diamond_block",
            "wall":  "minecraft:deepslate_tiles",
            "ceil":  "minecraft:deepslate_tiles",
            "light": "minecraft:sea_lantern",
        },
        "ore_interior": {
            "floor": "minecraft:stone",
            "wall":  "minecraft:deepslate",
            "ceil":  "minecraft:deepslate",
            "light": "minecraft:glowstone",
        },
        "sand_interior": {
            "floor": "minecraft:sand",
            "wall":  "minecraft:sandstone",
            "ceil":  "minecraft:sandstone",
            "light": "minecraft:glowstone",
        },
        "gravel_interior": {
            "floor": "minecraft:gravel",
            "wall":  "minecraft:stone",
            "ceil":  "minecraft:stone",
            "light": "minecraft:glowstone",
        },
        "obsidian_interior": {
            "floor": "minecraft:obsidian",
            "wall":  "minecraft:obsidian",
            "ceil":  "minecraft:crying_obsidian",
            "light": "minecraft:glowstone",
        },
        "nether_interior": {
            "floor": "minecraft:netherrack",
            "wall":  "minecraft:nether_bricks",
            "ceil":  "minecraft:nether_bricks",
            "light": "minecraft:glowstone",
        },
        "end_interior": {
            "floor": "minecraft:end_stone",
            "wall":  "minecraft:end_stone_bricks",
            "ceil":  "minecraft:end_stone_bricks",
            "light": "minecraft:end_rod",
        },
        "chest_interior": {
            "floor": "minecraft:oak_planks",
            "wall":  "minecraft:oak_planks",
            "ceil":  "minecraft:oak_planks",
            "light": "minecraft:glowstone",
        },
        "crafting_interior": {
            "floor": "minecraft:oak_planks",
            "wall":  "minecraft:oak_log",
            "ceil":  "minecraft:oak_log",
            "light": "minecraft:glowstone",
        },
        "bookshelf_interior": {
            "floor": "minecraft:oak_planks",
            "wall":  "minecraft:bookshelf",
            "ceil":  "minecraft:oak_planks",
            "light": "minecraft:glowstone",
        },
        "water_interior": {
            "floor": "minecraft:prismarine",
            "wall":  "minecraft:prismarine_bricks",
            "ceil":  "minecraft:dark_prismarine",
            "light": "minecraft:sea_lantern",
        },
        "lava_interior": {
            "floor": "minecraft:magma_block",
            "wall":  "minecraft:basalt",
            "ceil":  "minecraft:basalt",
            "light": "minecraft:shroomlight",
        },
        "leaves_interior": {
            "floor": "minecraft:grass_block",
            "wall":  "minecraft:oak_leaves",
            "ceil":  "minecraft:oak_leaves",
            "light": "minecraft:glowstone",
        },
        "sponge_interior": {
            "floor": "minecraft:sponge",
            "wall":  "minecraft:sponge",
            "ceil":  "minecraft:sponge",
            "light": "minecraft:glowstone",
        },
        "debris_interior": {
            "floor": "minecraft:blackstone",
            "wall":  "minecraft:polished_blackstone_bricks",
            "ceil":  "minecraft:polished_blackstone_bricks",
            "light": "minecraft:shroomlight",
        },
    }

    for name, cfg in structures.items():
        print(f"Building {name}...")
        size, palette, blocks = build_room(
            palette_map={},
            floor_block=cfg["floor"],
            wall_block=cfg["wall"],
            ceil_block=cfg["ceil"],
            light_block=cfg["light"],
            chest_positions=chest_std,
        )
        save_structure(name, size, palette, blocks)

    print(f"\nDone! {len(structures)} structures saved to:\n  {OUTPUT_DIR}")


if __name__ == "__main__":
    make_structures()
