"""
Pure-Python NBT structure generator — no pip installs needed.
Generates Minecraft 1.21.1 .nbt structure files (13x8x13 rooms).
"""
import struct, gzip, os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
    "src","main","resources","data","insideblock","structures")
os.makedirs(OUT, exist_ok=True)

TAG_END=0;TAG_INT=3;TAG_STRING=8;TAG_LIST=9;TAG_COMPOUND=10

def enc_str(s):
    b=s.encode("utf-8"); return struct.pack(">H",len(b))+b
def enc_tag(tid,name,payload):
    return struct.pack("B",tid)+enc_str(name)+payload
def enc_int(v): return struct.pack(">i",v)
def enc_list(tid,items):
    return struct.pack("B",tid)+struct.pack(">i",len(items))+b"".join(items)
def enc_compound(fields):
    return b"".join(fields)+struct.pack("B",TAG_END)
def named_int(name,v):   return enc_tag(TAG_INT,name,enc_int(v))
def named_str(name,s):   return enc_tag(TAG_STRING,name,enc_str(s))
def named_list(name,tid,items): return enc_tag(TAG_LIST,name,enc_list(tid,items))
def palette_entry(block_name):
    return enc_compound([named_str("Name",block_name)])
def block_entry(x,y,z,si):
    pos=enc_tag(TAG_LIST,"pos",enc_list(TAG_INT,[enc_int(x),enc_int(y),enc_int(z)]))
    return enc_compound([pos,named_int("state",si)])

def build_room(floor,wall,ceil,light,W=13,H=8,D=13):
    AIR   = "minecraft:air"
    PRESS = "minecraft:warped_pressure_plate"
    CHEST = "minecraft:chest"
    needed={AIR,floor,wall,ceil,light,PRESS,CHEST}
    pal=sorted(needed); idx={b:i for i,b in enumerate(pal)}
    # 5 chests + center pressure plate
    chests={(3,1,3),(9,1,3),(3,1,9),(9,1,9),(6,1,3)}
    lights={(2,H-1,2),(10,H-1,2),(2,H-1,10),(10,H-1,10),(6,H-1,6)}
    # Pillars at corners (x,z)
    pillars={(2,z,2) for z in range(1,4)} | {(10,z,2) for z in range(1,4)} | \
            {(2,z,10) for z in range(1,4)} | {(10,z,10) for z in range(1,4)}
    blocks=[]
    for y in range(H):
        for z in range(D):
            for x in range(W):
                pos=(x,y,z)
                border=x==0 or x==W-1 or y==0 or y==H-1 or z==0 or z==D-1
                if pos in lights:          si=idx[light]
                elif pos in chests:        si=idx[CHEST]
                elif pos==(6,1,6):         si=idx[PRESS]
                elif pos in pillars:       si=idx[wall]
                elif border:
                    if y==0:               si=idx[floor]
                    elif y==H-1:           si=idx[ceil]
                    else:                  si=idx[wall]
                else:                      si=idx[AIR]
                blocks.append(block_entry(x,y,z,si))
    pal_nbt=enc_tag(TAG_LIST,"palette",enc_list(TAG_COMPOUND,[palette_entry(b) for b in pal]))
    blk_nbt=enc_tag(TAG_LIST,"blocks", enc_list(TAG_COMPOUND,blocks))
    ent_nbt=enc_tag(TAG_LIST,"entities",enc_list(TAG_COMPOUND,[]))
    size_nbt=enc_tag(TAG_LIST,"size",enc_list(TAG_INT,[enc_int(W),enc_int(H),enc_int(D)]))
    dv_nbt=named_int("DataVersion",3953)
    root_payload=enc_compound([dv_nbt,size_nbt,pal_nbt,blk_nbt,ent_nbt])
    return enc_tag(TAG_COMPOUND,"",root_payload)

def save(name,root):
    path=os.path.join(OUT,name+".nbt")
    with gzip.open(path,"wb") as f: f.write(root)
    print(f"  saved {name}.nbt")

# (floor, wall, ceil, light)
ROOMS = {
    "default_room":      ("minecraft:stone_bricks","minecraft:stone_bricks","minecraft:stone_bricks","minecraft:glowstone"),
    "stone_room":        ("minecraft:stone","minecraft:stone_bricks","minecraft:stone_bricks","minecraft:glowstone"),
    "iron_room":         ("minecraft:iron_block","minecraft:iron_bars","minecraft:iron_block","minecraft:glowstone"),
    "gold_room":         ("minecraft:gold_block","minecraft:gold_block","minecraft:gold_block","minecraft:glowstone"),
    "diamond_room":      ("minecraft:diamond_block","minecraft:deepslate_tiles","minecraft:deepslate_tiles","minecraft:sea_lantern"),
    "emerald_room":      ("minecraft:emerald_block","minecraft:deepslate_tiles","minecraft:emerald_block","minecraft:sea_lantern"),
    "netherite_room":    ("minecraft:netherite_block","minecraft:polished_blackstone_bricks","minecraft:netherite_block","minecraft:shroomlight"),
    "debris_room":       ("minecraft:blackstone","minecraft:polished_blackstone_bricks","minecraft:polished_blackstone_bricks","minecraft:shroomlight"),
    "coal_room":         ("minecraft:coal_block","minecraft:stone","minecraft:stone","minecraft:glowstone"),
    "crafting_room":     ("minecraft:oak_planks","minecraft:oak_planks","minecraft:oak_planks","minecraft:glowstone"),
    "furnace_room":      ("minecraft:stone_bricks","minecraft:furnace","minecraft:stone_bricks","minecraft:glowstone"),
    "enchanting_room":   ("minecraft:obsidian","minecraft:bookshelf","minecraft:obsidian","minecraft:crying_obsidian"),
    "smith_room":        ("minecraft:stone_bricks","minecraft:stone_bricks","minecraft:stone_bricks","minecraft:glowstone"),
    "library_room":      ("minecraft:oak_planks","minecraft:bookshelf","minecraft:oak_planks","minecraft:glowstone"),
    "grass_room":        ("minecraft:grass_block","minecraft:dirt","minecraft:oak_leaves","minecraft:glowstone"),
    "dirt_room":         ("minecraft:dirt","minecraft:dirt","minecraft:dirt","minecraft:glowstone"),
    "wood_room":         ("minecraft:oak_planks","minecraft:oak_log","minecraft:oak_planks","minecraft:shroomlight"),
    "leaves_room":       ("minecraft:grass_block","minecraft:oak_leaves","minecraft:oak_leaves","minecraft:glowstone"),
    "sand_room":         ("minecraft:sand","minecraft:sandstone","minecraft:sandstone","minecraft:glowstone"),
    "gravel_room":       ("minecraft:gravel","minecraft:stone","minecraft:stone","minecraft:glowstone"),
    "sponge_room":       ("minecraft:sponge","minecraft:sponge","minecraft:sponge","minecraft:glowstone"),
    "nether_room":       ("minecraft:netherrack","minecraft:nether_bricks","minecraft:nether_bricks","minecraft:glowstone"),
    "magma_room":        ("minecraft:magma_block","minecraft:nether_bricks","minecraft:magma_block","minecraft:shroomlight"),
    "end_room":          ("minecraft:end_stone","minecraft:end_stone_bricks","minecraft:end_stone_bricks","minecraft:sea_lantern"),
    "obsidian_room":     ("minecraft:obsidian","minecraft:obsidian","minecraft:crying_obsidian","minecraft:glowstone"),
    "bedrock_room":      ("minecraft:bedrock","minecraft:bedrock","minecraft:bedrock","minecraft:glowstone"),
    "dragon_room":       ("minecraft:purpur_pillar","minecraft:purpur_block","minecraft:purpur_block","minecraft:end_rod"),
    "beacon_room":       ("minecraft:glass","minecraft:sea_lantern","minecraft:glass","minecraft:sea_lantern"),
    "ender_room":        ("minecraft:obsidian","minecraft:crying_obsidian","minecraft:obsidian","minecraft:end_rod"),
    "spawner_room":      ("minecraft:mossy_cobblestone","minecraft:mossy_stone_bricks","minecraft:mossy_stone_bricks","minecraft:glowstone"),
    "treasure_room":     ("minecraft:gold_block","minecraft:diamond_block","minecraft:gold_block","minecraft:sea_lantern"),
}

if __name__=="__main__":
    print(f"Generating {len(ROOMS)} rooms into:\n  {OUT}\n")
    for name,(f,w,c,l) in ROOMS.items():
        save(name,build_room(f,w,c,l))
    print(f"\nDone! {len(ROOMS)} .nbt files generated.")
