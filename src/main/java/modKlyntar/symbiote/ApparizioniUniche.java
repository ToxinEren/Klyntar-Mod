package modKlyntar.symbiote;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Quello che in un mondo compare una volta sola.
 *
 * <p>Dal documento di design: All-Black si trova in un cratere e spawna una volta sola. Il dato
 * sta nel mondo (nell'Overworld), non nel giocatore: chi arriva dopo non trova un secondo
 * cratere.</p>
 *
 * <p><b>Segnalibro.</b> Il cratere di All-Black non c'e' ancora: quando ci sara', prima di
 * generarlo chiedera' {@code ApparizioniUniche.di(livello).prenota("allblack")}.</p>
 */
public final class ApparizioniUniche extends SavedData {
    private static final String NOME = "klyntars_apparizioni";
    private static final String CHIAVE = "Apparsi";
    private final Set<String> apparsi = new HashSet<>();

    private ApparizioniUniche() {
    }

    public static ApparizioniUniche di(ServerLevel livello) {
        return livello.getServer().overworld().getDataStorage()
                .computeIfAbsent(ApparizioniUniche::carica, ApparizioniUniche::new, NOME);
    }

    private static ApparizioniUniche carica(CompoundTag tag) {
        ApparizioniUniche dati = new ApparizioniUniche();
        ListTag lista = tag.getList(CHIAVE, Tag.TAG_STRING);
        for (int i = 0; i < lista.size(); i++) {
            dati.apparsi.add(lista.getString(i));
        }
        return dati;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag lista = new ListTag();
        for (String chi : this.apparsi) {
            lista.add(StringTag.valueOf(chi));
        }
        tag.put(CHIAVE, lista);
        return tag;
    }

    public boolean giaApparso(String chi) {
        return this.apparsi.contains(chi);
    }

    /** Prenota l'unica apparizione: true solo la prima volta, poi sempre false. */
    public boolean prenota(String chi) {
        if (!this.apparsi.add(chi)) {
            return false;
        }
        setDirty();
        return true;
    }
}
