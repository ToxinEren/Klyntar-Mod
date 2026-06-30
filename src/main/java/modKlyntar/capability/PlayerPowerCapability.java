package modKlyntar.capability;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import modKlyntar.network.ModNetwork.SyncVenomModelPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber
public class PlayerPowerCapability {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Capability<PlayerPower> PLAYER_POWER = CapabilityManager.get(new CapabilityToken<>() {});

    public static final String VENOM_TAG = "Klyntar.Venom";
    public static final String CARNAGE_TAG = "Klyntar.Carnage";

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerPower.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(MyMod.MOD_ID, "player_power"), new PlayerPowerProvider());
        }
    }

    @SubscribeEvent
    public static void playerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(PLAYER_POWER).ifPresent(oldPower -> {
            event.getEntity().getCapability(PLAYER_POWER).ifPresent(newPower -> {
                newPower.copyFrom(oldPower);
                if (event.getEntity() instanceof ServerPlayer serverPlayer && newPower.isTransformed()) {
                    newPower.applyTransformation(serverPlayer);
                }
            });
        });
    }

    public static void infectPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            infectPlayer(serverPlayer);
            return;
        }
        player.getCapability(PLAYER_POWER).ifPresent(power -> {
            power.setInfected(true);
            power.applyPassivePowers(player);
        });
    }

    public static void infectPlayer(ServerPlayer player) {
        PlayerPower fallbackPower = new PlayerPower();
        PlayerPower power = player.getCapability(PLAYER_POWER).orElse(fallbackPower);
        if (power == fallbackPower) {
            LOGGER.warn("Klyntar player_power capability missing on {}; applying Venom without persistent capability", player.getGameProfile().getName());
        }
        {
            power.setInfected(true);
            power.setForm("venom");
            power.setTransformed(true);
            power.applyTransformation(player);
            setInfectionScore(player, true);
        }
    }

    public static void transformPlayer(ServerPlayer player) {
        transformPlayer(player, "venom");
    }

    public static void transformPlayer(ServerPlayer player, String form) {
        PlayerPower power = player.getCapability(PLAYER_POWER).orElse(new PlayerPower());
        power.setInfected(true);
        power.setForm(form);
        power.setTransformed(true);
        power.applyTransformation(player);
    }

    public static void revertPlayer(ServerPlayer player) {
        player.getCapability(PLAYER_POWER).ifPresent(power -> {
            power.setTransformed(false);
            power.setForm("");
            power.removeTransformation(player);
        });
    }

    private static void syncPalladiumPower(ServerPlayer player, String powerPath) {
        callPalladiumSuperpower("removeSuperpower", player, "venom");
        callPalladiumSuperpower("removeSuperpower", player, "carnage");
        if (!callPalladiumSuperpower("addSuperpower", player, powerPath)
                && !callPalladiumSuperpower("hasSuperpower", player, powerPath)) {
            LOGGER.error("Palladium did not add superpower mymod:{} to {}", powerPath, player.getGameProfile().getName());
            player.displayClientMessage(Component.literal("[Klyntar] ERRORE: Palladium non ha assegnato mymod:" + powerPath), false);
        } else {
            LOGGER.info("Palladium superpower mymod:{} synced to {}", powerPath, player.getGameProfile().getName());
            player.displayClientMessage(Component.literal("[Klyntar] Superpower applicato: mymod:" + powerPath), false);
        }
        runServerCommand(player, "ability unlock " + player.getGameProfile().getName() + " mymod:" + powerPath + " all");
    }

    private static void removePalladiumPower(ServerPlayer player, String powerPath) {
        callPalladiumSuperpower("removeSuperpower", player, powerPath);
    }

    private static boolean callPalladiumSuperpower(String methodName, ServerPlayer player, String powerPath) {
        try {
            Class<?> utilClass = Class.forName("net.threetag.palladium.power.SuperpowerUtil");
            Method method = utilClass.getMethod(methodName, LivingEntity.class, ResourceLocation.class);
            Object result = method.invoke(null, player, new ResourceLocation(MyMod.MOD_ID, powerPath));
            return !(result instanceof Boolean booleanResult) || booleanResult;
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Unable to call Palladium SuperpowerUtil.{} for mymod:{}", methodName, powerPath, exception);
            return false;
        }
    }

    private static void runServerCommand(ServerPlayer player, String command) {
        if (player.getServer() != null) {
            player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
        }
    }

    private static void setInfectionScore(ServerPlayer player, boolean infected) {
        runServerCommand(player, "scoreboard objectives add Klyntar.SymbioteInfected dummy");
        runServerCommand(player, "scoreboard players set " + player.getGameProfile().getName() + " Klyntar.SymbioteInfected " + (infected ? 1 : 0));
    }

    private static void applyVenomSuperpowerBridge(ServerPlayer player) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(MyMod.MOD_ID, "venom_infection"));
        if (effect != null) {
            player.addEffect(new MobEffectInstance(effect, 80, 0, false, false, false));
        }
    }

    public static class PlayerPower implements INBTSerializable<CompoundTag> {
        private boolean infected;
        private boolean transformed;
        private String form = "";

        public boolean isInfected() {
            return infected;
        }

        public void setInfected(boolean infected) {
            this.infected = infected;
        }

        public boolean isTransformed() {
            return transformed;
        }

        public void setTransformed(boolean transformed) {
            this.transformed = transformed;
        }

        public String getForm() {
            return form;
        }

        public void setForm(String form) {
            this.form = normalizeForm(form);
        }

        public void applyPassivePowers(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
        }

        public void applyTransformation(ServerPlayer player) {
            boolean carnage = "carnage".equals(form);
            player.getTags().remove(VENOM_TAG);
            player.getTags().remove(CARNAGE_TAG);
            player.addTag(carnage ? CARNAGE_TAG : VENOM_TAG);

            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, carnage ? 3 : 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, carnage ? 2 : 3, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, carnage ? 5 : 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, carnage ? 1 : 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, Integer.MAX_VALUE, carnage ? 3 : 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));

            if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(carnage ? 36.0D : 40.0D);
                player.setHealth(player.getMaxHealth());
            }
            if (player.getAttribute(Attributes.ARMOR) != null) {
                player.getAttribute(Attributes.ARMOR).setBaseValue(carnage ? 10.0D : 15.0D);
            }
            if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(carnage ? 10.0D : 8.0D);
            }
            syncPalladiumPower(player, carnage ? "carnage" : "venom");
            if (!carnage) {
                applyVenomSuperpowerBridge(player);
            }
            ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomModelPacket(carnage ? "carnage" : "venom"));
        }

        public void removeTransformation(ServerPlayer player) {
            player.getTags().remove(VENOM_TAG);
            player.getTags().remove(CARNAGE_TAG);
            player.removeEffect(MobEffects.REGENERATION);
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            player.removeEffect(MobEffects.DAMAGE_BOOST);
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            player.removeEffect(MobEffects.NIGHT_VISION);
            player.removeEffect(MobEffects.JUMP);
            player.setInvulnerable(false);

            if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
                player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            }
            if (player.getAttribute(Attributes.ARMOR) != null) {
                player.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
            }
            if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1.0D);
            }
            removePalladiumPower(player, "venom");
            removePalladiumPower(player, "carnage");
            setInfectionScore(player, false);
            ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomModelPacket(""));
        }

        public void handleDamage(LivingHurtEvent event) {
            if (!transformed) {
                return;
            }

            if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                event.getEntity().removeEffect(MobEffects.REGENERATION);
                return;
            }

            event.setAmount(event.getAmount() * 0.35F);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("infected", infected);
            tag.putBoolean("transformed", transformed);
            tag.putString("form", form);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            infected = nbt.getBoolean("infected");
            transformed = nbt.getBoolean("transformed");
            form = normalizeForm(nbt.getString("form"));
        }

        public void copyFrom(PlayerPower source) {
            this.infected = source.infected;
            this.transformed = source.transformed;
            this.form = source.form;
        }

        private static String normalizeForm(String form) {
            if (form == null) {
                return "";
            }
            String normalized = form.trim().toLowerCase();
            return "carnage".equals(normalized) ? "carnage" : "venom";
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(PLAYER_POWER).ifPresent(power -> {
                if (power.isInfected()) {
                    power.handleDamage(event);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(PLAYER_POWER).ifPresent(power -> {
            if (power.isInfected()) {
                if (!power.isTransformed()) {
                    power.setForm("venom");
                    power.setTransformed(true);
                }
                power.applyTransformation(player);
                setInfectionScore(player, true);
            }
        });
    }
}

