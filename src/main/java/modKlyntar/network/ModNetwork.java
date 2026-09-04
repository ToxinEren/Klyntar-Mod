package modKlyntar.network;

import modKlyntar.MyMod;
import modKlyntar.client.ClientEventHandler;
import modKlyntar.client.VenomTentaclesTraversalClientController;
import modKlyntar.client.renderer.VenomTentaclesTraversalRenderer;
import modKlyntar.player.VenomAttackBarrageHandler;
import modKlyntar.player.VenomSymbioteSystemsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.Optional;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MyMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerPackets() {
        int id = 0;
        INSTANCE.registerMessage(id++, SyncVenomModelPacket.class, SyncVenomModelPacket::encode, SyncVenomModelPacket::new, SyncVenomModelPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, SyncVenomTentaclesTraversalPacket.class, SyncVenomTentaclesTraversalPacket::encode, SyncVenomTentaclesTraversalPacket::new, SyncVenomTentaclesTraversalPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, SyncVenomTentaclesTraversalVelocityPacket.class, SyncVenomTentaclesTraversalVelocityPacket::encode, SyncVenomTentaclesTraversalVelocityPacket::new, SyncVenomTentaclesTraversalVelocityPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, SyncVenomGrabTentaclePacket.class, SyncVenomGrabTentaclePacket::encode, SyncVenomGrabTentaclePacket::new, SyncVenomGrabTentaclePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, SyncVenomCombatTargetsPacket.class, SyncVenomCombatTargetsPacket::encode, SyncVenomCombatTargetsPacket::new, SyncVenomCombatTargetsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, SyncVenomFlightStatePacket.class, SyncVenomFlightStatePacket::encode, SyncVenomFlightStatePacket::new, SyncVenomFlightStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, SyncVenomClimbInputPacket.class, SyncVenomClimbInputPacket::encode, SyncVenomClimbInputPacket::new, SyncVenomClimbInputPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, SyncVenomAttackClickPacket.class, SyncVenomAttackClickPacket::encode, SyncVenomAttackClickPacket::new, SyncVenomAttackClickPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(id++, SyncSymbioteFormPacket.class, SyncSymbioteFormPacket::encode, SyncSymbioteFormPacket::new, SyncSymbioteFormPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        INSTANCE.registerMessage(id++, SyncSymbioteMiningPacket.class, SyncSymbioteMiningPacket::encode, SyncSymbioteMiningPacket::new, SyncSymbioteMiningPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(id++, SyncVenomSizePacket.class, SyncVenomSizePacket::encode, SyncVenomSizePacket::new, SyncVenomSizePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void syncVenomAttackClick() {
        INSTANCE.sendToServer(new SyncVenomAttackClickPacket());
    }

    /** un clic sinistro: il pacchetto non porta dati, conta solo il fatto che sia avvenuto */
    public static class SyncVenomAttackClickPacket {
        public SyncVenomAttackClickPacket() {
        }

        public SyncVenomAttackClickPacket(FriendlyByteBuf buf) {
        }

        public void encode(FriendlyByteBuf buf) {
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    VenomAttackBarrageHandler.onAttackClick(player);
                }
            });
            ctx.get().setPacketHandled(true);
            return true;
        }
    }

    public static class SyncVenomModelPacket {
        private final String form;

        public SyncVenomModelPacket(String form) {
            this.form = form == null ? "" : form;
        }

        public SyncVenomModelPacket(FriendlyByteBuf buf) {
            this.form = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(form);
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().player != null) {
                    ClientEventHandler.setTransformedForm(form);
                    Minecraft.getInstance().player.refreshDimensions(); // Forza l'aggiornamento del modello del giocatore
                }
            }));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }
    public static void syncVenomTentaclesTraversal(ServerPlayer player, List<Vec3> anchors) {
        syncVenomTentaclesTraversal(player, anchors, true);
    }

    public static void syncVenomTentaclesTraversal(ServerPlayer player, List<Vec3> anchors, boolean active) {
        assicuraForma(player);
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomTentaclesTraversalPacket(player.getId(), anchors, active));
    }

    public static class SyncVenomTentaclesTraversalPacket {
        private final int entityId;
        private final List<Vec3> anchors;
        private final boolean active;

        public SyncVenomTentaclesTraversalPacket(int entityId, List<Vec3> anchors, boolean active) {
            this.entityId = entityId;
            this.anchors = anchors == null ? List.of() : anchors;
            this.active = active;
        }

        public SyncVenomTentaclesTraversalPacket(FriendlyByteBuf buf) {
            this.entityId = buf.readInt();
            this.active = buf.readBoolean();
            int size = buf.readVarInt();
            List<Vec3> readAnchors = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                readAnchors.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
            }
            this.anchors = readAnchors;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(entityId);
            buf.writeBoolean(active);
            buf.writeVarInt(anchors.size());
            for (Vec3 anchor : anchors) {
                buf.writeDouble(anchor.x);
                buf.writeDouble(anchor.y);
                buf.writeDouble(anchor.z);
            }
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> VenomTentaclesTraversalRenderer.updateAnchors(entityId, anchors, active)));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }
    public static void syncVenomTentaclesTraversalVelocity(Vec3 velocity) {
        INSTANCE.sendToServer(new SyncVenomTentaclesTraversalVelocityPacket(velocity));
    }

    public static class SyncVenomTentaclesTraversalVelocityPacket {
        private final Vec3 velocity;

        public SyncVenomTentaclesTraversalVelocityPacket(Vec3 velocity) {
            this.velocity = velocity == null ? Vec3.ZERO : velocity;
        }

        public SyncVenomTentaclesTraversalVelocityPacket(FriendlyByteBuf buf) {
            this.velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeDouble(velocity.x);
            buf.writeDouble(velocity.y);
            buf.writeDouble(velocity.z);
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !isVenomMovementVelocityAllowed(player)) {
                    return;
                }
                player.setDeltaMovement(velocity);
                player.fallDistance = 0.0F;
            });
            ctx.get().setPacketHandled(true);
            return true;
        }
    }


    public static void syncVenomGrabTentacle(ServerPlayer player, Vec3 target) {
        assicuraForma(player);
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomGrabTentaclePacket(player.getId(), target));
    }

    public static class SyncVenomGrabTentaclePacket {
        private final int entityId;
        private final Vec3 target;

        public SyncVenomGrabTentaclePacket(int entityId, Vec3 target) {
            this.entityId = entityId;
            this.target = target;
        }

        public SyncVenomGrabTentaclePacket(FriendlyByteBuf buf) {
            this.entityId = buf.readInt();
            this.target = buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(entityId);
            buf.writeBoolean(target != null);
            if (target != null) {
                buf.writeDouble(target.x);
                buf.writeDouble(target.y);
                buf.writeDouble(target.z);
            }
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> VenomTentaclesTraversalRenderer.updateGrabTarget(entityId, target)));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }
    public static void syncVenomCombatTargets(ServerPlayer player, List<Vec3> targets) {
        assicuraForma(player);
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomCombatTargetsPacket(player.getId(), targets));
    }

    public static void syncVenomFlightState(ServerPlayer player, boolean active) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomFlightStatePacket(active));
    }

    public static void syncVenomClimbInput(boolean forward, boolean ctrl, boolean shift) {
        INSTANCE.sendToServer(new SyncVenomClimbInputPacket(forward, ctrl, shift));
    }

    public static class SyncVenomClimbInputPacket {
        private final boolean forward;
        private final boolean ctrl;
        private final boolean shift;

        public SyncVenomClimbInputPacket(boolean forward, boolean ctrl, boolean shift) {
            this.forward = forward;
            this.ctrl = ctrl;
            this.shift = shift;
        }

        public SyncVenomClimbInputPacket(FriendlyByteBuf buf) {
            this.forward = buf.readBoolean();
            this.ctrl = buf.readBoolean();
            this.shift = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(forward);
            buf.writeBoolean(ctrl);
            buf.writeBoolean(shift);
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    VenomSymbioteSystemsHandler.setClimbInput(player, forward, ctrl, shift);
                }
            });
            ctx.get().setPacketHandled(true);
            return true;
        }
    }

    public static class SyncVenomFlightStatePacket {
        private final boolean active;

        public SyncVenomFlightStatePacket(boolean active) {
            this.active = active;
        }

        public SyncVenomFlightStatePacket(FriendlyByteBuf buf) {
            this.active = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(active);
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> VenomTentaclesTraversalClientController.setFlightActiveFromServer(active)));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }

    public static class SyncVenomCombatTargetsPacket {
        private final int entityId;
        private final List<Vec3> targets;

        public SyncVenomCombatTargetsPacket(int entityId, List<Vec3> targets) {
            this.entityId = entityId;
            this.targets = targets == null ? List.of() : targets;
        }

        public SyncVenomCombatTargetsPacket(FriendlyByteBuf buf) {
            this.entityId = buf.readInt();
            int size = buf.readVarInt();
            List<Vec3> readTargets = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                readTargets.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
            }
            this.targets = readTargets;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(entityId);
            buf.writeVarInt(targets.size());
            for (Vec3 target : targets) {
                buf.writeDouble(target.x);
                buf.writeDouble(target.y);
                buf.writeDouble(target.z);
            }
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> VenomTentaclesTraversalRenderer.updateCombatTargets(entityId, targets)));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }
    private static boolean isVenomMovementVelocityAllowed(ServerPlayer player) {
        return isScoreActive(player, "Venom.TentaclesTraversal") || isScoreActive(player, "Venom.Flight");
    }

    private static boolean isScoreActive(ServerPlayer player, String objectiveName) {
        var objective = player.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return false;
        }
        return player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore() > 0;
    }

    /**
     * Dice ai client quale forma simbionte ha un giocatore, per disegnargli i tentacoli del
     * colore giusto. Va in broadcast perche' i tentacoli si vedono anche addosso agli altri:
     * la scoreboard non serviva, gli obiettivi dummy non arrivano ai client.
     */
    public static void syncSymbioteForm(ServerPlayer player, String form) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncSymbioteFormPacket(player.getId(), form));
    }

    public static class SyncSymbioteFormPacket {
        private final int entityId;
        private final String form;

        public SyncSymbioteFormPacket(int entityId, String form) {
            this.entityId = entityId;
            this.form = form == null ? "" : form;
        }

        public SyncSymbioteFormPacket(FriendlyByteBuf buf) {
            this.entityId = buf.readVarInt();
            this.form = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeUtf(form);
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> VenomTentaclesTraversalRenderer.updateForm(entityId, form)));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }

    /** Dice al client se il corpo simbionte e' fuori e quale attrezzo ha in pugno. */

    public static void syncSymbioteMining(ServerPlayer player, boolean corpoAttivo, int attrezzo) {

        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),

                new SyncSymbioteMiningPacket(corpoAttivo, attrezzo));

    }



    /**

     * Lo stato che serve al client per scavare allo stesso ritmo del server.

     *

     * <p>Va solo al diretto interessato: nessun altro ha bisogno di sapere con che velocita'

     * sta scavando.</p>

     */

    public static class SyncSymbioteMiningPacket {

        private final boolean corpoAttivo;

        private final int attrezzo;



        public SyncSymbioteMiningPacket(boolean corpoAttivo, int attrezzo) {

            this.corpoAttivo = corpoAttivo;

            this.attrezzo = attrezzo;

        }



        public SyncSymbioteMiningPacket(FriendlyByteBuf buf) {

            this.corpoAttivo = buf.readBoolean();

            this.attrezzo = buf.readVarInt();

        }



        public void encode(FriendlyByteBuf buf) {

            buf.writeBoolean(corpoAttivo);

            buf.writeVarInt(attrezzo);

        }



        public boolean handle(Supplier<NetworkEvent.Context> ctx) {

            ctx.get().enqueueWork(() ->

                    modKlyntar.client.ClientSymbioteMiningState.aggiorna(corpoAttivo, attrezzo));

            ctx.get().setPacketHandled(true);

            return true;

        }

    }



    /**
     * Dice a tutti quelli che vedono questo giocatore quanto e' alto il suo simbionte.
     *
     * <p>Serve perche' la taglia dipende da Klyntar.VenomSize, che e' un obiettivo fittizio e
     * come tale non raggiunge mai i client: senza pacchetto il client terrebbe l'altezza
     * vanilla e la telecamera in prima persona resterebbe all'altezza di un umano.</p>
     */
    public static void syncVenomSize(ServerPlayer player, int stato) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncVenomSizePacket(player.getId(), stato));
    }

    /** La stessa taglia, ma a un solo destinatario: serve a chi inizia a vedere il giocatore. */
    public static void syncVenomSizeA(ServerPlayer destinatario, int idEntita, int stato) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> destinatario),
                new SyncVenomSizePacket(idEntita, stato));
    }

    public static class SyncVenomSizePacket {
        private final int entityId;
        private final int stato;

        public SyncVenomSizePacket(int entityId, int stato) {
            this.entityId = entityId;
            this.stato = stato;
        }

        public SyncVenomSizePacket(FriendlyByteBuf buf) {
            this.entityId = buf.readVarInt();
            this.stato = buf.readVarInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeVarInt(stato);
        }

        public boolean handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    modKlyntar.client.ClientVenomSize.applica(entityId, stato));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }

    /** ultima forma comunicata a ciascun giocatore, per non ripetere il pacchetto ogni tick */
    private static final java.util.Map<Integer, String> ULTIMA_FORMA = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Assicura che i client sappiano la forma di questo giocatore prima di disegnargli i
     * tentacoli. Mandarla solo al momento della trasformazione non basta: chi entra dopo, o chi
     * era gia' trasformato al login, non la riceverebbe mai.
     */
    public static void assicuraForma(ServerPlayer player) {
        String forma = player.getCapability(modKlyntar.capability.PlayerPowerCapability.PLAYER_POWER)
                .map(modKlyntar.capability.PlayerPowerCapability.PlayerPower::getForm).orElse("");
        if (!forma.equals(ULTIMA_FORMA.get(player.getId()))) {
            ULTIMA_FORMA.put(player.getId(), forma);
            syncSymbioteForm(player, forma);
        }
    }
}
