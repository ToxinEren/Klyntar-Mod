package modKlyntar.entity.custom;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import modKlyntar.MyMod;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CustomArrowEntity extends AbstractArrow {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MyMod.MOD_ID, "textures/entity/custom_arrow.png");

    public CustomArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }
    
    public CustomArrowEntity(EntityType<? extends CustomArrowEntity> entityType, Level level, LivingEntity shooter) {
        super(entityType, shooter, level);
    }

    public ResourceLocation getTextureResource() {
        return TEXTURE;
    }

	@Override
	protected ItemStack getPickupItem() {
		// TODO Auto-generated method stub
		return null;
	}

    // Aggiungi qui altri comportamenti personalizzati della freccia se necessario
}
