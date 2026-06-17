StartupEvents.registry('palladium:condition_serializer', (event) => {
    event.create('mymod:venom_idle_time')
        .addProperty('ticks', 'integer', 100, 'Ticks the player must stay idle.')
        .addProperty('interval', 'integer', 80, 'Ticks between repeated idle animation triggers.')
        .addProperty('data', 'string', 'mymod.venom_idle_ticks', 'Persistent data key.')
        .test((entity, props) => {
            if (!entity || !entity.isPlayer()) return false;

            const dataKey = props.get('data');
            const requiredTicks = props.get('ticks');
            const intervalTicks = props.get('interval');
            const lastTickKey = dataKey + '.last_tick';
            const gameTime = entity.level.time;
            const moving =
                palladium.getProperty(entity, 'forward_key_down') === true ||
                palladium.getProperty(entity, 'backwards_key_down') === true ||
                palladium.getProperty(entity, 'right_key_down') === true ||
                palladium.getProperty(entity, 'left_key_down') === true ||
                palladium.scoreboard.getScore(entity, 'oms.player.jump_key', 0) === 1 ||
                !entity.onGround();

            if (moving) {
                entity.persistentData[dataKey] = 0;
                entity.persistentData[lastTickKey] = gameTime;
                return false;
            }

            const lastTick = Number(entity.persistentData[lastTickKey] || 0);
            let idleTicks = Number(entity.persistentData[dataKey] || 0);
            if (lastTick === 0) {
                entity.persistentData[dataKey] = 0;
                entity.persistentData[lastTickKey] = gameTime;
                return false;
            }
            if (gameTime !== lastTick) {
                idleTicks += Math.max(1, gameTime - lastTick);
                entity.persistentData[dataKey] = idleTicks;
                entity.persistentData[lastTickKey] = gameTime;
            }
            return idleTicks >= requiredTicks && (idleTicks - requiredTicks) % intervalTicks === 0;
        });
});
