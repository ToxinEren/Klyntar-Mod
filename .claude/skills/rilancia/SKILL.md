---
name: rilancia
description: Rilancia il client Minecraft della mod Klyntar (Forge 1.20.1) con JDK e cache Gradle locali, dopo aver controllato che non ce ne sia gia' uno aperto. Usala quando serve provare in gioco una modifica a codice, modelli, texture o script KubeJS.
---

# Rilanciare il client Klyntar

## Prima di tutto: non lanciarne due

Avviare un secondo client sopra uno gia' in esecuzione fa crashare la JVM con `0xC0000005`.
Il controllo non e' facoltativo, e deve **bloccare davvero** — non basta stampare il conteggio:

```bash
N=$(tasklist 2>/dev/null | grep -cE "javaw\.exe|java\.exe")
if [ "$N" -ne 0 ]; then echo "NON lancio: ci sono gia $N processi java"; exit 1; fi
```

Se ne trovi uno aperto, fermati e chiedi all'utente di chiuderlo. Non ucciderlo tu: potrebbe
avere un mondo aperto con lavoro non salvato.

## Il lancio

JDK e cache Gradle sono locali al disco, non quelli di sistema. `--offline` e' obbligatorio:
le dipendenze mod (Palladium, GeckoLib, KubeJS, Curios, Architectury, Pehkui, f_tech) sono jar
in `../mods_dependencies` e non si risolvono dalla rete.

```bash
cd "D:/MinecraftModding/Klyntar-1.20.1-palladium"
rm -f run/logs/latest.log
JAVA_HOME="D:/MinecraftModding/gradle-cache/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.19+10" \
GRADLE_USER_HOME="D:/MinecraftModding/gradle-cache" \
nohup ./gradlew runClient --offline > "$SCRATCH/gradle.out" 2>&1 &
```

Cancellare `latest.log` prima serve a non leggere gli errori della sessione precedente
scambiandoli per nuovi.

Va lanciato in background (`run_in_background`): il client resta aperto finche' l'utente non
lo chiude, quindi in primo piano bloccherebbe la sessione.

## Controllare che sia partito

Il log e' `run/logs/latest.log`. Non fidarti dell'output di Gradle: il client ci scrive dentro
molto di piu'.

```bash
L="D:/MinecraftModding/Klyntar-1.20.1-palladium/run/logs/latest.log"
grep -aiE "Exception|Caused by|Using missing texture|Unable to load|Not a JSON" "$L" | head -20
```

`grep -a` e' necessario: il log contiene byte che fanno decidere a grep che e' binario, e senza
`-a` non stampa niente e sembra che vada tutto bene.

Rumore normale, da ignorare: i mixin di EMI, REI e ModNameTooltip che non trovano le classi
(sono mod opzionali assenti) e l'auth di Realms che fallisce.

Se usi il tool Monitor per seguire il log, **ogni** stadio della pipe deve svuotare il buffer
riga per riga: un `grep` senza `--line-buffered` in fondo alla catena trattiene tutto e il
monitor resta muto fino allo scadere del timeout.

## Verificare che il client serva davvero le modifiche

`runClient` serve le risorse da `build/resources/main`, non da `src`. Una texture o uno script
KubeJS cambiato non si vede se il client era gia' avviato, e `/kubejs reload client_scripts`
ricarica diligentemente la copia stantia in `build`. Prima di concludere che una correzione
non funziona, confronta:

```bash
cd "D:/MinecraftModding/Klyntar-1.20.1-palladium"
for f in assets/klyntars/geo/allblack.geo.json assets/klyntars/textures/models/allblack.png; do
  cmp -s "src/main/resources/$f" "build/resources/main/$f" && echo "OK   $f" || echo "DIVERSO $f"
done
```

Se risulta `DIVERSO`, il rilancio non ha rifatto `processResources`: rilancia di nuovo, oppure
esegui `./gradlew processResources --offline` con le stesse variabili d'ambiente.

## Compilare senza lanciare

Per il solo controllo che il Java compili, senza aprire il gioco:

```bash
JAVA_HOME="D:/MinecraftModding/gradle-cache/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.19+10" \
GRADLE_USER_HOME="D:/MinecraftModding/gradle-cache" \
./gradlew compileJava processResources --offline 2>&1 | grep -E "error:|BUILD|\.java:"
```
