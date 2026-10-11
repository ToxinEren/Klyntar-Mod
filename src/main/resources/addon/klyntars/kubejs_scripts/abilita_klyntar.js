// Le abilita' e le condizioni di Klyntar fatte in KubeJS (10/10/2026). Gli id e le proprieta' sono
// quelli che i poteri gia' usano.

const $Util = Java.loadClass('net.minecraft.Util');
const $Player = Java.loadClass('net.minecraft.world.entity.player.Player');
const $PvpRules = Java.loadClass('modKlyntar.player.PvpRules');

// ------------------------------------------------------------------ Grab & Crush

// si prende solo cio' che e' davanti, a portata di braccio, e lo si tiene alla mano
const PRESA_PORTATA = 3.0;
const PRESA_AVANTI = 2.2;      // la mano, davanti agli occhi
const PRESA_SOTTO = 0.6;       // e un po' sotto
// li leggono anche le animazioni e GrabCrushLandingHandler (lo schianto quando tocca terra)
const PRESA_IN_CORSO = 'Venom.Grab.Playing';
const PRESA_LASCIATO = 'venom_grab_dropped';

// creature vive soltanto; un altro giocatore solo dove il PvP lo consente
function sipuoPrendere(chi, cosa) {
    if (cosa == null || !cosa.isLiving() || !cosa.isAlive() || cosa.isSpectator()) return false;
    if (cosa.type == 'minecraft:armor_stand') return false;
    if (cosa instanceof $Player) return chi instanceof $Player && $PvpRules.colpibile(chi, cosa);
    return true;
}

function preso(livello, entry) {
    let id = entry.getPropertyByName('held_entity');
    if (id == null || id == $Util.NIL_UUID) return null;
    let tutte = livello.getEntities();
    for (let i = 0; i < tutte.size(); i++) {
        if (tutte.get(i).uuid.equals(id)) return tutte.get(i);
    }
    return null;
}

StartupEvents.registry('palladium:abilities', event => {
    event.create('klyntars:grab')
        .icon(palladium.createItemIcon('minecraft:player_head'))
        .documentationDescription('Grabs the creature in front of you and holds it in your hand.')
        .addProperty('range_objective', 'string', 'Klyntars.Range', 'Scoreboard objective kept for compatibility.')
        .addProperty('strength', 'float', 1.0, 'How fast the held creature follows the hand.')
        .addProperty('damage', 'float', 2, 'Damage dealt by what you hold to whatever it hits.')
        .addUniqueProperty('held_entity', 'uuid', $Util.NIL_UUID)

        .firstTick((entity, entry, holder, enabled) => {
            if (!enabled) return;
            palladium.scoreboard.setScore(entity, PRESA_IN_CORSO, 1);
            let mira = entity.rayTrace(PRESA_PORTATA, false);
            if (sipuoPrendere(entity, mira.entity)) entry.setUniquePropertyByName('held_entity', mira.entity.uuid);
        })

        .tick((entity, entry, holder, enabled) => {
            if (!enabled) return;
            let tenuto = preso(entity.level, entry);
            if (tenuto == null) return;
            let mano = entity.getEyePosition()
                .add(entity.getLookAngle().scale(PRESA_AVANTI))
                .add(0, -PRESA_SOTTO, 0);
            tenuto.setDeltaMovement(mano.subtract(tenuto.getEyePosition()).scale(entry.getPropertyByName('strength')));
            tenuto.resetFallDistance();
            // il movimento di un giocatore lo decide il suo client: va avvisato
            if (tenuto instanceof $Player) tenuto.hurtMarked = true;
            let box = tenuto.getBoundingBox();
            entity.level.sendParticles('minecraft:enchant', tenuto.x, tenuto.y, tenuto.z, 1,
                box.getXsize(), box.getYsize(), box.getZsize(), 0);
        })

        .lastTick((entity, entry, holder, enabled) => {
            palladium.scoreboard.setScore(entity, PRESA_IN_CORSO, 0);
            let tenuto = preso(entity.level, entry);
            if (tenuto == null) return;
            tenuto.addTag(PRESA_LASCIATO);
            entry.setUniquePropertyByName('held_entity', $Util.NIL_UUID);
        });

    // -------------------------------------------------------------- l'esplosione
    event.create('klyntars:explosion')
        .icon(palladium.createItemIcon('minecraft:tnt'))
        .documentationDescription('An explosion centred on you.')
        .addProperty('cause_fire', 'boolean', false, 'Whether the explosion starts fires.')
        .addProperty('radius', 'integer', 1, 'Strength of the explosion.')
        .addProperty('self_damage', 'integer', 1, 'Damage you take yourself.')
        .firstTick((entity, entry, holder, enabled) => {
            // solo il server: sul client l'abilita' gira anche per gli altri giocatori
            if (!enabled || entity.level.isClientSide() || !entity.isPlayer()) return;
            let scoppio = entity.level.createExplosion(entity.x, entity.y, entity.z);
            scoppio.causesFire(entry.getPropertyByName('cause_fire') == true);
            scoppio.strength(entry.getPropertyByName('radius'));
            scoppio.exploder(entity);
            // come i creeper: i blocchi si rompono solo se la gamerule mobGriefing lo permette
            scoppio.explosionMode('mob');
            let danno = entry.getPropertyByName('self_damage');
            if (danno > 0) entity.attack(danno);
            scoppio.explode();
        });

    // -------------------------------------------------------------- una proprieta' di Palladium
    event.create('klyntars:palladium_property_modifier')
        .icon(palladium.createItemIcon('minecraft:comparator'))
        .documentationDescription('Adds to, subtracts from or sets a Palladium property every tick.')
        .addProperty('property_name', 'string', 'example_property', 'The property to change.')
        .addProperty('adjustment_type', 'string', 'add', 'add, subtract or set.')
        .addProperty('adjustment_amount', 'integer', 1, 'By how much.')
        .tick((entity, entry, holder, enabled) => {
            if (!enabled || !entity.isPlayer()) return;
            let nome = entry.getPropertyByName('property_name');
            let quanto = entry.getPropertyByName('adjustment_amount');
            let modo = entry.getPropertyByName('adjustment_type');
            let ora = palladium.getProperty(entity, nome);
            if (modo === 'set') palladium.setProperty(entity, nome, quanto);
            else if (modo === 'add') palladium.setProperty(entity, nome, ora + quanto);
            else if (modo === 'subtract') palladium.setProperty(entity, nome, ora - quanto);
        });

    // -------------------------------------------------------------- un numero a caso
    event.create('klyntars:random_scoreboard')
        .icon(palladium.createItemIcon('minecraft:clock'))
        .documentationDescription('Writes a random whole number into a scoreboard objective every tick.')
        .addProperty('min_value', 'float', 1, 'Smallest value.')
        .addProperty('max_value', 'float', 100, 'Largest value.')
        .addProperty('objective_score', 'string', 'Klyntars.Random', 'Objective to write into.')
        .tick((entity, entry, holder, enabled) => {
            if (!enabled) return;
            let da = Math.ceil(entry.getPropertyByName('min_value'));
            let a = Math.floor(entry.getPropertyByName('max_value'));
            let numero = da + Math.floor(Math.random() * (a - da + 1));
            palladium.scoreboard.setScore(entity, entry.getPropertyByName('objective_score'), numero);
        });
});

// ------------------------------------------------------------------ le condizioni

StartupEvents.registry('palladium:condition_serializer', event => {
    // vera se il giocatore non ha nessuno dei poteri elencati ne' poteri dei namespace elencati
    // (tolti quelli da ignorare): spegne le abilita' quando c'e' un'altra mod di poteri addosso
    event.create('klyntars:disable_if_have_power_or_namespace')
        .addProperty('powers', 'string_array', [], 'Superpowers that switch the ability off.')
        .addProperty('namespaces', 'string_array', [], 'Power namespaces that switch the ability off.')
        .addProperty('ignore_powers', 'string_array', [], 'Superpowers not to count.')
        .test((entity, props) => {
            if (!props) return false;
            for (const id of props.get('powers')) {
                if (palladium.superpowers.hasSuperpower(entity, id)) return false;
            }
            let ignora = props.get('ignore_powers');
            for (const ns of props.get('namespaces')) {
                if (String(ns || '').trim() === '') continue;
                for (const p of palladium.powers.getPowerIdsForNamespace(entity, ns)) {
                    if (!ignora.includes(p.toString())) return false;
                }
            }
            return true;
        });

    // vera se lo stadio dell'arrampicata salvato nei dati del giocatore e' quello chiesto
    event.create('klyntars:is_climb_stage')
        .addProperty('data', 'string', 'klyntar.climb', 'Where the climbing stage is stored.')
        .addProperty('stage', 'string', 'hanging', 'hanging | hanging_move | ceiling')
        .test((entity, props) => entity.persistentData[props.get('data')] == `${props.get('stage')}`);
});
