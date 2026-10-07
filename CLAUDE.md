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
| `src/main/resources/data/klyntars/palladium/powers/` | i poteri: `venom`, `venomspidey`, `riot` |
| `src/main/resources/assets/klyntars/kubejs_scripts/` | 165 script KubeJS (moveset, animazioni) |
| `src/main/resources/assets/klyntars/animations/` | 21 file di animazione GeckoLib |
| `src/main/resources/assets/klyntars/textures/icon/` | icone delle abilità, con sottocartelle per forma |

`PlayerPowerCapability` è il cuore del sistema: tiene la forma corrente, insegue il superpotere
Palladium tick per tick (`seguiPalladium`) e applica gli effetti legati al corpo del simbionte.

## La progressione dei simbionti

Il piano e' nel documento di design (`../Klyntars – Documento di design progressione simbionti e All-Black.pdf`):
da Venom ad All-Black attraverso simbionti che si cacciano, si nutrono tra loro e si contendono lo
stesso corpo. I sistemi stanno in `modKlyntar/symbiote/`:

| Classe | Cosa fa |
|---|---|
| `RegistroSimbionti` | chi sono i simbionti: tier, temperamento, forza base, origine, padre, tratti nativi. Quelli non ancora nella mod sono **segnalibri** con `disponibile = false` |
| `ProfiliSimbionti` | per giocatore e simbionte: inglobati (per la nascita), digeriti, tratti acquisiti; la forza; gli objective dei tratti (`Klyntar.BodyWeapons`, `Klyntar.Trait.*`) che i JSON leggono |
| `ConflittoSimbionti` | un secondo simbionte nello stesso corpo, a tre tempi sul modello del "graft" della mod Symbiote: l'ingresso (quello di dentro si oppone, i filamenti dei due si frustano), la lotta (tensione 0-100, avvisi a 60 e 85, mangiare aiuta quello di dentro, non uccide mai l'ospite), la resa (chi perde implora; fuga o assorbimento) |
| `SensoSimbionti` | il simbionte sente gli altri entro 18 blocchi, anche dietro i muri, e lo dice; avvisa quando quello che lo bracca gli viene addosso |
| `DuelloSimbionti` | il duello coi tentacoli prima dell'ingresso: il simbionte dell'ospite risponde da solo alle frustate del rivale (piu' forte col bond alto); le frustate si disegnano in `client/renderer/FrustateSimbionte` |
| `NascitaSimbionti` | Carnage da Venom, Toxin da Carnage: 2 inglobati + vita sotto il 30% |
| `CorruzioneAllBlack` | la barra 0-100 di All-Black che sale uccidendo, gli stadi, il calo, i prezzi |
| `UnioneDoppia` | All-Black con un altro simbionte: dominanza contro resistenza, eccezione Toxin |
| `ApparizioniUniche` | quello che compare una volta per mondo (il cratere di All-Black) |

I mob dei simbionti che attaccano (Riot) non corrono dritti su chi porta gia' un simbionte: lo
braccano (`BraccaOspiteGoal` in `SymbioteEntity`, dal WildHostBrain della mod Symbiote) - lo
osservano, gli girano intorno a nove blocchi e, trovata un'apertura, lo sfidano a frustate da
quattro-sei blocchi; quando la preda e' provata la agganciano da lontano (lo strattone) e se la
tirano addosso: il contatto e' l'ingresso, senza danno. I colpi di un rivale (un altro simbionte
libero, o chi ne porta uno di un'altra famiglia) feriscono il mob invece di farlo entrare, ma non
lo uccidono (si fermano a mezzo cuore; lo uccidono solo fuoco e suono); sotto il 40% di vita il
mob scappa. Le frustate non uccidono mai l'ospite. Chi entra malconcio entra piu' debole nel
conflitto.

**Riot non lascia mai scappare nessuno** (`Temperamento.nonLasciaScappare`, chiesto da Luigi):
quando vince il conflitto assorbe sempre, chi gli cede il corpo viene inghiottito, chi e' nel suo
mirino non fugge, e lui non si ritira mai - allo stremo ritenta lo strattone per entrare. Attacca
a vista i simbionti liberi (`CacciaSimbionteGoal`), corre piu' di chi scappa, frusta piu' forte
(2 cuori invece di 1,5) e divora quelli ridotti sotto un quarto. Fuso con un giocatore, afferra coi
tentacoli i simbionti liberi di un'altra famiglia entro 8 blocchi e li trascina all'ospite: al
contatto entrano e parte il conflitto, con Riot dentro (`DuelloSimbionti.onPlayerTick`). Chi e'
tenuto (`SymbioteEntity.trattieni`) non scappa e non si rifugia negli animali.
La mod di riferimento e' `../Venom Bedrock/symbiote-1.1.2.jar` (l'autore ha dato il permesso di
usarne il codice); si decompila con ForgeFlower dalla cache di Gradle.

I profili stanno in `PlayerPersisted` dei dati del giocatore, che Forge copia alla morte.
Per rendere disponibile un simbionte segnalibro: il potere con la skill `nuovo-simbionte`, poi
`disponibile = true` nel registro. I segnalibri si trovano cercando `SEGNALIBRO` nel codice.

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

**Le texture generate dai layer si tengono per nome d'uscita.** Un layer con `alpha_mask`
registra la texture che produce sotto il suo `"output"`. Due layer con lo stesso output e
immagini diverse si pestano i piedi: la prima forma che la genera la impone all'altra (la maschera
di trasformazione di Riot compariva su Venom). Ogni forma con texture sue deve avere output suoi
(`..._riot_#MASK.png`); `deriva_potere.py` lo fa da solo. Si controlla con lo script
`uscite_condivise.py` o cercando gli `"output"` doppi.

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
