package modKlyntar.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * La memoria del cratere di All-Black: dove sta, e se e' gia' stato posato.
 *
 * <p>La struttura deve comparire <em>una volta sola</em> in tutto il mondo, e una feature di
 * worldgen non basta: quelle girano per ogni chunk, e per farne nascere una sola servirebbe
 * comunque un promemoria come questo. Tanto vale posarla direttamente e segnarla qui.</p>
 */
public class DatiCratere extends SavedData {

    private static final String NOME = "klyntar_cratere_allblack";

    private boolean posato;
    @Nullable
    private BlockPos dove;

    public static DatiCratere di(ServerLevel livello) {
        return livello.getDataStorage().computeIfAbsent(DatiCratere::leggi, DatiCratere::new, NOME);
    }

    public static DatiCratere leggi(CompoundTag tag) {
        DatiCratere dati = new DatiCratere();
        dati.posato = tag.getBoolean("Posato");
        if (tag.contains("X")) {
            dati.dove = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        }
        return dati;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Posato", this.posato);
        if (this.dove != null) {
            tag.putInt("X", this.dove.getX());
            tag.putInt("Y", this.dove.getY());
            tag.putInt("Z", this.dove.getZ());
        }
        return tag;
    }

    public boolean posato() {
        return this.posato;
    }

    @Nullable
    public BlockPos dove() {
        return this.dove;
    }

    /** Cancella il promemoria: il mondo tornera' a posare la struttura da capo. */
    public void dimentica() {
        this.posato = false;
        this.dove = null;
        setDirty();
    }

    public void segna(BlockPos posizione) {
        this.posato = true;
        this.dove = posizione;
        setDirty();
    }
}
