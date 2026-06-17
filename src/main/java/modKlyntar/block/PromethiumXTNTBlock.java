package modKlyntar.block;

import javax.annotation.Nullable;
import modKlyntar.entity.custom.PrimedPromethiumXTnt;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PromethiumXTNTBlock extends Block {

    public PromethiumXTNTBlock() {
        super(BlockBehaviour.Properties.of().strength(3.0F).noOcclusion());
    }

    @Override
    public void onProjectileHit(Level world, BlockState state, BlockHitResult hitResult, Projectile projectile) {
        BlockPos blockpos = hitResult.getBlockPos();
        LivingEntity livingentity = projectile.getOwner() instanceof LivingEntity ? (LivingEntity)projectile.getOwner() : null;
        this.explode(world, blockpos, livingentity);
    }

    @Override
    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
        if (!world.isClientSide) {
            PrimedPromethiumXTnt tntEntity = new PrimedPromethiumXTnt(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, null);
            int fuseTime = tntEntity.getFuse();
            tntEntity.setFuse((short)(world.random.nextInt(fuseTime / 4) + fuseTime / 8));
            world.addFreshEntity(tntEntity);
        }
    }

    private void explode(Level world, BlockPos pos, @Nullable LivingEntity igniter) {
        if (!world.isClientSide) {
            PrimedPromethiumXTnt tntEntity = new PrimedPromethiumXTnt(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter);
            world.addFreshEntity(tntEntity);
            world.removeBlock(pos, false);
            world.playSound(null, tntEntity.getX(), tntEntity.getY(), tntEntity.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(Items.FIRE_CHARGE)) {
            return super.use(state, world, pos, player, hand, hit);
        } else {
            explode(world, pos, player);
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            Item item = itemstack.getItem();
            if (!player.isCreative()) {
                if (itemstack.is(Items.FLINT_AND_STEEL)) {
                    itemstack.hurtAndBreak(1, player, (p_220287_1_) -> {
                        p_220287_1_.broadcastBreakEvent(hand);
                    });
                } else {
                    itemstack.shrink(1);
                }
            }

            player.awardStat(Stats.ITEM_USED.get(item));
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }
}
