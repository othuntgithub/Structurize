package com.ldtteam.structurize.pipeline.build;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import com.ldtteam.structurize.pipeline.build.ComponentPlacer.BlockStateComponentPlacer;
import com.ldtteam.structurize.pipeline.build.ComponentPlacer.TileEntityComponentPlacer;
import com.ldtteam.structurize.util.BlockStateWithTileEntity;
import com.ldtteam.structurize.util.FastIterator;
import com.ldtteam.structurize.util.ItemStackList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StagedPlacerUtils
{
    public static class BSwithTEplaceAction extends PlaceAction<BlockStateWithTileEntity>
    {
        protected BSwithTEplaceAction(
            final BlockStateWithTileEntity blockStateWithTileEntity,
            final BlockStateComponentPlacer bsp,
            final TileEntityComponentPlacer tep,
            final World world,
            final BlockPos pos)
        {
            super(blockStateWithTileEntity.getTileEntity() == null ? () -> {
                return bsp.getRequirements(blockStateWithTileEntity.getBlockState(), world, pos);
            } : () -> {
                final ItemStackList result = new ItemStackList();
                result.addAll(bsp.getRequirements(blockStateWithTileEntity.getBlockState(), world, pos));
                result.addAll(tep.getRequirements(blockStateWithTileEntity.getTileEntity(), world, pos));
                return (ArrayList<ItemStack>) result;
            }, blockStateWithTileEntity.getTileEntity() == null ? () -> {
                bsp.place(blockStateWithTileEntity.getBlockState(), world, pos);
            } : () -> {
                bsp.place(blockStateWithTileEntity.getBlockState(), world, pos);
                tep.place(blockStateWithTileEntity.getTileEntity(), world, pos);
            }, blockStateWithTileEntity, null, world, pos);
        }
    }

    public static class PlaceAction<T>
    {
        private final Supplier<List<ItemStack>> requirements;
        private final Runnable action;
        private final T objectToPlace;
        private final ResourceLocation objectRegistryName;
        private final World world;
        private final BlockPos pos;

        protected PlaceAction(
            final Supplier<List<ItemStack>> requirements,
            final Runnable action,
            final T objectToPlace,
            final ResourceLocation objectRegistryName,
            final World world,
            final BlockPos pos)
        {
            this.requirements = requirements;
            this.action = action;
            this.objectToPlace = objectToPlace;
            this.objectRegistryName = objectRegistryName;
            this.world = world;
            this.pos = pos;
        }

        public Supplier<List<ItemStack>> getRequirements()
        {
            return requirements;
        }

        public void perform()
        {
            action.run();
        }

        public T getObjectToPlace()
        {
            return objectToPlace;
        }

        public ResourceLocation getObjectRegistryName()
        {
            return objectRegistryName;
        }

        public World getWorld()
        {
            return world;
        }

        public BlockPos getPos()
        {
            return pos;
        }
    }

    public static class ActionIterator<I, T> implements FastIterator<PlaceAction<T>>
    {
        private final Iterator<I> backingIterator;
        private final Function<I, PlaceAction<T>> iteratorTransformer;

        protected ActionIterator(final Iterator<I> backingIterator, final Function<I, PlaceAction<T>> iteratorTransformer)
        {
            this.backingIterator = backingIterator;
            this.iteratorTransformer = iteratorTransformer;
        }

        @Override
        public boolean hasNext()
        {
            return backingIterator.hasNext();
        }

        @Override
        public PlaceAction<T> next()
        {
            PlaceAction<T> result = null;
            while (result == null && hasNext())
            {
                result = iteratorTransformer.apply(backingIterator.next());
            }
            return result;
        }

        @Override
        public void fastConsume(final PlaceAction<T> placeAction)
        {
            placeAction.perform();
        }

        protected ActionIterator<?, T> successor()
        {
            return null;
        }
    }
}