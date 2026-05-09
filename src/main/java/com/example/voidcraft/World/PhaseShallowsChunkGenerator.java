package com.example.voidcraft.World;

import com.example.voidcraft.Block.ModBlock;
import com.example.voidcraft.VoidCraft;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class PhaseShallowsChunkGenerator extends ChunkGenerator {
    public static final int SURFACE_BASE_Y = 64;
    public static final int SURFACE_MIN_Y = 60;
    public static final int SURFACE_MAX_Y = 68;
    public static final int SAFE_SPAWN_EXTRA_Y = 2;

    private static final int BROAD_CELL_SIZE = 64;
    private static final int DETAIL_CELL_SIZE = 24;
    private static final Identifier HEIGHT_RANDOM = Identifier.fromNamespaceAndPath(VoidCraft.MODID, "phase_shallows_height");
    public static final MapCodec<PhaseShallowsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("biome").forGetter(generator -> generator.biome)
    ).apply(instance, instance.stable(PhaseShallowsChunkGenerator::new)));

    private final Holder<Biome> biome;

    public PhaseShallowsChunkGenerator(Holder<Biome> biome) {
        super(new FixedBiomeSource(biome), ignored -> BiomeGenerationSettings.EMPTY);
        this.biome = biome;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetLookup, RandomState randomState, long seed) {
        return ChunkGeneratorStructureState.createForFlat(randomState, seed, this.biomeSource, Stream.empty());
    }

    @Override
    public void createStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager,
            ResourceKey<Level> level
    ) {
    }

    @Override
    public void createReferences(WorldGenLevel level, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
            ServerLevel level,
            HolderSet<Structure> structure,
            BlockPos pos,
            int searchRadius,
            boolean skipKnownStructures
    ) {
        return null;
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
    }

    @Override
    public void applyCarvers(
            WorldGenRegion level,
            long seed,
            RandomState random,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int chunkMinY = chunk.getMinY();
        int chunkMaxY = chunk.getMinY() + chunk.getHeight() - 1;
        int minFillY = Math.max(chunkMinY, this.getMinY());

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunk.getPos().getMinBlockX() + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunk.getPos().getMinBlockZ() + localZ;
                int surfaceY = Mth.clamp(surfaceHeight(worldX, worldZ, randomState), minFillY, chunkMaxY);

                for (int y = minFillY; y <= surfaceY; y++) {
                    BlockState state = terrainState(y, surfaceY, minFillY);
                    chunk.setBlockState(mutablePos.set(localX, y, localZ), state);
                    oceanFloor.update(localX, y, localZ, state);
                    worldSurface.update(localX, y, localZ, state);
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int surfaceY = Mth.clamp(surfaceHeight(x, z, random), Math.max(level.getMinY(), this.getMinY()), level.getMaxY());
        BlockState surface = surfaceState();
        return type.isOpaque().test(surface) ? surfaceY + 1 : level.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        int minY = height.getMinY();
        int maxY = height.getMinY() + height.getHeight() - 1;
        int minFillY = Math.max(minY, this.getMinY());
        int surfaceY = Mth.clamp(surfaceHeight(x, z, random), minFillY, maxY);
        BlockState[] states = new BlockState[height.getHeight()];

        for (int i = 0; i < states.length; i++) {
            int y = minY + i;
            states[i] = y >= minFillY && y <= surfaceY
                    ? terrainState(y, surfaceY, minFillY)
                    : Blocks.AIR.defaultBlockState();
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return Math.min(level.getMaxY(), SURFACE_MAX_Y + SAFE_SPAWN_EXTRA_Y);
    }

    @Override
    public int getGenDepth() {
        return 128;
    }

    @Override
    public int getSeaLevel() {
        return SURFACE_MIN_Y - 8;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Phase shallows surface: " + surfaceHeight(pos.getX(), pos.getZ(), random));
    }

    private static int surfaceHeight(int x, int z, RandomState randomState) {
        double broad = smoothNoise(x, z, BROAD_CELL_SIZE, 0, randomState) * 3.2D;
        double detail = smoothNoise(x, z, DETAIL_CELL_SIZE, 1, randomState) * 1.4D;
        int height = SURFACE_BASE_Y + Mth.floor(broad + detail + 0.5D);
        return Mth.clamp(height, SURFACE_MIN_Y, SURFACE_MAX_Y);
    }

    private static double smoothNoise(int x, int z, int cellSize, int salt, RandomState randomState) {
        int cellX = Math.floorDiv(x, cellSize);
        int cellZ = Math.floorDiv(z, cellSize);
        double localX = Math.floorMod(x, cellSize) / (double) cellSize;
        double localZ = Math.floorMod(z, cellSize) / (double) cellSize;
        double sx = smoothStep(localX);
        double sz = smoothStep(localZ);

        double n00 = cornerNoise(cellX, cellZ, salt, randomState);
        double n10 = cornerNoise(cellX + 1, cellZ, salt, randomState);
        double n01 = cornerNoise(cellX, cellZ + 1, salt, randomState);
        double n11 = cornerNoise(cellX + 1, cellZ + 1, salt, randomState);
        double nx0 = Mth.lerp(sx, n00, n10);
        double nx1 = Mth.lerp(sx, n01, n11);
        return Mth.lerp(sz, nx0, nx1);
    }

    private static double cornerNoise(int cellX, int cellZ, int salt, RandomState randomState) {
        RandomSource random = randomState.getOrCreateRandomFactory(HEIGHT_RANDOM).at(cellX, salt, cellZ);
        return random.nextDouble() * 2.0D - 1.0D;
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static BlockState terrainState(int y, int surfaceY, int minFillY) {
        return surfaceState();
    }

    private static BlockState surfaceState() {
        return ModBlock.BLACK_BLOCK.get().defaultBlockState();
    }
}
