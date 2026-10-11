// Le pose del corpo del giocatore durante le abilita' del simbionte (10/10/2026). Una tabella sola
// per tutte le forme; il modello del simbionte segue le parti del giocatore, quindi queste pose
// muovono anche lui.
//
// Ogni posa: il timer (un'abilita' animation_timer del potere) o lo stato (un'abilita' accesa),
// le forme che ce l'hanno, e per ogni parte del corpo gli angoli in gradi (rx ry rz), la scala
// (sx sy sz), gli spostamenti (my mz, e sx/sz "a peso" con px pz), in terza o in prima persona.
// "gambe": true sulle parti che seguono anche il timer barragelegs (le gambe della raffica).

const TUTTE = ['venom', 'riot', 'venomspidey'];
const SOLO_SPIDEY = ['venomspidey'];

const POSE = [
    // ---------------------------------------------------------------- arrampicata (Spidey)
    { nome: 'appeso', stato: 'wallcrawl_hanging', forme: SOLO_SPIDEY, priorita: 5, terza: {
        right_arm: { rx: -118, rz: 8 }, left_arm: { rx: -118, rz: -8 },
        right_leg: { rx: -16, rz: 4 }, left_leg: { rx: -16, rz: -4 } } },
    { nome: 'soffitto', stato: 'wallcrawl_ceiling', forme: SOLO_SPIDEY, priorita: 5, terza: {
        head: { rx: -48 }, body: { rx: -90, rz: 180, my: 22, mz: 15 },
        right_leg: { rx: -20 }, left_leg: { rx: -20 } } },
    { nome: 'soffitto_fermo', stato: 'climb_ceiling_hold', forme: SOLO_SPIDEY, priorita: 5, terza: {
        head: { rx: -48 }, body: { rx: -90, rz: 180, my: 22, mz: 15 },
        right_arm: { rx: -12 }, left_arm: { rx: -12 }, right_leg: { rx: -20 }, left_leg: { rx: -20 } } },
    { nome: 'passo_1', timer: 'wallcrawl_step1', forme: SOLO_SPIDEY, curva: 'easeInOutSine', terza: {
        right_arm: { rx: -128 }, left_arm: { rx: -96 }, right_leg: { rx: -36 }, left_leg: { rx: -4 } } },
    { nome: 'passo_2', timer: 'wallcrawl_step2', forme: SOLO_SPIDEY, curva: 'easeInOutSine', terza: {
        right_arm: { rx: -96 }, left_arm: { rx: -128 }, right_leg: { rx: -4 }, left_leg: { rx: -36 } } },

    // ---------------------------------------------------------------- prese
    { nome: 'stritola', timer: 'gripcrush', forme: TUTTE, terza: {
        left_arm: { rx: -96, ry: 28, rz: -8 }, body: { ry: 10 } } },
    { nome: 'afferra', timer: 'gripreach', forme: TUTTE, terza: {
        right_arm: { rx: -96, ry: -28, rz: 8 }, body: { ry: -10 } } },

    // ---------------------------------------------------------------- colpi pesanti
    { nome: 'schianto', timer: 'groundpound', forme: TUTTE, terza: {
        right_arm: { rx: -162, rz: 14, sx: 1.4, sy: 1.4, sz: 1.4 }, left_arm: { rx: 22, rz: -26 },
        right_leg: { rx: 12 }, left_leg: { rx: -26 }, body: { rx: 6, ry: -26 } },
      prima: { right_arm: { rz: 36, sx: 1.25, sy: 1.25, sz: 1.25 } } },
    { nome: 'schianto_2', timer: 'groundpound2', forme: TUTTE, terza: {
        right_arm: { rx: -62, sx: 1.4, sy: 1.4, sz: 1.4 }, left_arm: { rx: 26, rz: -26 },
        right_leg: { rx: 18 }, left_leg: { rx: -16 }, body: { rx: -26, ry: 26 } },
      prima: { right_arm: { rz: -62, sx: 1.25, sy: 1.25, sz: 1.25 } } },
    { nome: 'martello', timer: 'overheadsmash', forme: TUTTE, terza: {
        right_arm: { rx: -164, sx: 1.4, sy: 1.4, sz: 1.4 }, left_arm: { rx: -164, sx: 1.4, sy: 1.4, sz: 1.4 },
        right_leg: { rx: 12 }, left_leg: { rx: -26 }, body: { rx: 24 } },
      prima: { right_arm: { rz: 36, sx: 1.25, sy: 1.25, sz: 1.25 }, left_arm: { rz: -36, sx: 1.25, sy: 1.25, sz: 1.25 } } },
    { nome: 'martello_2', timer: 'overheadsmash2', forme: TUTTE, terza: {
        right_arm: { rx: -62, sx: 1.4, sy: 1.4, sz: 1.4 }, left_arm: { rx: -62, sx: 1.4, sy: 1.4, sz: 1.4 },
        right_leg: { rx: 18 }, left_leg: { rx: -16 }, body: { rx: -26 } },
      prima: { right_arm: { rz: -62, sx: 1.25, sy: 1.25, sz: 1.25 }, left_arm: { rz: 62, sx: 1.25, sy: 1.25, sz: 1.25 } } },

    // ---------------------------------------------------------------- pugni
    { nome: 'pugno', timer: 'tentaclepunchwindup', forme: TUTTE, terza: {
        right_arm: { rx: -94, pz: 5 }, left_arm: { rx: 24, rz: -26 },
        right_leg: { rx: 12 }, left_leg: { rx: -26 }, body: { ry: -26 } },
      prima: { right_arm: { pz: 8 } } },
    { nome: 'pugno_2', timer: 'tentaclepunchthrust', forme: TUTTE, terza: {
        right_arm: { rx: -94, pz: -4 }, left_arm: { rx: 24, rz: -26 },
        right_leg: { rx: 12 }, left_leg: { rx: -18 }, body: { ry: 8 } },
      prima: { right_arm: { pz: -2, rx: -12, ry: -18, rz: -26 } } },

    // ---------------------------------------------------------------- artigli (la raffica)
    { nome: 'artiglio_sx', timer: 'leftclaw', forme: TUTTE, terza: {
        left_arm: { rx: -126, rz: 56 }, right_arm: { rx: -32, rz: 26 },
        right_leg: { rx: -26, gambe: true }, left_leg: { rx: 12, gambe: true } },
      prima: { left_arm: { rz: -46 } } },
    { nome: 'artiglio_sx_2', timer: 'leftclaw2', forme: TUTTE, terza: {
        left_arm: { rx: -126, rz: 56 }, right_arm: { rx: -32, rz: 26 }, body: { ry: -36 },
        right_leg: { rx: -26, gambe: true }, left_leg: { rx: 12, gambe: true } },
      prima: { left_arm: { rz: 46 } } },
    { nome: 'artiglio_dx', timer: 'rightclaw', forme: TUTTE, terza: {
        right_arm: { rx: -126, rz: -56 }, left_arm: { rx: -32, rz: -26 }, body: { ry: -26 },
        right_leg: { rx: 12, gambe: true }, left_leg: { rx: -26, gambe: true } },
      prima: { right_arm: { rz: 46 } } },
    { nome: 'artiglio_dx_2', timer: 'rightclaw2', forme: TUTTE, terza: {
        right_arm: { rx: -126, rz: -56 }, left_arm: { rx: -32, rz: -26 }, body: { ry: 62 },
        right_leg: { rx: 12, gambe: true }, left_leg: { rx: -18, gambe: true } },
      prima: { right_arm: { rz: -46 } } },

    // ---------------------------------------------------------------- giri e ruggito
    { nome: 'trottola', timer: 'spinstrike', forme: TUTTE, terza: { body: { ry: -360 } } },
    { nome: 'trottola_2', timer: 'spinstrike2', forme: TUTTE, terza: {
        right_arm: { rx: -102, rz: 86 }, left_arm: { rx: -102, rz: -86 } } },
    { nome: 'trottola_2_braccia', timer: 'spinstrike2', da: 0.2, forme: TUTTE, terza: {
        right_arm: { sy: 1.8 }, left_arm: { sy: 1.8 } }, prima: { left_arm: { rx: -46 } } },
    { nome: 'trottola_2_prima', timer: 'spinstrike2', da: 0.5, forme: TUTTE, prima: { right_arm: { rx: 46 } } },
    { nome: 'ruggito', timer: 'symbioteroar', forme: TUTTE, terza: {
        right_arm: { rx: -42, ry: 48 }, left_arm: { rx: -42, ry: -48 }, head: { rx: -46 }, body: { rx: -8 } } },

    // ---------------------------------------------------------------- in prima persona
    { nome: 'braccia_corte', timer: 'shortarms', forme: TUTTE, prima: {
        right_arm: { sx: 0.82, sy: 0.82, sz: 0.82 }, left_arm: { sx: 0.82, sy: 0.82, sz: 0.82 } } },
];

// mette una parte del corpo nella posa, pesata da "peso" (0 = niente, 1 = tutta)
function metti(builder, parte, p, peso, curva) {
    let b = builder.get(parte);
    if (p.rx !== undefined) b = b.setXRotDegrees(p.rx);
    if (p.ry !== undefined) b = b.setYRotDegrees(p.ry);
    if (p.rz !== undefined) b = b.setZRotDegrees(p.rz);
    if (p.sx !== undefined) b = b.scaleX(p.sx);
    if (p.sy !== undefined) b = b.scaleY(p.sy);
    if (p.sz !== undefined) b = b.scaleZ(p.sz);
    if (p.my !== undefined) b = b.moveY(p.my);
    if (p.mz !== undefined) b = b.moveZ(p.mz);
    if (p.pz !== undefined) b = b.setZ(p.pz).multiplier(peso);
    b.animate(curva, peso);
}

// la registrazione sta in una funzione a parte: nei cicli di Rhino let e const non fanno una
// variabile nuova a ogni giro, e le funzioni registrate vedrebbero tutte l'ultima posa
function registra(event, forma, posa) {
    const potere = 'klyntars:' + forma;
    event.registerForPower(`klyntar/${forma}_${posa.nome}`, potere, posa.priorita || 10, builder => {
        const giocatore = builder.getPlayer();
        let peso;
        if (posa.stato) {
            peso = abilityUtil.isEnabled(giocatore, potere, posa.stato) ? 1.0 : 0.0;
        } else {
            peso = animationUtil.getAnimationTimerAbilityValue(giocatore, potere, posa.timer, builder.getPartialTicks());
        }
        if (peso <= (posa.da || 0.0)) return;
        const parti = builder.isFirstPerson() ? posa.prima : posa.terza;
        if (!parti) return;
        const curva = posa.curva || 'easeInOutCubic';
        let gambe = 1.0;
        for (const parte in parti) {
            if (parti[parte].gambe) {
                gambe = animationUtil.getAnimationTimerAbilityValue(giocatore, potere, 'barragelegs', builder.getPartialTicks());
                break;
            }
        }
        for (const parte in parti) {
            metti(builder, parte, parti[parte], parti[parte].gambe ? peso * gambe : peso, curva);
        }
    });
}

PalladiumEvents.registerAnimations(event => {
    for (let i = 0; i < TUTTE.length; i++) {
        for (let j = 0; j < POSE.length; j++) {
            if (POSE[j].forme.indexOf(TUTTE[i]) >= 0) registra(event, TUTTE[i], POSE[j]);
        }
    }
});
