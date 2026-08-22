package modKlyntar.capability;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import modKlyntar.network.ModNetwork.SyncVenomModelPacket;
import modKlyntar.player.VenomPlayerSizeHandler;
import modKlyntar.player.VenomSymbioteSystemsHandler;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Collection;

@Mod.EventBusSubscriber
public class PlayerPowerCapability {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Capability<PlayerPower> PLAYER_POWER = CapabilityManager.get(new CapabilityToken<>() {});

    public static final String VENOM_TAG = "Klyntar.Venom";
    public static final String CARNAGE_TAG = "Klyntar.Carnage";
    /** 1 mentre il giocatore e' in forma anti-venom: lo leggono i renderer dei tentacoli */
    public static final String ANTIVENOM_OBJECTIVE = "Klyntar.AntiVenom";
    private static final String PALLADIUM_SYNC_KEY = "Klyntar.PalladiumPowerSynced";
    /** tutti i simbionti della mod: se Palladium ne riconosce gia' uno non se ne assegna un altro */
    private static final String[] FORME_SIMBIONTE = {"venom", "carnage", "antivenom", "toxin", "wip"};

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

    /**
     * Riallinea il giocatore al superpotere che Palladium gli riconosce davvero.
     *
     * <p>E' il cuore del sistema da quando il comando {@code /transform} non esiste piu': la
     * forma non la decide piu' nessun comando nostro, la si legge da Palladium e la si
     * insegue. Vale per qualunque strada il potere sia arrivato — la barra, un comando di
     * Palladium, un altro addon.</p>
     */
    private static void seguiPalladium(ServerPlayer player) {
        String daPalladium = formaSuPalladium(player);
        player.getCapability(PLAYER_POWER).ifPresent(power -> {
            String attuale = power.isTransformed() ? power.getForm() : "";

            if (daPalladium == null) {
                if (!attuale.isEmpty()) {
                    power.setTransformed(false);
                    power.setForm("");
                    power.removeTransformation(player);
                }
                return;
            }

            if (daPalladium.equals(attuale)) {
                return;
            }

            power.setInfected(true);
            power.setForm(daPalladium);
            power.setTransformed(true);
            // il potere ce l'ha gia': si allinea la chiave perche' non venga riassegnato
            player.getPersistentData().putString(PALLADIUM_SYNC_KEY, daPalladium);
            power.applyTransformation(player);
            LOGGER.info("Forma allineata a klyntars:{} per {}",
                    daPalladium, player.getGameProfile().getName());
        });
    }

    /** Rimette in scena la forma che il giocatore ha gia', senza cambiarla. */
    public static void riapplicaForma(ServerPlayer player) {
        player.getCapability(PLAYER_POWER).ifPresent(power -> {
            if (power.isTransformed()) {
                power.applyTransformation(player);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        // mezzo secondo di passo: seguire il potere non richiede di guardarlo a ogni tick
        if (player.tickCount % 10 == 0) {
            seguiPalladium(player);
            modKlyntar.symbiote.SymbioteState.assicuraAffinita(player);
        }
    }

    public static void revertPlayer(ServerPlayer player) {
        player.getCapability(PLAYER_POWER).ifPresent(power -> {
            power.setTransformed(false);
            power.setForm("");
            power.removeTransformation(player);
        });
    }

    /**
     * Il simbionte che Palladium riconosce davvero al giocatore, o null se non ne ha nessuno.
     * E' la fonte autorevole: la capability puo' essere rimasta indietro, Palladium no.
     */
    public static String formaSuPalladium(ServerPlayer player) {
        try {
            Method metodo = Class.forName("net.threetag.palladium.power.SuperpowerUtil")
                    .getMethod("getSuperpowerIds", LivingEntity.class);
            if (metodo.invoke(null, player) instanceof Collection<?> poteri) {
                for (Object potere : poteri) {
                    String id = String.valueOf(potere);
                    for (String forma : FORME_SIMBIONTE) {
                        if (id.endsWith(":" + forma)) {
                            return forma;
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException eccezione) {
            LOGGER.error("Impossibile leggere i superpoteri di Palladium per {}",
                    player.getGameProfile().getName(), eccezione);
        }
        return null;
    }

    private static void syncPalladiumPower(ServerPlayer player, String powerPath) {
        for (String forma : FORME_SIMBIONTE) {
            callPalladiumSuperpower("removeSuperpower", player, forma);
        }
        if (!callPalladiumSuperpower("addSuperpower", player, powerPath)
                && !callPalladiumSuperpower("hasSuperpower", player, powerPath)) {
            LOGGER.error("Palladium did not add superpower klyntars:{} to {}", powerPath, player.getGameProfile().getName());
            player.displayClientMessage(Component.literal("[Klyntar] ERRORE: Palladium non ha assegnato klyntars:" + powerPath), false);
        } else {
            LOGGER.info("Palladium superpower klyntars:{} synced to {}", powerPath, player.getGameProfile().getName());
            player.displayClientMessage(Component.literal("[Klyntar] Superpower applicato: klyntars:" + powerPath), false);
            player.getPersistentData().putString(PALLADIUM_SYNC_KEY, powerPath);
        }
        runServerCommand(player, "ability unlock " + player.getGameProfile().getName() + " klyntars:" + powerPath + " all");
    }

    private static void removePalladiumPower(ServerPlayer player, String powerPath) {
        callPalladiumSuperpower("removeSuperpower", player, powerPath);
        if (powerPath.equals(player.getPersistentData().getString(PALLADIUM_SYNC_KEY))) {
            player.getPersistentData().remove(PALLADIUM_SYNC_KEY);
        }
    }

    private static boolean callPalladiumSuperpower(String methodName, ServerPlayer player, String powerPath) {
        try {
            Class<?> utilClass = Class.forName("net.threetag.palladium.power.SuperpowerUtil");
            Method method = utilClass.getMethod(methodName, LivingEntity.class, ResourceLocation.class);
            Object result = method.invoke(null, player, new ResourceLocation(MyMod.MOD_ID, powerPath));
            return !(result instanceof Boolean booleanResult) || booleanResult;
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Unable to call Palladium SuperpowerUtil.{} for klyntars:{}", methodName, powerPath, exception);
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

    /**
     * Segna la forma anti-venom su un obiettivo, che il client legge per disegnare i tentacoli
     * bianchi invece che neri. Serve un obiettivo perche' i tentacoli si disegnano anche per gli
     * altri giocatori, non solo per il proprio.
     */
    private static void setAntiVenomScore(ServerPlayer player, boolean anti) {
        runServerCommand(player, "scoreboard objectives add " + ANTIVENOM_OBJECTIVE + " dummy");
        runServerCommand(player, "scoreboard players set " + player.getGameProfile().getName()
                + " " + ANTIVENOM_OBJECTIVE + " " + (anti ? 1 : 0));
    }

    /**
     * Accende l'effetto {@code klyntars:venom_infection}, che dallo script del pack assegna
     * il superpotere Venom finche' dura.
     *
     * <p>Chiamarlo per una forma diversa da venom significa assegnare Venom a chi ha gia'
     * un altro simbionte.</p>
     */
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
            // il vecchio carnage e' diventato WIP: la forma "carnage" ora e' una copia di Venom,
            // e le sue abilita' passano dai gestori Java che cercano il tag di Venom
            boolean carnage = "wip".equals(form);
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
            String powerPath = form.isEmpty() ? "venom" : form;
            if (!powerPath.equals(player.getPersistentData().getString(PALLADIUM_SYNC_KEY))) {
                syncPalladiumPower(player, powerPath);
            }
            // il ponte mette addosso l'effetto venom_infection, e quell'effetto assegna
            // klyntars:venom a ogni tick: va acceso solo quando la forma e' davvero venom,
            // altrimenti carnage e anti-venom si ritrovano venom appiccicato sopra
            if ("venom".equals(powerPath)) {
                applyVenomSuperpowerBridge(player);
            }
            setAntiVenomScore(player, "antivenom".equals(powerPath));
            VenomSymbioteSystemsHandler.resetHunger(player);
            ModNetwork.syncSymbioteForm(player, powerPath);
            ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomModelPacket(powerPath));
            player.refreshDimensions();
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
            for (String forma : FORME_SIMBIONTE) {
                removePalladiumPower(player, forma);
            }
            setInfectionScore(player, false);
            setAntiVenomScore(player, false);
            VenomSymbioteSystemsHandler.resetHunger(player);
            // senza simbionte l'indebolimento non ha piu' oggetto, e lasciarlo acceso
            // significherebbe ritrovarselo addosso alla prossima trasformazione
            VenomSymbioteSystemsHandler.clearWeakness(player);
            ModNetwork.syncSymbioteForm(player, "");
            ModNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomModelPacket(""));
            player.refreshDimensions();
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
            // forma vuota vuol dire "nessuna trasformazione": non va tradotta in venom
            if (normalized.isEmpty()) {
                return "";
            }
            if ("carnage".equals(normalized)) {
                return "carnage";
            }
            if ("wip".equals(normalized)) {
                return "wip";
            }
            if ("toxin".equals(normalized)) {
                return "toxin";
            }
            // anti-venom e' Venom con un'altra pelle: stesse abilita', stesse statistiche
            if ("antivenom".equals(normalized) || "anti-venom".equals(normalized)) {
                return "antivenom";
            }
            return "venom";
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
            if (!power.isInfected()) {
                return;
            }
            setInfectionScore(player, true);
            String giaAttivo = formaSuPalladium(player);
            if (giaAttivo != null) {
                // Palladium conserva il potere tra una sessione e l'altra: se il giocatore e' gia'
                // carnage, toxin o anti-venom non gli si rimette Venom, si allinea la capability
                power.setForm(giaAttivo);
                power.setTransformed(true);
                player.getPersistentData().putString(PALLADIUM_SYNC_KEY, giaAttivo);
            }
            // chi si e' ritrasformato indietro deve restare tale: prima di questa guardia
            // ogni rientro nel mondo rimetteva addosso Venom a un giocatore solo infetto
            if (power.isTransformed()) {
                power.applyTransformation(player);
            }
        });
    }
}

