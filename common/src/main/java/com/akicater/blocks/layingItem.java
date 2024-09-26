
package com.akicater.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.akicater.ItemPlacerCommon.*;

public class layingItem extends Block implements Waterloggable, BlockEntityProvider {
    public static BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public layingItem(Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, false));
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED))
            world.createAndScheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected void spawnBreakParticles(World world, PlayerEntity player, BlockPos pos, BlockState state) {
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new layingItemBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        layingItemBlockEntity blockEntity = (layingItemBlockEntity) world.getChunk(pos).getBlockEntity(pos);
        if (blockEntity != null && (RETRIEVE_KEY.isUnbound() || RETRIEVE_KEY.isPressed())) {
            blockEntity.dropItem(getDirection(hit));
            if (isInventoryClear(blockEntity.inventory)) {
                if (state.get(WATERLOGGED)) {
                    world.setBlockState(pos, Blocks.WATER.getDefaultState());
                } else {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_PAINTING_PLACE, SoundCategory.BLOCKS, 1f, 2f, true);
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    Boolean isInventoryClear(DefaultedList<ItemStack> inventory) {
        for (ItemStack itemStack : inventory) {
            if (!ItemStack.EMPTY.equals(itemStack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof layingItemBlockEntity entity) {
                for (int i = 0; i < 6; i++) {

                    ItemStack itemStack = entity.inventory.get(i);

                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), itemStack);

                    world.updateComparators(pos, this);
                }
                entity.clear();
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView blockView, BlockPos pos, ShapeContext context) {
        if (!RETRIEVE_KEY.isUnbound() && !RETRIEVE_KEY.isPressed() && !STOP_SCROLLING_KEY.isPressed())
            return VoxelShapes.empty();
        layingItemBlockEntity entity = (layingItemBlockEntity) blockView.getBlockEntity(pos);
        List<VoxelShape> tempShape = new ArrayList<>();
        if (entity != null) {
            if (!entity.inventory.get(0).isEmpty()) {
                tempShape.add(VoxelShapes.cuboid(0.125f, 0.125f, 0.875f, 0.875f, 0.875f, 1.0f));
            }
            if (!entity.inventory.get(1).isEmpty()) {
                tempShape.add(VoxelShapes.cuboid(0.125f, 0.125f, 0.0f, 0.875f, 0.875f, 0.125f));
            }
            if (!entity.inventory.get(2).isEmpty()) {
                tempShape.add(VoxelShapes.cuboid(0.875f, 0.125f, 0.125f, 1.0f, 0.875f, 0.875f));
            }
            if (!entity.inventory.get(3).isEmpty()) {
                tempShape.add(VoxelShapes.cuboid(0.0f, 0.125f, 0.125f, 0.125f, 0.875f, 0.875f));
            }
            if (!entity.inventory.get(4).isEmpty()) {
                tempShape.add(VoxelShapes.cuboid(0.125f, 0.875f, 0.125f, 0.875f, 1.0f, 0.875f));
            }
            if (!entity.inventory.get(5).isEmpty()) {
                tempShape.add(VoxelShapes.cuboid(0.125f, 0.0f, 0.125f, 0.875f, 0.125f, 0.875f));
            }
        }
        Optional<VoxelShape> shape = tempShape.stream().reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR));
        if (shape.isPresent()) return shape.get();
        else return VoxelShapes.cuboid(0.125f, 0.0f, 0.125f, 0.875f, 0.125f, 0.875f);
    }
}