# Klyntar — mod Minecraft Forge 1.20.1

Mod dei simbionti (Venom, Carnage, Anti-Venom, Toxin) costruita sopra Palladium.
Repo: `ToxinEren/Klyntar-Mod`. Branch di lavoro: `Paddium-upgrade`.

## Ambiente

Java 17, Gradle 8.8. Il JDK e la cache Gradle sono locali al disco, non quelli di sistema:

```bash
JAVA_HOME="D:/MinecraftModding/gradle-cache/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.19+10" GRADLE_USER_HOME="D:/MinecraftModding/gradle-cache" ./gradlew runClient --offline
```

`--offline` è obbligatorio: le dipendenze mod (Palladium, GeckoLib, KubeJS, Curios, Architectury,
Pehkui, f_tech) sono jar locali in `../mods_dependencies` e non si risolvono dalla rete.

## Struttura

| Percorso | Contenuto |
|---|---|
| `src/main/java/modKlyntar/` | 93 file Java: capability, handler, entità, worldgen |
| `src/main/resources/data/klyntars/palladium/powers/` | i poteri: `venom`, `carnage`, `antivenom`, `toxin`, `wip` |
| `src/main/resources/assets/klyntars/kubejs_scripts/` | 165 script KubeJS (moveset, animazioni) |
| `src/main/resources/assets/klyntars/animations/` | 21 file di animazione GeckoLib |
| `src/main/resources/assets/klyntars/textures/icon/` | icone delle abilità, con sottocartelle per forma |

`PlayerPowerCapability` è il cuore del sistema: tiene la forma corrente, insegue il superpotere
Palladium tick per tick (`seguiPalladium`) e applica gli effetti legati al corpo del simbionte.

## Trappole ricorrenti

Queste sono le cause reali di bug già capitati più volte. Vanno controllate prima di dare per
buona una modifica.

**Gli id di potere sono cablati.** I render layer, gli script KubeJS e i condition serializer
contengono l'id del potere in chiaro. Quando un potere viene derivato da venom restano riferimenti
a `klyntars:venom` che poi falliscono in silenzio o al caricamento. In una copia va sempre
rimappato **prima** il nome dei layer e **poi** fatta la sostituzione generale dell'id: l'ordine
inverso rompe `venom_idle_time`, `venomcopy` e i layer che iniziano per `tox`.

**Un objective mai scritto non esiste.** `getScore` non lo crea. Una condizione Palladium che
confronta un objective inesistente con `0..0` fallisce sempre — non è vera per default. Usare
`palladium:not` attorno a un `>= 1` invece di confrontare con zero.

**Gli objective dummy non arrivano al client.** Per qualsiasi valore che serve lato client
(es. la velocità di scavo) serve un packet dedicato; lo scoreboard da solo non basta.

**Per spegnere un toggle Palladium da Java** serve un objective "lucchetto" corto messo su una
condizione `unlocking` o `enabling` dell'abilità. Non c'è un modo diretto.

**GeckoLib sostituisce la scala, non la moltiplica.** E i valori dei keyframe devono essere
vettori (`[1,1,1]`): un numero nudo fa crashare il caricamento con `Not a JSON Object: 1`.
Un controller GeckoLib riproduce una sola animazione: se un controller ha 64 trigger, qualsiasi
trigger interrompe quello in corso.

**L'id del potere negli script KubeJS compare con due punteggiature diverse.** In
`registerForPower` e `getAnimationTimerAbilityValue` sta fra apici singoli, in
`abilityUtil.isEnabled` sta fra virgolette doppie. Una sostituzione che ne cerca una sola lascia
l'altra puntata al potere di partenza, e il ramo che ne dipende semplicemente non parte: è così
che la rotazione orizzontale sul soffitto è rimasta muta in una forma derivata.

**`runClient` serve le risorse da `build/resources/main`, non da `src`.** Una modifica a uno
script KubeJS o a una texture non si vede con un reload a caldo se il client era gia' avviato:
`/kubejs reload client_scripts` ricarica diligentemente la copia stantia in `build`. Per vedere
la modifica bisogna rilanciare, cosi' Gradle riesegue `processResources`. Nel dubbio, confrontare
il file in `build` con quello in `src` prima di concludere che la correzione non funziona.

**I fine riga dei JSON sono misti** (`\n`, `\r\n`, `\r\r\n`). Le modifiche vanno fatte sui byte
con sostituzione di sottostringa, mai riscrivendo il file riga per riga: altrimenti il diff
esplode in migliaia di righe di solo cambio di terminatore.

**Le icone sono PNG a palette** (`colortype=3`). Vanno lette decodificando `PLTE`/`tRNS`: un
decoder che ignora la palette restituisce gli indici come valori di grigio e fa sembrare
l'icona tutta nera. Per le varianti colorate si tinge **solo** il nero del disegno e si lasciano
intatti gli altri colori.

**`bar_color` accetta nomi di colorante** (`orange` sì, `gold` no) e non tinge le icone.

## Convenzioni

Il codice Java di questo progetto è scritto con nomi e commenti in italiano
(`tickEffettiDelCorpo`, `formaSuPalladium`, `riapplicaForma`). Mantieni quello stile.

Le descrizioni delle abilità nei JSON dei poteri sono in inglese e non devono citare dettagli
implementativi.

## Difetti noti ancora aperti

- Log diagnostici da rimuovere in 18 file Java (anti-venom, mining, allineamento forma, campana, affinità, frammenti).
- `symbiontmaceattack1..4` è vincolata a `CombatTool == 30` e non parte mai.
- Toxin non ha una `toxin_symbol.png` dedicata: usa quella di venom.
- 4 icone sono ancora condivise fra le forme e avrebbero del nero da tingere: `punch`, `knullwings`, `heart` e il set dei symbol.
