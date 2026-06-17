StartupEvents.registry('palladium:abilities', (event) => {
    event.create('mymod:explosion')
        .icon(palladium.createItemIcon('palladium:vibranium_circuit'))
        .documentationDescription('Make you Explode')

        .addProperty('cause_fire', 'boolean', false, 'if the explosion create fire')
        .addProperty('radius', 'integer', 1, 'radius of the explosion')
        .addProperty('self_damage', 'integer', 1, 'the damage on yourself')

        .firstTick((entity, entry, holder, enabled) => {
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
                explosion.explosionMode('block');
                entity.attack(self_dmg)

                explosion.explode();
            }
        });
});