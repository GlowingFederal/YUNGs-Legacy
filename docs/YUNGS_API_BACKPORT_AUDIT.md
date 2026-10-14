# YUNG's API and retained reference backport audit

Date: 2026-10-13. Analysis/design only. No production ports, new runtime dependencies, reference edits, commits or publication.

## 1. Executive summary

The six selected mods import **11 distinct API classes**, plus two important resource contracts: capped single jigsaw elements for Dungeons/Fortresses and safe structure-location advancements for Strongholds/Fortresses. Config registration, biome integration, structure registration, processors and most compatibility code belong to individual mods, not the API.

Do not port the API as a product or preserve its package tree. Share an absolute-probability selector with a default outcome and narrow JSON support where worthwhile. Keep noise in Caves. Build native generation/structure integrations independently per Minecraft target. Later introduce a connector/pool assembly core for Dungeons/Fortresses and deliberately accommodate Strongholds' separate assembler and portal policy.

Publish each mod with only its needed helpers, in a **per-mod private namespace**. Repository-level source reuse must not result in duplicate shared fully qualified class names at runtime. No central API JAR or public compatibility framework is needed.

Recommended sequence: **Extras → Mineshafts → Caves → Dungeons → Fortresses → Strongholds**. Begin **Extras on Forge 1.12.2**, with a normal-well vertical slice establishing palette translation, placement, config and independent packaging. Bring that slice down to 1.6.4 before expanding abstractions; then finish Extras on all four versions. This is a hybrid strategy, not a plan to implement all mods on 1.12.2 first.

Local history explicitly changes the API LICENSE and mods.toml from GPLv3 to LGPLv3 while leaving its GPLv3 publishing label unchanged. This supports stale POM metadata, but interpretation remains a manual gate before substantial API source reuse. Dungeons/Fortresses have separate GPLv3 licenses; the other retained mods have LGPLv3 licenses. Reimplementation does not erase provenance of reused templates, data or third-party code.

## 2. Evidence, inventory and limits

All reference paths are relative to **reference/1.16.5/**. Aliases below designate retained directories:

| Alias | Directory | Declared mod version | API Gradle pin / metadata minimum | Java files | NBT templates / pool JSON / processor-list JSON |
| --- | --- | --- | --- | ---: | --- |
| API | yungs-api | 1.16.4-Forge-13 | — | 31 | 0 / 0 / 0 |
| Caves | YUNGs-Better-Caves-1.16 | 1.1.2 | 1.16.4-Forge-2 | 48 | 0 / 0 / 0 |
| Dungeons | YUNGs-Better-Dungeons-forge-1.16 | 1.2.1 | 1.16.4-Forge-12 | 52 | 144 / 29 / 7 |
| Fortresses | YUNGs-Better-Fortresses-forge-1.16 | 0.0.1 | 1.16.4-Forge-12 | 9 | 16 / 3 / 1 |
| Mineshafts | YUNGs-Better-Mineshafts-forge-1.16 | 2.0.4 | 1.16.4-Forge-3 | 27 | 0 / 0 / 0 |
| Strongholds | YUNGs-Better-Strongholds-forge-1.16 | 1.2.1 | 1.16.4-Forge-12 | 34 | 97 / 10 / 8 |
| Extras | YUNGs-Extras-forge-1.16 | 1.0 | 1.16.4-Forge-6 | 33 | 16 / 0 / 0 |

**Every retained project declares Minecraft 1.16.4** in gradle.properties, with 1.16.2/1.16.3 snapshot mappings and Forge 35.1.x. The directory is a reference-family label, not proof of synchronized 1.16.5 releases. The API snapshot is Forge-13 while clients pin 2/3/6/12. This audit maps retained source, not downloaded historical dependency binaries.

Coverage: enumerated 723 retained files and 234 Java files; scanned source, Gradle, metadata, JSON, presets/configs, mixins, access transformers and notices; traced consumers, methods/fields, bootstrap listeners, resource registration, JSON adapters and local duplicate implementations. **All 273 compressed NBT templates were decoded**, including palettes, entities, tile data and jigsaw connectors; all report DataVersion 2586. Appendices record every Java API import consumer and API-namespaced JSON dependency.

No selected mod extends an API class, implements an API interface or uses an API annotation. No wildcard/static API imports, executable fully qualified API calls outside imports, API reflection, service loading or service descriptors were found. Reflection exists in Mineshafts' chunk-generator codec lookup and Caves' NightConfig map access; neither discovers API types. No custom packet/channel/message implementation or datagen-provider implementation was found in these seven projects. A Gradle runData declaration is not implemented datagen. Ordinary Minecraft chunk/entity/inventory synchronization still matters.

API @Mod bootstrap initializes jigsaw registration, criteria and world-load settings copying for all clients, even noise-only Caves. Operational reachability does not justify carrying these systems into legacy ports.

Production source/build layout was inspected. Native boundaries were checked against locally cached Forge 9.11.1.1345 and 10.13.4.1614 source archives, 11.15.1.2318 mapped classes, and the exact 14.23.5.2859 mapped archive. Availability is not adapter correctness. No build, launch, gameplay, multiplayer, benchmark or legal determination was performed. No Internet evidence was used for licensing.

## 3. API usage matrix

Class prefixes are **com.yungnickyoung.minecraft.yungsapi.**. D = direct Java; R = resource contract; T = transitive/bootstrap; — = no feature use. Bootstrap-only rows are not counted as semantic consumers.

| Class/subsystem | Caves | Dungeons | Fortresses | Mineshafts | Strongholds | Extras | Feature consumers | Usage and decision |
| --- | --- | --- | --- | --- | --- | --- | ---: | --- |
| noise.FastNoise | D | — | — | — | — | — | 1 | Substantial sampling/config enums; adapt Caves-local; platform-neutral |
| noise.INoiseLibrary | D | — | — | — | — | — | 1 | Tiny GetNoise interface; local |
| noise.OpenSimplex2S | D | — | — | — | — | — | 1 | Alternate fractal noise; local adaptation/provenance review |
| math.Vector2f, Vector3f | T | — | — | — | — | — | 1 | FastNoise implementation support; local |
| math.ColPos (+ Mutable) | D | — | — | — | — | — | 1 | Tiny coordinates; rewrite/inline; original imports MC |
| world.BlockSetSelector | — | D | — | D | D | D | 4 | Substantial palette distributions; shared pure concept, native block values |
| world.ItemSetSelector | — | — | — | — | D | — | 1 | Same concept; Strongholds-local item binding |
| io.JSON | — | — | — | D | D | D | 3 | Small file/Gson facade; share narrow IO/schema |
| json.BlockStateAdapter, BlockSetSelectorAdapter | — | — | — | T | T | T | 3 | Native registry/state conversion; rewrite per target |
| json.ItemAdapter, ItemSetSelectorAdapter | — | — | — | — | T | — | 1 | Strongholds semantic requirement; registered globally by original JSON |
| json.BiomeDictionaryTypeAdapter | — | — | — | T | — | — | 1 | Mineshaft variant predicates/tag list; original JSON registers globally |
| world.BoundingBoxHelper | — | — | — | D | — | — | 1 | One rotated-box function; local/native |
| world.SurfaceHelper | — | — | — | D | — | — | 1 | VerticalEntrance scan; local/native |
| api.YungJigsawConfig, YungJigsawManager | — | D | D | — | — | — | 2 | Assembly contract; rewrite internal descriptors/assembler |
| world.jigsaw.JigsawManager, piece.IMaxCountJigsawPiece | — | T | T | — | — | — | 2 | Actual algorithm/quotas/geometry; conceptual shared core later |
| piece.MaxCountSingleJigsawPiece | — | R | R | — | — | — | 2 | Data element loader; rewrite into internal descriptor |
| piece.MaxCountLegacySingleJigsawPiece, MaxCountFeatureJigsawPiece, MaxCountListJigsawPiece | — | — | — | — | — | — | 0 | Registered but not used by selected pools; drop |
| criteria.SafeStructurePositionTrigger, SafeStructureLocatePredicate | — | — | R/T | — | R/T | — | 2 | Safe missing-structure behavior; native criterion on 1.12.2 |
| init.YAModJigsaw | T | T | T | T | T | T | 2 | Modern registry/codec glue; replace mod-owned loader |
| init.YAModCriteria | T | T | T | T | T | T | 2 | Modern advancement/tick glue; local/native |
| init.YAModWorldgen + mixin.ChunkGeneratorAccessor | T | T | T | T | T | T | 4 structure mods benefit conceptually | Modern settings isolation; discard implementation, preserve world ownership |
| YungsApi.MOD_ID / LOGGER / @Mod | T | T | T | T | T | T | 0 direct | Own mod logger/lifecycle; no API bootstrap |

Legacy Configuration/lifecycle/events replace ForgeConfigSpec and modern mod buses. MapGenStructure/StructureStart/StructureComponent supply persistent structure mechanisms in all four targets; they do **not** supply jigsaw/processor assembly. Only 1.12.2 has vanilla templates, loot tables and advancements. No exact native selector with this residual-default JSON schema was established; do not substitute normalized weights blindly.

## 4. Per-mod dependency details

Within each directory, **J** means src/main/java/com/yungnickyoung/minecraft/<modid>/ and **R** means src/main/resources/. Appendix A expands import paths/line numbers.

### Caves

Direct: FastNoise, INoiseLibrary, OpenSimplex2S, ColPos. J/noise/NoiseGen.java constructs both samplers and calls INoiseLibrary.GetNoise(x,y,z). FastNoise members: SetSeed, SetFractalType, SetNoiseType, SetFractalOctaves, SetFractalGain, SetFrequency. OpenSimplex2S: long-seed constructor, setGain, setOctaves, setFrequency, setLacunarity(2.0). Region controllers call GetNoise(x,z), SetSeed/SetFrequency; CaveCarverController also sets NoiseType.Cellular and CellularDistanceFunction.Natural. NoiseSettings/builders use NoiseType values/valueOf and FractalType.FBM/RigidMulti, including SimplexFractal defaults. Preserve all selectable algorithms, not one default. Vector2f/Vector3f are transitive FastNoise support.

ColPos/Mutable is used by BetterCavesUtils' biome-distance scan and LiquidRegionController's 16×16 loop: construction, setPos, getX/getZ, packing/toLong and coordinate operations around biome lookup. Preserve signed packing and global/chunk distinctions; unused MC convenience methods need not become shared math.

BetterCaves initializes BCModConfig and BCCarver. Config registration, NightConfig dimension TOML/presets, registry resolution, logger and controller caches are local. **No API config/JSON/worldgen-registration helper is called.** BCCarver retains original AIR/LIQUID biome carvers, replaces lists and cleans dimension state on unload. ChunkStatusMixin injects at ChunkStatus.doGenerationWork HEAD during CARVERS to push CarvingContext. BetterCavesCarver consumes it once in enabled dimensions but retains it while replaying ordinary carvers in excluded dimensions. Both masks matter. Post-population IWorldGenerator is not a substitute for terrain-phase carving.

Decision: Caves-local noise/column logic and native terrain access. Preserve ravines, flooded oceans, liquids, bedrock, surface protection, excluded-dimension fallback, unload/reseed and dimension settings behavior.

### Dungeons

Direct: BlockSetSelector, YungJigsawConfig, YungJigsawManager. Constructors/default states, addBlock, from and get(Random) occur in eight processors and three spider piece classes: damaged masonry/support legs, pots, stairs/cubbies, cobweb/wool spheres. SpiderDungeonNestPiece optionally adds Quark cobbedstone. These are absolute probabilities plus a residual default, not normalized weights.

SmallDungeonStructure, SkeletonDungeonStructure and ZombieDungeonStructure construct YungJigsawConfig(pool supplier, configured size) and call assembleJigsawStructure(false,false). Pools resolve through DynamicRegistries. The enclosing Structure uses NoFeatureConfig, so no direct YungJigsawConfig.CODEC call occurs. SpiderDungeonStructure has its **own procedural pieces**, not API jigsaw. Twelve pool JSON files use max_count_single_element with name/max_count/location/processors/projection and weights. Transitive requirements include assembler, count interface, capped single element registration and access to raw pools.

BetterDungeons initializes BDModConfig, BDModProcessors, BDModStructures and BDModCompat. Deferred registers, monster-room suppression, biome/dimension rules, spacing, piece registration and processor codecs are local. DungeonContextMixin initializes a local context at Template.func_237146_a_ HEAD for chest/banner limits. Rewrite this as explicit placement context, not weak Integer counters and global template interception. Quark integration is guarded by ModList.isLoaded; compileOnly Quark/AutoRegLib are not mandatory runtime dependencies.

Decision: shared selector concept and later connector core; local processors, spider geometry, loot, banners and optional Quark adapter. Preserve all four dungeon families; one small-dungeon slice is not a finished mod.

### Fortresses

Direct: YungJigsawConfig/YungJigsawManager only. BetterFortressStructure constructs battle_bridge_start with depth 40 and calls assembly(false,false). battle_bridge.json uses capped singles: piece_1 weight 5000/max_count 2, piece_2 weight 1/max_count 2. Weighted sampling must not expand lists by weight. **YungJigsawConfig.CODEC accepts 0–7 but its Java constructor accepts 40**; that codec range is not the effective in-code generation limit.

R/data/minecraft/advancements/nether/find_fortress.json uses yungsapi:structure_location. BFModProcessors/BFModStructures/BFModConfig own registration/settings/biome replacement and BridgeLegProcessor. The structure defines blaze, zombified-piglin, wither-skeleton, skeleton and magma-cube spawn entries. Legacy zombie-pigman/skeleton-subtype and Nether generator spawn lookup must be integrated; placing a building alone is incomplete.

Baseline caution: version 0.0.1 has 16 templates, three pools, hardcoded Y=60, unused minY/maxY and a YUNGs-Bridges issue link. Active start is battle_bridge_start. This suggests an experimental retained baseline, not proof of a complete released fortress overhaul. Settle intended content scope before this phase.

### Mineshafts

Direct: JSON, BlockSetSelector, BoundingBoxHelper, SurfaceHelper. ModConfig uses JSON.toJsonString for available BiomeDictionary tags and createJsonFileFromObject/loadObjectFromJsonFile for variants.json on world load. MineshaftVariants/MineshaftVariantSettings carry native states, selectors and required/blacklisted biome types. Semantic adapters: block state, block selector, biome dictionary; item adapters are only incidental universal-Gson registration.

BlockSetSelectors builds themed distributions with constructor/addBlock. Variants/settings/MineshaftPiece/BigTunnel consume selectors, get(Random) and getDefaultBlockState. Seven pieces use boxFromCoordsWithRotation(x,y,z,secondaryLen,yLen,mainLen,direction). VerticalEntrance calls getSurfaceHeight(chunk,ColumnPos). The original helper scans from 255 and compares ordinary AIR identity; native top-solid/liquid height queries are not automatically equivalent.

BetterMineshafts initializes ModStructures and ModConfig. Starts/pieces, NBT, frequency, altitude, vanilla replacement and biome variants are local procedural code. No templates/jigsaw/API assembler. Local reflection of ChunkGenerator.func_230347_a_ is a TerraForged bypass, not API discovery.

Decision: second mod proves persistent native structures; geometry/surface helpers local, selector concept shared, variants JSON native. MapGenStructureIO registration and restart-safe piece fields matter more than modern wrappers.

### Strongholds

Direct: JSON, BlockSetSelector, ItemSetSelector. BSModConfig creates/loads ores.json, rareblocks.json, armorstands.json, itemframes.json on world load. OreChances/RareBlockChances/LegProcessor use block constructor/addBlock/get; ArmorStandChances/ItemFrameChances use item constructor/addItem/get with empty defaults for equipment/display distributions. Both block and item JSON adapters are required.

R/data/minecraft/advancements/story/follow_ender_eye.json references yungsapi:structure_location twice. **No API jigsaw facade/config is used.** BetterStrongholdStructure calls J/world/jigsaw/JigsawManager.java and local JigsawConfig. This assembler implements template-keyed room quotas from BSConfig, portal-room priority after half maximum depth, weighted retry, rotation, collision, fallback and maxY=255. An API-only port misses the authoritative path.

BSModProcessors/BSModStructures/BSModConfig own template processing and placement. Mixins alter EnderEyeItem.onItemRightClick's locate result, LocateCommand.func_241053_a_, ChunkGenerator.func_235958_g_ (vanilla stronghold coordinates) and settings/codec access. Placement uses alternating radial bands, not blindly historical vanilla positions/rings. Preserve progression, nearest intended vanilla/custom structure lookup, frequency/rings and old structures through native locating hooks.

Decision: selector and later connector concepts shared; portal policy, entity dressing, locating and version content substitutions local. Do not erase the local assembler's behavior by switching to a generic API algorithm.

### Extras

Direct: JSON and BlockSetSelector only. YEModConfig creates/loads wishing_wells.json on world load. WishingWellChances uses default sandstone, gold ore 0.5, emerald ore 0.05 and gold block 0.01 via constructor/addBlock/get. Only block/state selector adapters are semantic requirements.

YungsExtras initializes YEModConfig/YEModPlacements/YEModFeatures. Sixteen configured features cover nine wells (normal/dry/wishing × three sizes), four obelisks, giant torch, ruins and chillzone. AbstractTemplateFeature loads a template, offsets by half width/depth, places using Template/PlacementSettings then processes markers. Features own substrate/surface checks and postprocessing. RngInitializerPlacement draws odd seed factors, reseeds the supplied random and passes a position onward. Preserve reproducible variation; trace random ownership before reseeding a shared Forge population stream.

The biome handler removes vanilla desert wells and adds features to desert/category or explicitly allowed biomes with blacklist filtering. Empty mixin JSON/commented setup is not active compatibility work. No jigsaw, structure-start or custom network requirement. Native decoration and modern palette translation are needed even for this small mod.

## 5. Classification of every API source class

A = actual Java/data requirement; B = transitive support; C = potentially useful but unnecessary; D = modern implementation unsuitable for direct legacy use; E = unrelated. Primary categories overlap conceptually with modernization: a B class can still need complete replacement. Paths follow API/src/main/java/com/yungnickyoung/minecraft/yungsapi/.

| Source class path | Category | Legacy decision |
| --- | --- | --- |
| noise/FastNoise.java | A | Adapt Caves-local; retain MIT notice and selectable kernels |
| noise/INoiseLibrary.java | A | Tiny local interface |
| noise/OpenSimplex2S.java | A | Adapt after provenance review; local |
| math/ColPos.java (+ Mutable) | A | Inline primitive column representation |
| math/Vector2f.java | B | Local noise support |
| math/Vector3f.java | B | Local noise support |
| world/BlockSetSelector.java | A | Shared absolute-probability concept, native values |
| world/ItemSetSelector.java | A | Same core; Strongholds item binding |
| world/BoundingBoxHelper.java | A | Local/native; compare vanilla anchoring before replacement |
| world/SurfaceHelper.java | A | Native scan with explicit height/air semantics |
| io/JSON.java | A | Minimal controlled IO; no universal global Gson |
| json/BlockStateAdapter.java | B | Native name/property or metadata resolver |
| json/BlockSetSelectorAdapter.java | B | Adapt schema/default and stable outcome order |
| json/ItemAdapter.java | B | Native ID/missing/empty outcome |
| json/ItemSetSelectorAdapter.java | B | Strongholds-only |
| json/BiomeDictionaryTypeAdapter.java | B | Mineshafts-only native predicates |
| api/YungJigsawConfig.java | A | Pool/depth descriptor; drop modern Codec/IFeatureConfig |
| api/YungJigsawManager.java | A | Replace forwarding facade with new internal assembler |
| world/jigsaw/JigsawManager.java (+ Assembler/Entry) | B | Reimplement connector graph, bounds, sampling, quotas |
| world/jigsaw/piece/IMaxCountJigsawPiece.java | B | Fields on internal descriptor replace interface |
| world/jigsaw/piece/MaxCountSingleJigsawPiece.java | A (data) | Translate used schema; no modern subclass/codec |
| world/jigsaw/piece/MaxCountLegacySingleJigsawPiece.java | C | Drop; “legacy” here is modern single-element semantics, not old-MC support |
| world/jigsaw/piece/MaxCountFeatureJigsawPiece.java | C | Drop; no selected feature-element consumer |
| world/jigsaw/piece/MaxCountListJigsawPiece.java | C | Drop; no selected list-element consumer |
| criteria/SafeStructurePositionTrigger.java (+ Instance) | A (data) | Native 1.12.2 criterion; older versions have no advancement format |
| criteria/SafeStructureLocatePredicate.java | B | Missing structure safely fails; native lookup |
| init/YAModCriteria.java | B | Native/local registration and tick integration |
| init/YAModJigsaw.java | B | Internal data loader replaces modern registry glue |
| init/YAModWorldgen.java | D | Drop modern map/settings copier; preserve world-owned state |
| mixin/ChunkGeneratorAccessor.java | D | Drop codec/settings accessors; choose native hooks |
| YungsApi.java | D | Drop API @Mod/global ID/bootstrap/logger |

No wholly unrelated E package exists in this small snapshot. Do not invent config frameworks, networking, auto-registration annotations or features from later API versions.

YAModWorldgen copies DimensionStructuresSettings on server-world load, including stronghold spread/settings ownership, and skips TerraForged. Its comment mentions another map patch, but the retained mixin JSON only declares ChunkGeneratorAccessor: no corresponding settings-patch class is present. Preserve isolation as a requirement, not the comment as proof of that patch. Criteria safely fails if a structure is absent, requires a loaded valid start and checks server players every 20 ticks.

## 6. Approximate used vs unused scope

Measured primary classification: **13 A + 12 B + 3 C + 3 D + 0 E = 31 files**. A includes 11 imported classes plus a capped element and criterion referenced by data. Reachability is not a recommendation to port 25 classes.

A coarse allocation by responsibility is **about 40% durable functionality/concepts, 50% modern implementation/scaffolding, 10% unused optional variants**. These are design estimates, not LOC or effort percentages. Much modern scaffolding sits inside A/B, not just primary-D rows.

Source including comments/blanks is roughly 4,785 lines; noise contributes 2,699 (56%). LOC therefore makes Caves math look much larger than class counts. Eleven direct classes is not one-third of behavior, and 25 reachable files does not justify wholesale copying. True sharing concentrates in **block probability selection (4 mods), JSON IO/state selection (3), and connector assembly (2 API clients plus Strongholds' local implementation)**.

## 7. Recommended internal architecture

This is a design recommendation, not source trees or interfaces created by this audit. Keep version projects as independent build islands. Only share implementations that are actually equivalent; avoid a single world interface covering all mods and all versions.

| Internal responsibility | Consumers | Platform-neutral common code? | Native work / sharing boundary |
| --- | --- | --- | --- |
| Absolute-probability selection with residual default | Dungeons, Mineshafts, Strongholds, Extras | Yes: generic outcome/chance data and Random selection, Java 7 compatible | States/items stay native; resolve configuration into native values once; duplicate tiny facade if simpler |
| Minimal JSON file IO and probability schema | Mineshafts, Strongholds, Extras | File IO and neutral strings/numbers can be shared | Gson availability/version must be checked per target; native block/item/biome adapters under each version; initially local in Extras |
| Column key and simple bounds arithmetic | Caves / Mineshafts respectively | Possible, but one semantic consumer each | Keep local until reuse is demonstrated; do not build common math just for two unrelated helpers |
| Noise kernels and interpolation | Caves only | Pure computation can live in a Caves-specific portable source set | Native biome/block/carving access remains version-local; no reason every mod should package noise |
| Palette/template descriptors and coordinate transforms | Extras; later Dungeons, Fortresses, Strongholds | Descriptor/rotation math can be neutral | Target-specific state mapping, tile/entity data and placement; start Extras-local; extract only when second consumer fits |
| Connector pools, weighted candidate selection, quotas, fallback/depth | Dungeons, Fortresses; later Strongholds | Yes for immutable descriptors, topology and policy, after behavior is mapped | Templates, starts, persistence and placing pieces depend on MC; one native implementation per target, packaged privately per mod |
| Persistent structure starts/pieces, locating and generation hooks | Mineshafts, Dungeons, Fortresses, Strongholds | No Minecraft classes in common | Native MapGenStructure/StructureStart/StructureComponent/IO per target; share only genuinely identical version-local placement mechanics |
| Configuration definitions, lifecycle and biome/dimension routing | All, with different semantics | Neutral validated settings possible; no ForgeConfigSpec/common MC types | Native config loading and lifecycle per target; each mod owns rules/listeners/cache state |
| Safe advancement criteria | Fortresses, Strongholds | Optional pure “inside structure” predicate concept | 1.12.2 native criterion; no forced emulation framework on older versions |
| Banners, entity equipment, spawner/loot processing | Dungeons/Strongholds plus structure-specific cases | Tiny descriptive data can be neutral | Native feature capabilities and NBT per target; local policies, especially unavailable pre-1.8 entities/banners |

Suggested organization within existing roots, only as implementation begins:

~~~text
common/src/main/java/.../<mod>/...           genuinely portable mod logic
common/src/main/java/.../internal/selection/ small reusable neutral source, if extracted
versions/<mc>/src/main/java/.../<mod>/...    native lifecycle/config/worldgen/persistence
versions/<mc>/src/main/java/.../<mod>/internal/... private helpers after packaging
~~~

Per-mod build outputs will need selective source/resource inputs: the existing neutral bootstrap and its all-common inclusion are not yet six independently installable products. Do not interpret the current root common JAR as a required runtime library or blindly copy all of it into all products. Keep target-specific compilation/remapping/resource processing unchanged except necessary future product packaging.

Behavioral contracts to settle before extraction:

- Selectors use absolute fractions, reject duplicates/total above one and fall through to a default; block default is CAVE_AIR, item default AIR. Generic normalized weighted sampling would change probabilities. Original HashMap iteration is not a robust cross-version seed-order contract; choose explicit stable iteration, document any layout difference, and preserve distributions. Do not inherit mutable exposed maps, missing-item nulls, unknown-property logging or parser failure behavior without review.
- API JSON registers all adapters in one static Gson even when unused. Prefer only required adapters, explicit charset/resource closing and validated configs. State names such as block[property=value] cannot be universally forwarded to old registries. Preserve useful schema behavior with explicit target mapping; do not silently turn unknown saved data into air.
- Assembly sampling retains weighted candidates without expanding weights, removes rejected candidates and retries, tracks quotas per structure, tries rotations/connectors, uses fallbacks at depth boundaries and clips within available volume. Modern voxel-shape subtraction permits nested/internal connections; a blanket “AABBs never overlap” rewrite is not equivalent. Start radius ±80 and vertical max 255 are observable limits to assess.
- All selected API assembler calls disable boundary-adjustment and heightmap flags. Do not implement unused terrain-matching/village projection modes simply because the API supports them. Still inspect actual connector joint/final_state/projection data before reducing descriptor fields.
- Structure planning and placing must be separated by native chunk lifecycle. Persist enough piece/template identity, position, rotation, variant, random/loot seed and processed state to resume generation after reload. Do not rely on regenerated layouts matching a mutable config. No shared static world references or unsynchronized per-dimension maps across saves.

## 8. Standalone distribution and collision strategy

**Recommendation: selective private inclusion with per-mod relocation**, performed as part of each product's native build. Example destination namespaces: com.glowingfederal.yungslegacy.yungsextras.internal and com.glowingfederal.yungslegacy.bettermineshafts.internal. Final names can follow established project conventions; the invariant is uniqueness per independently installed mod.

| Option | Assessment |
| --- | --- |
| Copy/include same common package into every JAR | Reject: LaunchWrapper generally sees one class identity per fully qualified name; first resolved copy wins, exposing version/ABI/static-state conflicts |
| Compile only needed portable/shared sources, relocate private helper package per product | Preferred for worthwhile shared implementations; preserve source attribution and validate bytecode/resources after native remap/reobf |
| Generate namespace-substituted Java source per product | Possible if relocation tooling cannot safely handle legacy output, but not preferred: generated source merging is unnecessary for these independent targets |
| Private source duplication of tiny selector/IO/box functions | Good initial option; simpler than building relocation machinery for a few small classes; same behavior/provenance still required |
| Central runtime selected/bundled once | Reject for now: introduces discovery, load-order/version negotiation and hidden dependency ownership; no selected behavior requires cross-mod state |
| Required YungsApi-Legacy.jar | Reject: violates standalone distribution |

Identical duplicate bytecode is still fragile: later independent mod updates can change class fields/methods or static registration. Package-private modifiers do not create separate classloader identities. Isolation must also cover nested classes, service descriptors if later introduced, reflective strings, mixin/refmap names, registry IDs, config paths and resource names. Existing API MOD_ID=yungsapi and its @Mod bootstrap must never be bundled as another mod entry in several products.

Keep resources in their owning mod namespace. Translate API max_count schema to mod-owned internal descriptors instead of globally registering yungsapi pool types from each JAR. Rewrite affected advancement criteria IDs to mod-owned ones on 1.12.2; avoid two mods registering the same global yungsapi:structure_location. Vanilla advancement override paths (find_fortress/follow_ender_eye) are distinct, but can conflict with unrelated packs; explicitly assess those overrides.

Sharing source is different from shipping one runtime copy. Safely standalone large assembly code will be duplicated in isolated packages in relevant JARs. Reduce this by shipping only used capabilities and avoiding a wholesale API/noise bundle, not by inventing a once-only bootstrap. Build-time deduplication is useful; cross-mod runtime deduplication is unnecessary here.

Future acceptance: each final JAR alone and all selected JARs together; unique mod IDs/private FQCNs/mixin configs, no API mod dependency or upstream API package, required assets/licenses only, correct native remap and Java major 51 for 1.6.4. Update existing archive verification for real mod classes deliberately when ports are implemented; it currently protects the bootstrap from upstream class inclusion.

## 9. Cross-mod and copied-code findings

No selected-to-selected hard dependency, optional detection, foreign Java import, config path or resource namespace reference was found in scanned text or decoded templates. All six declare a **mandatory external yungsapi** dependency in their reference mods.toml; the future architecture removes that requirement. Shared runtime registry/worldgen systems are vanilla/Forge mechanisms, not calls to another selected mod.

Other dependencies:

- Dungeons optionally uses Quark's ModuleStateChangedEvent/ModuleCategory/SpiderNestUndergroundBiomeModule to add cobbedstone only when enabled/configured. AutoRegLib is compileOnly supporting Quark; no direct AutoRegLib call was found. Re-evaluate against the actual legacy Quark version where available; do not assume the 1.16 classes/module exist. Optional absence must remain safe.
- API plus four structure mods bypass certain settings edits for chunk-generator codec namespace terraforged. This is defensive optional detection, not a runtime dependency. Legacy generation integrations need their own custom-provider compatibility checks; transplanting the codec test is meaningless.
- Strongholds' eye mixin explicitly credits TelepathicGrunt/RepurposedStructures. This is copied-code provenance, **not** a runtime dependency on RepurposedStructures.
- API build explicitly depends on Gson 2.8.6 and Mixin 0.8.2 annotation processing; Guava, Mojang codecs/datafixer pairs, Apache Commons mutable values and Minecraft/Forge classes are used by source. They are not all new libraries to bundle in legacy outputs. Actual target runtime availability must govern implementation.

Useful non-API commonality:

1. Strongholds' local JigsawConfig and JigsawManager mirror API assembly concepts but add room quotas/portal priority; they are not interchangeable copies. This is a stronger future sharing candidate than importing the API facade wholesale.
2. Dungeons/Fortresses/Strongholds ChunkGeneratorAccessor files match after namespace/prefix normalization. API's variant additionally has @Mutable for the settings setter; this matters to its recorded Java 9+ fix. All are modern scaffolding to replace, not share into legacy.
3. Dungeons and Strongholds BannerPattern.java are identical after package normalization. Banner builders are related but Dungeons adds naming/color behavior. Duplicate tiny pattern data or later share a neutral description; native banners do not exist on 1.6.4/1.7.10.
4. Four structure registration classes repeat spacing-map, flat-world, world-load and TerraForged defensive patterns. These reflect modern registry semantics; use native per-world structure routing instead of porting a universal settings-map framework.
5. Three mods repeat create-default/read-JSON-on-world-load patterns. A small IO helper is sensible only after keeping their different reload/error rules and singleton ownership explicit.

No common external YUNG's tags or selected-mod resource packs were discovered. Native vanilla tags are used (for example BlockTags.SAND in Extras substrate checks); translate their predicates explicitly on older targets. Mineshaft “biome tags” are Forge BiomeDictionary types, not modern datapack tags. Dungeons/Strongholds/Fortresses pools/processors and each mod's loot/templates belong to their own namespaces.

## 10. Modernization boundaries by system

Difficulty labels: S = straightforward adaptation; M = moderate rewrite; J = major rewrite; R = architecture redesign. Estimates concern **translation**, not proven runtime compatibility. Individual targets need native method/descriptor tracing during implementation.

| System / 1.16 boundary | 1.12.2 | 1.8.9 | 1.7.10 | 1.6.4 | Required translation |
| --- | --- | --- | --- | --- | --- |
| ForgeConfigSpec, NightConfig, ModLoadingContext | M | M | M | M | Native Configuration/lifecycle; preserve defaults/ranges and separate JSON; Caves dimension IDs/TOML model local |
| Mod event bus, FMLCommonSetupEvent.enqueueWork | S/M | S/M | M | M | Native preInit/init/events; FML package and bus/tick conventions differ; no modern enqueue copy |
| DeferredRegister/RegistryObject, WorldGenRegistries | M | M | M | J | Explicit native registration/MapGenStructureIO; numeric ID era on 1.6.4; no deferred registry framework |
| BiomeLoadingEvent/category generation lists | J | J | J | J | TerrainGen mapgen/populate/decorate hooks and native biome predicates; suppress vanilla feature only on intended paths |
| ConfiguredFeature/Placement/codec chains | M/J | J | J | J | Native population/decoration and independently seeded chance checks; no generic codec compatibility layer |
| AIR/LIQUID carvers, ChunkStatus, IChunk masks/context | J | J | J | R | Terrain-stage mapgen replacement; 1.12/1.8 primer states vs older block buffers/metadata; preserve fallback and order |
| Structure<Config>/StructureStart factories | J | J | J | J | MapGenStructure and native starts/pieces, deterministic candidate selection and per-chunk placement |
| Structure pools/jigsaw/AbstractVillagePiece | R | R | R | R | Native connector assembler absent on every target; explicit pools, connectors, quotas and piece persistence |
| Template/TemplateManager and StructureProcessor codecs | J | R | R | R | 1.12 templates usable after conversion; older native template representation/placement needed; processor behavior explicit |
| Modern state palette/property names / flattened IDs | J | J | J | J | Curated mappings, rotations, waterlogging, renamed blocks; pre-1.8 block+metadata; avoid guessed numeric IDs |
| DynamicRegistries/resource/data reload | J | J | J | J | Mod-owned resource loading/validation; no automatic modern datapack lifecycle; reset caches on relevant lifecycle |
| Structure spacing map + dimension-key routing | J | J | J | J | Native generator ownership and dimension/provider IDs; no shared modern DimensionStructuresSettings |
| VoxelShapes collision/available-volume subtraction | J | J | J | J | Bounds/occupancy with intentional nesting; no blanket disjoint-box approximation |
| Piece/start NBT and regeneration after reload | M/J | J | J | J | Stable native serialization IDs, native NBT and placement state; no codec-as-save assumption |
| Loot tables/functions/chest and minecart seeds | M/J | J | J | J | 1.12 native loot tables after schema/item translation; older weighted inventory filling/ChestGenHooks where suitable |
| Advancement predicates/structure-location trigger | M | No native format | No native format | No native format | Keep safe criterion in 1.12; do not invent advancements mod; existing achievements only if intentional |
| Eye-of-Ender, locate command, vanilla stronghold suppression | J | J | J | J/R | Native provider/generator/item paths; old versions lack modern locate command; preserve progression, avoid fallback loss |
| Fortress mob-spawn region/list behavior | J | J | J | J | Native Nether generator and structure containment, skeleton subtype/zombie-pigman mapping |
| Banners/armor stands and modern decorative entities | M | M/J | Native content absent | Native content absent | Explicit substitutions; item frames available but item contents/NBT differ; no unsolicited custom entity/block pack |
| Custom networking / client rendering | None found | None found | None found | None found | No API network/render layer to port; keep server authority and native vanilla sync |
| Datagen / optional-mod bootstrap / mixin coremod APIs | M where needed | M/J | J | J | No actual data providers; rewrite necessary hooks only; stock ASM 4.1/Java 7 constraint on 1.6.4 |

Cached source/class checks establish that all four targets contain MapGenStructure, MapGenStructureIO, StructureStart, StructureComponent and terrain generation events. 1.8.9/1.12.2 have IBlockState; 1.6.4/1.7.10 do not. Template/TemplateManager, LootTableList and CriteriaTriggers were found on 1.12.2 and absent from the earlier target archives. BiomeLoadingEvent is absent in all four. ChestGenHooks exists in the three older targets; 1.12 uses loot tables. Configuration moves from net.minecraftforge.common.Configuration (1.6.4) to net.minecraftforge.common.config.Configuration later.

Keep world mutation on the owning generation/server thread. Nothing in this audit justifies async world writes. Pure noise/layout computations can be separated later, but lifecycle/cancellation/world unload and safe handoff must be designed before claiming concurrency. Dedicated server class loading must remain free of client render references.

## 11. Per-version feasibility and player-visible priorities

| Mod | 1.12.2 | 1.8.9 | 1.7.10 | 1.6.4 |
| --- | --- | --- | --- | --- |
| Extras | M: decoration + converted native templates | J: custom template placement | J: metadata representation and template placement | J: same plus old registry/feature content gaps |
| Mineshafts | M/J: procedural pieces map naturally; modern integration replaced | J: earlier states/entities/loot | J: block metadata + native biome variants | J: numeric registry/biome roster differences, NBT details |
| Caves | J: carving phase/primer integration | J: primer/mapgen and biome semantics | J/R: older block buffers and metadata | R: terrain ownership/stock launch restrictions; not post-gen carving |
| Dungeons | J/R: custom pools, processors, piece persistence | R: template engine plus pools | R: templates/pools, missing decoration entities | R: full placement/persistence/content boundary |
| Fortresses | J/R: assembler + Nether spawn integration | R | R | R; settle experimental content baseline first |
| Strongholds | R: local assembly and progression hooks | R | R plus banner/armor-stand substitutions | R plus old provider/eye routing; largest progression risk |

No target is declared impossible. Complexity follows actual missing mechanisms, not Java age. Player-visible parity is coherent geometry, encounter/loot balance, configurable rates and biome/dimension scope, progression, stable saves and coexistence with other worldgen mods. Exact 1.16 block-state/NBT representation and bit-identical seed layouts across versions are separate, generally unsuitable acceptance requirements unless explicitly chosen.

Concrete content decisions are unavoidable: Extras templates include campfire, sandstone wall, wood-specific trapdoors/fences, smooth sandstone stairs and blackstone/gilded blackstone. Earlier versions need explicit palette substitutions. Mineshaft variants include mesa/savanna/dark oak/acacia/packed/blue ice and modern workstations; 1.6.4 has a different biome/content roster. Strongholds/Dungeons use banners, armor stands, modern slabs/stairs and flower-pot states. Wither skeletons predate the new EntityType representation but need subtype data. Missing decorative blocks should preserve silhouettes/material themes, while loot/progression substitutions must preserve intended value rather than silently disappear.

Do not add new custom blocks/entities solely to reproduce post-1.13 decoration unless authorized. Where cosmetic exactness is disproportionately costly, use target-native substitutes and document them in the real mod's support information. Conversely, portal-room availability, fortress spawning, persistent starts and vanilla suppression/fallback are core behavior, not dispensable cosmetics.

Retained-data cautions from reference-closure checks:

- Dungeons: worldgen/template_pool/zombie_dungeon/terminators/big_stairs_terminators.json points to betterdungeons:zombie_dungeon/big_stairs_crumbled_0. The retained NBT is under zombie_dungeon/big_stairs/big_stairs_crumbled_0.nbt. This is a confirmed path mismatch, not a reason to preserve a broken lookup.
- Strongholds: structures/terminators/spiral_stairs_terminator.nbt contains pool betterstrongholds:spiral_stairs, but there is no retained worldgen/template_pool/spiral_stairs.json. Determine whether that connector is intentionally never traversed or stale before converting; static absence does not prove a runtime failure.
- Fortresses: experimental 0.0.1 active battle bridge, depth 40 and start Y=60 require a scope/baseline decision. Do not infer additional missing fortress content or promise release-level parity from its project title.
- Modern NBT DataVersion 2586 is not a downward migration facility. Older DataFixers cannot reconstruct unsupported modern content. Convert explicitly; keep retained originals untouched.

## 12. Port order and prerequisites

| Order | Mod / estimated complexity | Prerequisites | Why here / major hazards |
| ---: | --- | --- | --- |
| 1 | Extras / medium on 1.12, high on earliest targets | Private packaging, config/selector, curated palettes, native placement | Exercises reusable data/state/placement without solving persistent large structures or caves; visible surface results are easy to inspect. Must validate older template boundary early |
| 2 | Mineshafts / medium-high | Selector/variant JSON, per-target biome resolution, persistent MapGenStructure pieces | Proves starts/NBT/chunk continuation and vanilla replacement using procedural code, avoiding premature jigsaw framework; surface entrances/entities/loot are hazards |
| 3 | Caves / high | Native terrain hook, config/dimension lifecycle, Caves-local noise | Proves carving/coexistence needed for underground structures; useful early but inappropriate as first shared-API prototype; double carving/liquid/surface order are risks |
| 4 | Dungeons / high-very high | Templates/state conversion, persistent starts/pieces, capped connector pools, processors/loot | First actual API assembler client; small dungeon first, then skeleton/zombie and procedural spider. Distinguish processor/context behavior from shared graph logic |
| 5 | Fortresses / high, baseline-dependent | Capped assembler, native Nether generation/spawn containment | Reuses Dungeons' exercised assembly; validates weight 5000 without list expansion. Experimental content and fortress mob/progression rules prevent using tiny Java size as simplicity proxy |
| 6 | Strongholds / very high | Proven connector engine/serialization, native locate/eye integration, item palettes | Local quotas/portal prioritization plus progression/vanilla compatibility are highest risk; adapt a proven engine with explicit policy rather than shaping all initial shared code around Strongholds |

Within later mods, begin on 1.12.2 for quicker native feedback, immediately bring a representative hardest-boundary slice to 1.6.4, then complete all four targets before marking that mod ported. Do not leave arbitrary feature families absent from older versions. Complexity estimates are qualitative source-derived judgments, not schedules or measured port durations.

## 13. Development strategy

Choose **hybrid**. Establish Extras/1.12.2's independent product and a normal well, prove the same descriptor/placement contract on 1.6.4, then complete Extras across all targets. Mineshafts repeats this with persistent starts/pieces; Dungeons later repeats it with connector/pool assembly. 1.8.9 and 1.7.10 receive native implementations at each feature stage, not a mechanical copy of either extreme.

Pure mod-first without a downward slice risks inventing shared contracts that only fit 1.12 templates. Pure version-first across several mods postpones every old-world boundary until expensive modern-style architecture has spread. Early extreme-target verification exposes representation constraints before abstraction becomes permanent, while completion by mod keeps deliverables usable.

Keep static/native build and final-remapped-JAR checks separate for each version. Runtime matrices need single-mod and combined installs, dedicated and integrated servers, reload/restart, custom dimensions/providers, chunk borders and existing/new chunks. No current analysis establishes any of those runtime results.

## 14. Licensing and provenance: repository-local findings

| Project/file evidence | Included license | mods.toml label | Other metadata / source notices |
| --- | --- | --- | --- |
| Root LICENSE and API/LICENSE | GNU Lesser GPL version 3 | API LGPLv3 | API/build.gradle POM says GNU GPLv3 and links forge-1.16.3/LICENSE |
| Caves/LICENSE.md | LGPLv3 | LGPLv3 | Project authors/credits retained in properties/resources |
| Dungeons/LICENSE | GPLv3 | GPLv3 | Authors YUNGNICKYOUNG, Acarii; no separate GPL header in Java files found |
| Fortresses/LICENSE | GPLv3 | GPLv3 | Authors YUNGNICKYOUNG, Acarii; prototype metadata includes Bridges issue link |
| Mineshafts/LICENSE | LGPLv3 | LGPLv3 | Project attribution plus TelepathicGrunt integration credits |
| Strongholds/LICENSE | LGPLv3 | LGPLv3 | Authors YUNGNICKYOUNG, Acarii; explicitly credited copied eye mixin |
| Extras/LICENSE | LGPLv3 | LGPLv3 | Authors YUNGNICKYOUNG, Acarii |

Exact API evidence: build.gradle lines 169–175 contains the POM license stanza; LICENSE identifies LGPLv3; src/main/resources/META-INF/mods.toml line 3 says LGPLv3; gradle.properties says modMinecraftVersion=1.16.4/modBuildNum=Forge-13. The other projects also declare 1.16.4, so this is not an API-only version-label inconsistency.

Local commit **9aba31a23086cf12291c2e458453ffdbeac11961**, authored by Nick Young on 2021-12-23, is titled “Update license to LGPLv3”. It changes precisely LICENSE and src/main/resources/META-INF/mods.toml from GPL to LGPL; build.gradle is untouched. The retained API files agree with the resulting LGPL text/label; HEAD's original API publishing file still has the GPL POM stanza. This is strong evidence for stale publishing metadata, but this audit does not assert legal precedence, retroactive coverage of all older pinned releases, or authorization from every contributor. Embedded mod snapshots were added through local Sources history; no separate retained per-project upstream Git histories were found. The API's license-changing commit does not relicense the other projects.

Source-specific notices across the retained Java inventory:

- API/noise/FastNoise.java lines 1–27 includes an explicit **MIT license and Copyright(c) 2017 Jordan Peck**, requiring inclusion of that copyright/permission notice in copies/substantial portions. Preserve this verbatim if reused. The packaged adaptation may include API-specific changes; its file header alone does not establish provenance of every addition.
- API/noise/OpenSimplex2S.java credits **K.jpg** and describes the smooth SuperSimplex variant, but contains no explicit license grant in the retained file. Do not import an assumed public-domain/other license from knowledge of some upstream version; provenance of this particular adaptation still needs confirmation.
- API criteria/SafeStructureLocatePredicate.java and SafeStructurePositionTrigger.java contain @author TelepathicGrunt; init/YAModWorldgen.java credits TelepathicGrunt. Preserve these attributions in adapted source.
- Strongholds/mixin/EnderEyeBetterStrongholdLocatingMixin.java begins “100% credit to TelepathicGrunt” and links the RepurposedStructures 1.16 source. The linked contents/license are not retained here; no conclusion about that external source's license was made.
- Structure registration/spacing and Strongholds ring placement also credit TelepathicGrunt. These are provenance notes, not independent license grants.
- No other explicit source-level license/copyright grant was found in the 234 Java files. Project-level licenses/author metadata still matter; lack of a Java header is not absence of licensing.

Practical decision: clean native reimplementation of small selector/IO/lifecycle mechanics avoids copying a broad API and simplifies dependency/provenance accounting. It is not a clean-room legal claim: the author has inspected the references, and reused assets, template geometry, data, substantive algorithms and copied/adapted source retain their provenance. Relocation/shading does not change license obligations. Keep origin notices and corresponding project license context with each real product; do not relabel GPL Dungeons/Fortresses or fold their copied implementations into an LGPL shared source indiscriminately. Prefer neutral independently written helpers rather than extracting GPL project code into all products without review.

**Resolution status:** historical cause is substantially explained; the contradictory published label remains in retained evidence. Manual confirmation of source-reuse/distribution treatment is required before substantial API reuse, especially older pins and third-party adaptations. This need not block independently written native Extras helpers and work under that project's clearly retained license context once the next task authorizes implementation. Substantial Caves noise reuse and copied eye-mixin reuse have additional provenance gates. No license files or notices were changed.

## 15. Open questions and blockers

1. Confirm accepted API license/provenance interpretation before substantive reuse; separately verify OpenSimplex adaptation and credited external eye-mixin provenance. These are source-reuse gates, not excuses to ship a required API JAR.
2. Define whether Fortresses is limited to the retained battle-bridge prototype or expects another canonical released snapshot. Additional content cannot be invented from this audit.
3. Decide explicit per-target palettes/entity/item substitutions. Core gameplay parity can be preserved without shipping post-1.13 decorative content, but choices should be visible and intentional.
4. Resolve Dungeons' confirmed template path mismatch when porting that data; trace Strongholds' missing connector-pool reference before deciding correction/removal. Preserve original reference files.
5. Confirm desired legacy locate command exposure: older native locating/eye behavior should work; adding modern command syntax where none exists is a separate public behavior decision.
6. Trace actual custom chunk-provider overrides during each port. Forge terrain events provide candidate integrations, not guaranteed coverage of every third-party generator.
7. Choose private helper duplication initially versus build-time relocation when shared code becomes substantial. The collision invariant is settled; no runtime singleton selection scheme is required.

## 16. Exact recommended next implementation phase

Next task: **implement the first real Extras port, beginning on Forge 1.12.2**, with native independently installable outputs. Keep reference code immutable. Do not first copy YUNG's API, add speculative common modules or migrate other mods.

Phase sequence and completion gates:

1. Inventory Extras' actual placement/config behavior and all 16 palettes into explicit target substitutions. Preserve feature rates, substrate checks, centering, blacklists/allowlists, wishing-well chances and configuration reload behavior. Record durable supported differences in appropriate existing user documentation as they become implemented.
2. Create Extras-specific product metadata/selective source/resources using existing target wrappers/remap/version allocation. No dependency on the neutral bootstrap/API; only own resources/notices. Initially use small privately namespaced helper implementations rather than a universal relocation framework.
3. Implement native 1.12.2 normal-small-well generation with translated Template data and native decoration/vanilla-well suppression. Write the real selector/JSON support only as its actual consumers require it. Validate surface/substrate and chunk-boundary behavior before generalizing template placement.
4. Implement the same representative slice natively on 1.6.4 using block+metadata placement and explicit NBT translation. This proves the hardest representation boundary. Implement corresponding native 1.7.10/1.8.9 slices; use no source-merging/preprocessing framework.
5. Complete all **16 feature variants** and config-driven biome/feature rules on all four versions. Treat cosmetic substitutions explicitly; preserve wishing-well loot and ordinary chest behavior. Extract a small reusable neutral selector only once its semantics and private packaging are concrete.
6. Validate each target compile/remap/reobf/package and final JAR contents; check 1.6.4 stock ASM/bytecode and absence of upstream/API/other-mod classes. Inspect and run real dedicated/integrated worlds when allowed: single-mod install, same-seed repeatability, multiple worlds/dimensions, config load/reload, disabled features, vanilla suppression, chunk borders, loot and combined installs as products become available.

A four-version well slice is an architecture milestone, not full Extras completion. The proposed next phase finishes Extras across all four targets before moving to Mineshafts. If runtime testing is prohibited or unavailable, report those named checks as pending and do not claim in-game parity.

## 17. Audit validation and change scope

This document is the only intended new repository file. Analysis used in-memory scripts, source inspection, resource decoding, normalized duplicate comparison and local Git/source/archive evidence. No test harnesses, generated source, benchmark projects or production changes were added. Final SHA-256 comparison confirmed all 723 reference files unchanged, with no additions or deletions; all 31 API source files are classified and every source link resolves. A changelog entry is unnecessary for investigation-only work with no implementation change; README remains player/build focused.

## Appendix A. Complete direct Java API import inventory

These are all import sites discovered across the 203 selected-mod Java files. Each heading uses the API fully qualified class name; paths identify native consumers, not production inputs. Instance use can propagate through mod-local wrappers described in section 4.

### com.yungnickyoung.minecraft.yungsapi.noise.FastNoise

- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseGen.java:4](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseGen.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseSettings.java:3](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseSettings.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/cave/CaveCarverBuilder.java:6](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/cave/CaveCarverBuilder.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/cavern/CavernCarverBuilder.java:6](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/cavern/CavernCarverBuilder.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/controller/CaveCarverController.java:13](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/controller/CaveCarverController.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/controller/CavernCarverController.java:12](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/controller/CavernCarverController.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/controller/LiquidRegionController.java:7](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/controller/LiquidRegionController.java)

### com.yungnickyoung.minecraft.yungsapi.noise.INoiseLibrary

- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseGen.java:5](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseGen.java)

### com.yungnickyoung.minecraft.yungsapi.noise.OpenSimplex2S

- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseGen.java:6](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseGen.java)

### com.yungnickyoung.minecraft.yungsapi.math.ColPos

- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/util/BetterCavesUtils.java:3](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/util/BetterCavesUtils.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/controller/LiquidRegionController.java:6](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/controller/LiquidRegionController.java)

### com.yungnickyoung.minecraft.yungsapi.world.BlockSetSelector

- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/skeleton_dungeon/RuinedStoneBrickProcessor.java:5](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/skeleton_dungeon/RuinedStoneBrickProcessor.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/skeleton_dungeon/SkeletonDungeonLegProcessor.java:5](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/skeleton_dungeon/SkeletonDungeonLegProcessor.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/small_dungeon/SmallDungeonLegProcessor.java:5](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/small_dungeon/SmallDungeonLegProcessor.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieDungeonCubbyProcessor.java:5](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieDungeonCubbyProcessor.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieDungeonFlowerPotProcessor.java:5](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieDungeonFlowerPotProcessor.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieDungeonLegProcessor.java:5](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieDungeonLegProcessor.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieDungeonStairProcessor.java:5](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieDungeonStairProcessor.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieMainStairsProcessor.java:7](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/processor/zombie_dungeon/ZombieMainStairsProcessor.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/spider_dungeon/piece/SpiderDungeonEggRoomPiece.java:5](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/spider_dungeon/piece/SpiderDungeonEggRoomPiece.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/spider_dungeon/piece/SpiderDungeonNestPiece.java:6](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/spider_dungeon/piece/SpiderDungeonNestPiece.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/spider_dungeon/piece/SpiderDungeonPiece.java:4](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/spider_dungeon/piece/SpiderDungeonPiece.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/BlockSetSelectors.java:3](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/BlockSetSelectors.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/MineshaftVariants.java:4](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/MineshaftVariants.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/MineshaftVariantSettings.java:3](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/MineshaftVariantSettings.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/BigTunnel.java:9](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/BigTunnel.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/MineshaftPiece.java:7](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/MineshaftPiece.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/OreChances.java:3](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/OreChances.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/processor/LegProcessor.java:5](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/processor/LegProcessor.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/RareBlockChances.java:4](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/RareBlockChances.java)
- [YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/world/WishingWellChances.java:3](../reference/1.16.5/YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/world/WishingWellChances.java)

### com.yungnickyoung.minecraft.yungsapi.api.YungJigsawConfig

- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/skeleton_dungeon/SkeletonDungeonStructure.java:7](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/skeleton_dungeon/SkeletonDungeonStructure.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/small_dungeon/SmallDungeonStructure.java:6](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/small_dungeon/SmallDungeonStructure.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/zombie_dungeon/ZombieDungeonStructure.java:7](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/zombie_dungeon/ZombieDungeonStructure.java)
- [YUNGs-Better-Fortresses-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterfortresses/world/BetterFortressStructure.java:5](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterfortresses/world/BetterFortressStructure.java)

### com.yungnickyoung.minecraft.yungsapi.api.YungJigsawManager

- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/skeleton_dungeon/SkeletonDungeonStructure.java:8](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/skeleton_dungeon/SkeletonDungeonStructure.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/small_dungeon/SmallDungeonStructure.java:7](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/small_dungeon/SmallDungeonStructure.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/zombie_dungeon/ZombieDungeonStructure.java:8](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/structure/zombie_dungeon/ZombieDungeonStructure.java)
- [YUNGs-Better-Fortresses-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterfortresses/world/BetterFortressStructure.java:6](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterfortresses/world/BetterFortressStructure.java)

### com.yungnickyoung.minecraft.yungsapi.io.JSON

- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/init/ModConfig.java:7](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/init/ModConfig.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/init/BSModConfig.java:11](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/init/BSModConfig.java)
- [YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/init/YEModConfig.java:4](../reference/1.16.5/YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/init/YEModConfig.java)

### com.yungnickyoung.minecraft.yungsapi.world.BoundingBoxHelper

- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/BigTunnel.java:10](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/BigTunnel.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/OreDeposit.java:7](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/OreDeposit.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/SideRoom.java:8](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/SideRoom.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/SmallTunnel.java:8](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/SmallTunnel.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/SmallTunnelStairs.java:6](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/SmallTunnelStairs.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/SmallTunnelTurn.java:6](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/SmallTunnelTurn.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/ZombieVillagerRoom.java:5](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/ZombieVillagerRoom.java)

### com.yungnickyoung.minecraft.yungsapi.world.SurfaceHelper

- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/VerticalEntrance.java:8](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/pieces/VerticalEntrance.java)

### com.yungnickyoung.minecraft.yungsapi.world.ItemSetSelector

- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/ArmorStandChances.java:3](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/ArmorStandChances.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/ItemFrameChances.java:3](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/ItemFrameChances.java)

## Appendix B. Complete API-namespaced resource inventory

The following JSON files contain API IDs. No selected NBT template contains a yungsapi namespace. Only max_count_single_element is used; registered legacy-single/feature/list variants are not required.

- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/skeleton_dungeon/arch.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/skeleton_dungeon/arch.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/skeleton_dungeon/bridge.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/skeleton_dungeon/bridge.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/skeleton_dungeon/bridge_big.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/skeleton_dungeon/bridge_big.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/skeleton_dungeon/stair2.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/skeleton_dungeon/stair2.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/small_dungeon_loot_piles.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/small_dungeon_loot_piles.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/big_stairs.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/big_stairs.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/floor1_hall.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/floor1_hall.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/floor1_room.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/floor1_room.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/floor2_hall.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/floor2_hall.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/floor2_room.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/floor2_room.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/terminators/big_stairs_terminators.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/terminators/big_stairs_terminators.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/tombstones.json](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/data/betterdungeons/worldgen/template_pool/zombie_dungeon/tombstones.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Fortresses-forge-1.16/src/main/resources/data/betterfortresses/worldgen/template_pool/battle_bridge.json](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/src/main/resources/data/betterfortresses/worldgen/template_pool/battle_bridge.json) — yungsapi:max_count_single_element
- [YUNGs-Better-Fortresses-forge-1.16/src/main/resources/data/minecraft/advancements/nether/find_fortress.json](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/src/main/resources/data/minecraft/advancements/nether/find_fortress.json) — yungsapi:structure_location
- [YUNGs-Better-Strongholds-forge-1.16/src/main/resources/data/minecraft/advancements/story/follow_ender_eye.json](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/resources/data/minecraft/advancements/story/follow_ender_eye.json) — yungsapi:structure_location

## Appendix C. Bootstrap, loader and modern-hook source anchors

- [yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/YungsApi.java](../reference/1.16.5/yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/YungsApi.java)
- [yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/init/YAModJigsaw.java](../reference/1.16.5/yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/init/YAModJigsaw.java)
- [yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/init/YAModCriteria.java](../reference/1.16.5/yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/init/YAModCriteria.java)
- [yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/init/YAModWorldgen.java](../reference/1.16.5/yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/init/YAModWorldgen.java)
- [yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/io/JSON.java](../reference/1.16.5/yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/io/JSON.java)
- [yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/world/jigsaw/JigsawManager.java](../reference/1.16.5/yungs-api/src/main/java/com/yungnickyoung/minecraft/yungsapi/world/jigsaw/JigsawManager.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/init/BCCarver.java](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/init/BCCarver.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/CarvingContext.java](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/CarvingContext.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/BetterCavesCarver.java](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/BetterCavesCarver.java)
- [YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/config/io/ConfigLoader.java](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/java/com/yungnickyoung/minecraft/bettercaves/config/io/ConfigLoader.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/init/BDModStructures.java](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/init/BDModStructures.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/init/BDModProcessors.java](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/init/BDModProcessors.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/DungeonContext.java](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/world/DungeonContext.java)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/compat/QuarkCompat.java](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterdungeons/compat/QuarkCompat.java)
- [YUNGs-Better-Fortresses-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterfortresses/init/BFModStructures.java](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterfortresses/init/BFModStructures.java)
- [YUNGs-Better-Fortresses-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterfortresses/world/BetterFortressStructure.java](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterfortresses/world/BetterFortressStructure.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/init/ModStructures.java](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/init/ModStructures.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/BetterMineshaftStructure.java](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/BetterMineshaftStructure.java)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/BetterMineshaftGenerator.java](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/java/com/yungnickyoung/minecraft/bettermineshafts/world/generator/BetterMineshaftGenerator.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/BetterStrongholdStructure.java](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/BetterStrongholdStructure.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/jigsaw/JigsawManager.java](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/jigsaw/JigsawManager.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/jigsaw/JigsawConfig.java](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/world/jigsaw/JigsawConfig.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/mixin/EnderEyeBetterStrongholdLocatingMixin.java](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/mixin/EnderEyeBetterStrongholdLocatingMixin.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/mixin/LocateStrongholdCommandMixin.java](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/mixin/LocateStrongholdCommandMixin.java)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/mixin/VanillaStrongholdSpawnMixin.java](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/java/com/yungnickyoung/minecraft/betterstrongholds/mixin/VanillaStrongholdSpawnMixin.java)
- [YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/init/YEModFeatures.java](../reference/1.16.5/YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/init/YEModFeatures.java)
- [YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/init/YEModConfiguredFeatures.java](../reference/1.16.5/YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/init/YEModConfiguredFeatures.java)
- [YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/world/feature/AbstractTemplateFeature.java](../reference/1.16.5/YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/world/feature/AbstractTemplateFeature.java)
- [YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/world/placement/RngInitializerPlacement.java](../reference/1.16.5/YUNGs-Extras-forge-1.16/src/main/java/com/yungnickyoung/minecraft/yungsextras/world/placement/RngInitializerPlacement.java)

### Licensing and provenance anchors

- [yungs-api/build.gradle](../reference/1.16.5/yungs-api/build.gradle)
- [yungs-api/gradle.properties](../reference/1.16.5/yungs-api/gradle.properties)
- [yungs-api/LICENSE](../reference/1.16.5/yungs-api/LICENSE)
- [yungs-api/src/main/resources/META-INF/mods.toml](../reference/1.16.5/yungs-api/src/main/resources/META-INF/mods.toml)
- [YUNGs-Better-Caves-1.16/gradle.properties](../reference/1.16.5/YUNGs-Better-Caves-1.16/gradle.properties)
- [YUNGs-Better-Caves-1.16/LICENSE.md](../reference/1.16.5/YUNGs-Better-Caves-1.16/LICENSE.md)
- [YUNGs-Better-Caves-1.16/src/main/resources/META-INF/mods.toml](../reference/1.16.5/YUNGs-Better-Caves-1.16/src/main/resources/META-INF/mods.toml)
- [YUNGs-Better-Dungeons-forge-1.16/gradle.properties](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/gradle.properties)
- [YUNGs-Better-Dungeons-forge-1.16/LICENSE](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/LICENSE)
- [YUNGs-Better-Dungeons-forge-1.16/src/main/resources/META-INF/mods.toml](../reference/1.16.5/YUNGs-Better-Dungeons-forge-1.16/src/main/resources/META-INF/mods.toml)
- [YUNGs-Better-Fortresses-forge-1.16/gradle.properties](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/gradle.properties)
- [YUNGs-Better-Fortresses-forge-1.16/LICENSE](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/LICENSE)
- [YUNGs-Better-Fortresses-forge-1.16/src/main/resources/META-INF/mods.toml](../reference/1.16.5/YUNGs-Better-Fortresses-forge-1.16/src/main/resources/META-INF/mods.toml)
- [YUNGs-Better-Mineshafts-forge-1.16/gradle.properties](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/gradle.properties)
- [YUNGs-Better-Mineshafts-forge-1.16/LICENSE](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/LICENSE)
- [YUNGs-Better-Mineshafts-forge-1.16/src/main/resources/META-INF/mods.toml](../reference/1.16.5/YUNGs-Better-Mineshafts-forge-1.16/src/main/resources/META-INF/mods.toml)
- [YUNGs-Better-Strongholds-forge-1.16/gradle.properties](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/gradle.properties)
- [YUNGs-Better-Strongholds-forge-1.16/LICENSE](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/LICENSE)
- [YUNGs-Better-Strongholds-forge-1.16/src/main/resources/META-INF/mods.toml](../reference/1.16.5/YUNGs-Better-Strongholds-forge-1.16/src/main/resources/META-INF/mods.toml)
- [YUNGs-Extras-forge-1.16/gradle.properties](../reference/1.16.5/YUNGs-Extras-forge-1.16/gradle.properties)
- [YUNGs-Extras-forge-1.16/LICENSE](../reference/1.16.5/YUNGs-Extras-forge-1.16/LICENSE)
- [YUNGs-Extras-forge-1.16/src/main/resources/META-INF/mods.toml](../reference/1.16.5/YUNGs-Extras-forge-1.16/src/main/resources/META-INF/mods.toml)
