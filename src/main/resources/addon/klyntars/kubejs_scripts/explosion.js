StartupEvents.registry('palladium:abilities', (event) => {
    event.create('klyntars:explosion')
        .icon(palladium.createItemIcon('palladium:vibranium_circuit'))
        .documentationDescription('Make you Explode')

        .addProperty('cause_fire', 'boolean', false, 'if the explosion create fire')
        .addProperty('radius', 'integer', 1, 'radius of the explosion')
        .addProperty('self_damage', 'integer', 1, 'the damage on yourself')

        .firstTick((entity, entry, holder, enabled) => {
            // solo il server fa esplodere: sul client l'abilita' gira anche per gli altri
            // giocatori, e ogni client scavava buchi fantasma e riceveva spinte doppie
            if (entity.level.isClientSide()) return;
            if (enabled && entity.isPlayer()) {
                const causingfire = entry.getPropertyByName("cause_fire");
                const radius = entry.getPropertyByName("radius");
                const self_dmg = entry.getPropertyByName("self_damage");

                const posX = entity.x;
                const posY = entity.y;
                const posZ = entity.z;
                const explosion = entity.level.createExplosion(posX, posY, posZ);

                if (causingfire == true) {
                    explosion.causesFire(true);
                } else {
                    explosion.causesFire(false);
                }

                explosion.strength(radius);
                explosion.exploder(entity);
                // 'mob' e non 'block': i blocchi si rompono solo se il server lo permette
                // (gamerule mobGriefing), come per le esplosioni dei creeper
                explosion.explosionMode('mob');
                entity.attack(self_dmg)

                explosion.explode();
            }
        });
});