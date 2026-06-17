package modKlyntar.util;

import modKlyntar.MyMod;
import net.minecraft.server.level.ServerPlayer;

public final class KlyntarScaleUtil {
    private KlyntarScaleUtil() {
    }

    public static void setPlayerScale(ServerPlayer player, float scale) {
        player.getEntityData().set(MyMod.PLAYER_SCALE, scale);
        player.refreshDimensions();
    }
}
