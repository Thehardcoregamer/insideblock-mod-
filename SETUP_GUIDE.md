# Inside Any Block Mod — Complete Setup Guide
## For Minecraft 1.21.1 (Fabric)

---

## What You Need First

Before starting, download and install these (all free):

1. **Java 21** — https://adoptium.net (download "Temurin 21 LTS")
   - During install, check "Add to PATH" if asked
   - Verify by opening Command Prompt and typing: `java -version`

2. **IntelliJ IDEA Community** — https://www.jetbrains.com/idea/download
   - Free version is fine. This is your code editor.

3. **Python 3** — https://python.org/downloads
   - Check "Add Python to PATH" during install
   - After install, open Command Prompt and run: `pip install nbtlib`

4. **Fabric Mod Template** (you'll generate this in Step 1)

---

## STEP 1 — Create the Mod Project

1. Go to https://fabricmc.net/develop/template/
2. Fill in:
   - **Mod Name:** Inside Any Block
   - **Mod ID:** insideblock
   - **Package Name:** com.insideblock
   - **Minecraft Version:** 1.21.1
   - Check "Use Kotlin" → **NO**
   - Check "Data Generation" → **NO**
3. Click **Download** — you get a ZIP file
4. Extract the ZIP to a folder, e.g. `C:\mods\insideblock-mod`

---

## STEP 2 — Replace Files With the Ones I Made

Copy the files from this download into your extracted folder,
**replacing** any existing files:

### Files to copy into `src/main/java/com/insideblock/`:
- `InsideBlockMod.java`
- `dimension/InsideBlockDimension.java`

### Files to copy into `src/main/resources/`:
- `fabric.mod.json`

### Files to copy into `src/main/resources/data/insideblock/dimension/`:
- `inside_dimension.json`

### Files to copy into `src/main/resources/data/insideblock/dimension_type/`:
- `inside_dimension.json`

### Files to copy into the **root** of your mod folder (same level as build.gradle):
- `build.gradle` (replace existing)
- `gradle.properties` (replace existing)
- `settings.gradle` (replace existing)
- `generate_structures.py`

---

## STEP 3 — Generate the Structure Files

The mod needs `.nbt` files for each block's interior room.
The Python script creates all of them automatically.

1. Open **Command Prompt**
2. Navigate to your mod folder:
   ```
   cd C:\mods\insideblock-mod
   ```
3. Run:
   ```
   pip install nbtlib
   python generate_structures.py
   ```
4. You should see output like:
   ```
   Building stone_interior...
     Saved stone_interior.nbt
   Building grass_interior...
     Saved grass_interior.nbt
   ...
   Done! 20 structures saved.
   ```

The `.nbt` files are now in `src/main/resources/data/insideblock/structures/`

---

## STEP 4 — Create the Structures Folder (Important!)

Make sure this folder exists (create it if it doesn't):
```
src/main/resources/data/insideblock/structures/
```

The Python script should create it automatically, but double-check.

---

## STEP 5 — Open Project in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Click **Open** (or File → Open)
3. Navigate to your mod folder and click **OK**
4. IntelliJ will ask "Trust project?" → click **Trust Project**
5. Wait for it to finish indexing (progress bar at bottom, takes 1-5 minutes)

---

## STEP 6 — Build the Mod

In IntelliJ, open the **Terminal** tab at the bottom and run:

**On Windows:**
```
gradlew.bat build
```

**On Mac/Linux:**
```
./gradlew build
```

Wait for it — it downloads Minecraft and Fabric the first time (~5 minutes).
When done you'll see:
```
BUILD SUCCESSFUL
```

Your mod `.jar` file is at:
```
build/libs/insideblock-1.0.0.jar
```

---

## STEP 7 — Install and Test

1. Install **Fabric Loader for 1.21.1**:
   - Go to https://fabricmc.net/use/installer/
   - Download and run the installer
   - Select version **1.21.1**, click Install

2. Open the Minecraft Launcher, select the new **Fabric 1.21.1** profile

3. Find your `.minecraft` folder:
   - Windows: `%appdata%\.minecraft`
   - Mac: `~/Library/Application Support/minecraft`

4. Inside `.minecraft`, open the `mods` folder
   (create it if it doesn't exist)

5. Copy these two files into the `mods` folder:
   - `insideblock-1.0.0.jar` (from `build/libs/`)
   - Download **Fabric API** from https://modrinth.com/mod/fabric-api
     (get the version for 1.21.1)

6. Launch Minecraft!

---

## STEP 8 — How to Use the Mod In-Game

- **Hold Shift + Right-click** any block to enter it
- You'll be teleported into a custom room themed to that block
- **Open the chests** to find loot
- **Step on the purple/warped pressure plate** in the center to return home

### Supported blocks (currently):
Stone, Grass Block, Dirt, Oak/Birch/Spruce Log, Diamond Ore,
Deepslate Diamond Ore, Iron Ore, Gold Ore, Coal Ore, Sand, Gravel,
Obsidian, Netherrack, End Stone, Chest, Crafting Table, Bookshelf,
Water, Lava, Oak Leaves, Sponge, Ancient Debris

---

## STEP 9 — Adding More Blocks (Optional)

To add a new block interior:

1. Open `InsideBlockMod.java`
2. Find the `static { }` block near the top
3. Add a new line like:
   ```java
   BLOCK_STRUCTURES.put(Blocks.EMERALD_ORE, "emerald_interior");
   ```
4. Open `generate_structures.py`
5. Add a new entry in the `structures` dictionary:
   ```python
   "emerald_interior": {
       "floor": "minecraft:emerald_block",
       "wall":  "minecraft:deepslate_tiles",
       "ceil":  "minecraft:deepslate_tiles",
       "light": "minecraft:sea_lantern",
   },
   ```
6. Re-run `python generate_structures.py`
7. Rebuild with `gradlew.bat build`

---

## Troubleshooting

**"BUILD FAILED" — cannot find symbol**
→ Make sure Java 21 is installed. In IntelliJ go to:
  File → Project Structure → SDK → Add JDK → point to Java 21 folder

**Mod doesn't appear in Minecraft**
→ Make sure Fabric API jar is also in the mods folder

**Dimension not found error in-game**
→ Make sure both `inside_dimension.json` files are in the right folders
  (one in `dimension/`, one in `dimension_type/`)

**Structures not placing (stone brick room instead of themed room)**
→ Run `python generate_structures.py` again and check the .nbt files exist
  in `src/main/resources/data/insideblock/structures/`

---

## File Structure Overview

```
insideblock-mod/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── generate_structures.py          ← Run this with Python first!
└── src/main/
    ├── java/com/insideblock/
    │   ├── InsideBlockMod.java     ← Main mod + right-click logic
    │   └── dimension/
    │       └── InsideBlockDimension.java  ← Teleport + room placement
    └── resources/
        ├── fabric.mod.json
        └── data/insideblock/
            ├── dimension/
            │   └── inside_dimension.json
            ├── dimension_type/
            │   └── inside_dimension.json
            └── structures/
                ├── stone_interior.nbt      ← Generated by Python script
                ├── grass_interior.nbt
                ├── diamond_interior.nbt
                └── ... (20 more)
```
