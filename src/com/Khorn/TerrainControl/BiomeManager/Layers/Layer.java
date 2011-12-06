package com.Khorn.TerrainControl.BiomeManager.Layers;

import com.Khorn.TerrainControl.Configuration.BiomeConfig;
import com.Khorn.TerrainControl.Configuration.WorldConfig;
import net.minecraft.server.*;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Layer
{
    protected long b;
    protected Layer a;
    private long c;
    protected long d;


    /*

   LayerIsland - chance to big land
   LayerLandRandom - a(3) - chance to increase big land
   GenLayerIcePlains - chance to ice
   GenLayerMushroomIsland - chance to mushroom island




   biome:
   1) is island
   2) size
   3) chance
   4) is shore
   5) color
   6) temperature
   7) downfall
   8) is snow biome
   9) Have rivers


   world
   1) chance to lands
   2) size of big lands
   3) chance to increase lands
   4) Chance for ice area
   5) Ice area size
   6) Rivers
   7) Rivers size





    */


    protected static final int BiomeBits = 63;
    protected static final int LandBit = 64;
    protected static final int RiverBits = 768;
    protected static final int IceBit = 128;
    protected static final int IslandBit = 1024;

    public static Layer[] a(long paramLong, WorldConfig config)
    {

        /*int BigLandSize = 2;  //default 0, more - smaller
        int ChanceToIncreaseLand = 6; //default 4
        int MaxDepth = 10;     */


        BiomeBase[][] NormalBiomeMap = new BiomeBase[config.GenerationDepth + 1][];
        BiomeBase[][] IceBiomeMap = new BiomeBase[config.GenerationDepth + 1][];


        for (int i = 0; i < config.GenerationDepth + 1; i++)
        {
            ArrayList<BiomeBase> normalBiomes = new ArrayList<BiomeBase>(config.normalBiomesRarity);
            ArrayList<BiomeBase> iceBiomes = new ArrayList<BiomeBase>(config.iceBiomesRarity);
            for (BiomeConfig biomeConfig : config.biomeConfigs)
            {
                if (biomeConfig.BiomeSize != i)
                    continue;
                if (config.NormalBiomes.contains(biomeConfig.Name))
                {
                    for (int t = 0; t < biomeConfig.BiomeRarity; t++)
                        normalBiomes.add(biomeConfig.Biome);
                    config.normalBiomesRarity -= biomeConfig.BiomeRarity;
                }

                if (config.IceBiomes.contains(biomeConfig.Name))
                {
                    for (int t = 0; t < biomeConfig.BiomeRarity; t++)
                        iceBiomes.add(biomeConfig.Biome);
                    config.iceBiomesRarity -= biomeConfig.BiomeRarity;
                }

            }
            NormalBiomeMap[i] = normalBiomes.toArray(new BiomeBase[normalBiomes.size()]);
            IceBiomeMap[i] = iceBiomes.toArray(new BiomeBase[iceBiomes.size()]);

        }


        Layer MainLayer = new LayerEmpty(1L);


        for (int depth = 0; depth <= config.GenerationDepth; depth++)
        {

            MainLayer = new LayerZoom(2001 + depth, MainLayer);

            if (config.LandSize == depth)
            {
                MainLayer = new LayerLand(1L, MainLayer, config.LandRarity);
                MainLayer = new LayerZoomFuzzy(2000L, MainLayer);
            }

            if (depth < (config.LandSize + config.LandFuzzy))
                MainLayer = new LayerLandRandom(depth, MainLayer);


            if (NormalBiomeMap[depth].length != 0 || IceBiomeMap[depth].length != 0)
            {

                LayerBiome layerBiome = new LayerBiome(200, MainLayer);
                layerBiome.biomes = NormalBiomeMap[depth];
                layerBiome.ice_biomes = IceBiomeMap[depth];
                MainLayer = layerBiome;
            }


            if (config.IceSize == depth)
                MainLayer = new LayerIce(depth, MainLayer, config.IceRarity);

            if (config.RiverRarity == depth)
                MainLayer = new LayerRiverInit(155, MainLayer);

            if ((config.GenerationDepth - config.RiverSize) == depth)
                MainLayer = new LayerRiver(5 + depth, MainLayer);

            LayerBiomeBorder layerBiomeBorder = new LayerBiomeBorder(3000 + depth, MainLayer);
            boolean haveBorder = false;
            for (BiomeConfig biomeConfig : config.biomeConfigs)
            {
                if (biomeConfig.BiomeSize != depth)
                    continue;
                if (config.IsleBiomes.contains(biomeConfig.Name) && biomeConfig.IsleInBiome != null)
                {

                    LayerBiomeInBiome layerBiome = new LayerBiomeInBiome(4000 + depth, MainLayer);
                    layerBiome.biome = biomeConfig.Biome;
                    if (biomeConfig.IsleInBiome == BiomeBase.OCEAN)
                        layerBiome.inOcean = true;
                    else
                        layerBiome.inBiome = biomeConfig.IsleInBiome.F;
                    layerBiome.chance = 101 - biomeConfig.BiomeRarity;
                    MainLayer = layerBiome;
                }

                if (config.BorderBiomes.contains(biomeConfig.Name) && biomeConfig.BiomeIsBorder != null)
                {
                    haveBorder = true;
                    layerBiomeBorder.BiomeBorders[biomeConfig.BiomeIsBorder.F] = biomeConfig.Biome.F;
                }
            }


            if (haveBorder)
                MainLayer = layerBiomeBorder;


        }
        MainLayer = new LayerMix(1L, MainLayer, config);

        MainLayer = new LayerSmooth(400L, MainLayer);

        Layer TemperatureLayer = new LayerTemperature(MainLayer, config);
        TemperatureLayer = LayerSmoothZoom.a(1000L, TemperatureLayer, 2);

        Layer DownfallLayer = new LayerDownfall(MainLayer, config);
        DownfallLayer = LayerSmoothZoom.a(1000L, DownfallLayer, 2);


        Layer ZoomedLayer = new LayerZoomVoronoi(10L, MainLayer);

        //TemperatureLayer = new LayerTemperatureMix(TemperatureLayer, ZoomedLayer, 0, config);

        ZoomedLayer.b(paramLong);
        TemperatureLayer.b(paramLong);
        DownfallLayer.b(paramLong);

        return new Layer[]{MainLayer, ZoomedLayer, TemperatureLayer, DownfallLayer};
    }

    public Layer(long paramLong)
    {
        this.d = paramLong;
        this.d *= (this.d * 6364136223846793005L + 1442695040888963407L);
        this.d += paramLong;
        this.d *= (this.d * 6364136223846793005L + 1442695040888963407L);
        this.d += paramLong;
        this.d *= (this.d * 6364136223846793005L + 1442695040888963407L);
        this.d += paramLong;
    }

    public void b(long paramLong)
    {
        this.b = paramLong;
        if (this.a != null)
            this.a.b(paramLong);
        this.b *= (this.b * 6364136223846793005L + 1442695040888963407L);
        this.b += this.d;
        this.b *= (this.b * 6364136223846793005L + 1442695040888963407L);
        this.b += this.d;
        this.b *= (this.b * 6364136223846793005L + 1442695040888963407L);
        this.b += this.d;
    }

    public void a(long paramLong1, long paramLong2)
    {
        this.c = this.b;
        this.c *= (this.c * 6364136223846793005L + 1442695040888963407L);
        this.c += paramLong1;
        this.c *= (this.c * 6364136223846793005L + 1442695040888963407L);
        this.c += paramLong2;
        this.c *= (this.c * 6364136223846793005L + 1442695040888963407L);
        this.c += paramLong1;
        this.c *= (this.c * 6364136223846793005L + 1442695040888963407L);
        this.c += paramLong2;
    }

    protected int a(int paramInt)
    {
        int i = (int) ((this.c >> 24) % paramInt);
        if (i < 0)
            i += paramInt;
        this.c *= (this.c * 6364136223846793005L + 1442695040888963407L);
        this.c += this.b;
        return i;
    }

    public abstract int[] a(int paramInt1, int paramInt2, int paramInt3, int paramInt4);
}