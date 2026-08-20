package modKlyntar.client;

/**
 * Quel poco che il client deve sapere per scavare alla velocita' giusta.
 *
 * <p>La velocita' di scavo la calcolano tutte e due le parti, e devono trovare lo stesso
 * numero: se solo il server sapesse del simbionte, il client continuerebbe a scavare piano e
 * il blocco cadrebbe comunque al suo ritmo. Gli obiettivi della scoreboard non bastano perche'
 * quelli fittizi non arrivano ai client, quindi lo stato viaggia in un pacchetto.</p>
 */
public final class ClientSymbioteMiningState {
    private static boolean corpoAttivo;
    private static int attrezzo;

    private ClientSymbioteMiningState() {
    }

    public static void aggiorna(boolean corpoAttivo, int attrezzo) {
        ClientSymbioteMiningState.corpoAttivo = corpoAttivo;
        ClientSymbioteMiningState.attrezzo = attrezzo;
    }

    public static boolean corpoAttivo() {
        return corpoAttivo;
    }

    /** 0 nessuno, 1 piccone, 2 ascia: gli stessi valori che scrive la ruota degli attrezzi */
    public static int attrezzo() {
        return attrezzo;
    }
}
