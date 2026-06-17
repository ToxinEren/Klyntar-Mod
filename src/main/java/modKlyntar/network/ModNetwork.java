package modKlyntar.network;

import modKlyntar.MyMod;
import modKlyntar.client.ClientEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;

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
            return true;
        }
    }
}
