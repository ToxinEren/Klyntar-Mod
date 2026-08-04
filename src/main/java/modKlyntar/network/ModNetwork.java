package modKlyntar.network;

import modKlyntar.MyMod;
import modKlyntar.client.ClientEventHandler;
import modKlyntar.client.VenomLocomotionClientController;
import modKlyntar.client.renderer.VenomLocomotionRenderer;
import modKlyntar.player.VenomAttackBarrageHandler;
import modKlyntar.player.VenomSymbioteSystemsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
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
        INSTANCE.registerMessage(id++, SyncVenomModelPacket.class, SyncVenomModelPacket::encode, SyncVenomModelPacket::new, SyncVenomModelPacket::handle);
        INSTANCE.registerMessage(id++, SyncVenomLocomotionPacket.class, SyncVenomLocomotionPacket::encode, SyncVenomLocomotionPacket::new, SyncVenomLocomotionPacket::handle);
        INSTANCE.registerMessage(id++, SyncVenomLocomotionVelocityPacket.class, SyncVenomLocomotionVelocityPacket::encode, SyncVenomLocomotionVelocityPacket::new, SyncVenomLocomotionVelocityPacket::handle);
        INSTANCE.registerMessage(id++, SyncVenomGrabTentaclePacket.class, SyncVenomGrabTentaclePacket::encode, SyncVenomGrabTentaclePacket::new, SyncVenomGrabTentaclePacket::handle);
        INSTANCE.registerMessage(id++, SyncVenomCombatTargetsPacket.class, SyncVenomCombatTargetsPacket::encode, SyncVenomCombatTargetsPacket::new, SyncVenomCombatTargetsPacket::handle);
        INSTANCE.registerMessage(id++, SyncVenomFlightStatePacket.class, SyncVenomFlightStatePacket::encode, SyncVenomFlightStatePacket::new, SyncVenomFlightStatePacket::handle);
        INSTANCE.registerMessage(id++, SyncVenomClimbInputPacket.class, SyncVenomClimbInputPacket::encode, SyncVenomClimbInputPacket::new, SyncVenomClimbInputPacket::handle);
        INSTANCE.registerMessage(id++, SyncVenomAttackClickPacket.class, SyncVenomAttackClickPacket::encode, SyncVenomAttackClickPacket::new, SyncVenomAttackClickPacket::handle);
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
            ctx.get().enqueueWork(() -> {
                if (Minecraft.getInstance().player != null) {
                    ClientEventHandler.setTransformedForm(form);
                    Minecraft.getInstance().player.refreshDimensions(); // Forza l'aggiornamento del modello del giocatore
                    System.out.println("Model change packet handled. form: " + form);
                }
            });
            ctx.get().setPacketHandled(true);
            return true;
        }
    }
    public static void syncVenomLocomotion(ServerPlayer player, List<Vec3> anchors) {
        syncVenomLocomotion(player, anchors, true);
    }

    public static void syncVenomLocomotion(ServerPlayer player, List<Vec3> anchors, boolean active) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncVenomLocomotionPacket(player.getId(), anchors, active));
    }

    public static class SyncVenomLocomotionPacket {
        private final int entityId;
        private final List<Vec3> anchors;
        private final boolean active;

        public SyncVenomLocomotionPacket(int entityId, List<Vec3> anchors, boolean active) {
            this.entityId = entityId;
            this.anchors = anchors == null ? List.of() : anchors;
            this.active = active;
        }

        public SyncVenomLocomotionPacket(FriendlyByteBuf buf) {
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
            ctx.get().enqueueWork(() -> VenomLocomotionRenderer.updateAnchors(entityId, anchors, active));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }
    public static void syncVenomLocomotionVelocity(Vec3 velocity) {
        INSTANCE.sendToServer(new SyncVenomLocomotionVelocityPacket(velocity));
    }

    public static class SyncVenomLocomotionVelocityPacket {
        private final Vec3 velocity;

        public SyncVenomLocomotionVelocityPacket(Vec3 velocity) {
            this.velocity = velocity == null ? Vec3.ZERO : velocity;
        }

        public SyncVenomLocomotionVelocityPacket(FriendlyByteBuf buf) {
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
            ctx.get().enqueueWork(() -> VenomLocomotionRenderer.updateGrabTarget(entityId, target));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }
    public static void syncVenomCombatTargets(ServerPlayer player, List<Vec3> targets) {
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
            ctx.get().enqueueWork(() -> VenomLocomotionClientController.setFlightActiveFromServer(active));
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
            ctx.get().enqueueWork(() -> VenomLocomotionRenderer.updateCombatTargets(entityId, targets));
            ctx.get().setPacketHandled(true);
            return true;
        }
    }
    private static boolean isVenomMovementVelocityAllowed(ServerPlayer player) {
        return isScoreActive(player, "Venom.Locomotion") || isScoreActive(player, "Venom.Flight");
    }

    private static boolean isScoreActive(ServerPlayer player, String objectiveName) {
        var objective = player.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return false;
        }
        return player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore() > 0;
    }
}
