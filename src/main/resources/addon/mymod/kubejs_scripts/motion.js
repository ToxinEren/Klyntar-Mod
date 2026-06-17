let ClientboundSetEntityMotionPacket = Java.loadClass('net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket');

StartupEvents.registry('palladium:abilities', (event) => {
    event.create('mymod:motion')
        .icon(palladium.createItemIcon('minecraft:blaze_rod'))
        .addProperty('motion_scale', 'float', 1, 'Motion Scale')
        .tick((entity, entry, holder, enabled) => {
            if (enabled) {
                let motionscale = entry.getPropertyByName('motion_scale');
                let motion = entity.getLookAngle().scale(motionscale);
                entity.setDeltaMovement(motion);

                if (entity.isPlayer()) {
                    entity.connection.send(new ClientboundSetEntityMotionPacket(entity));
                }
            }
        });

    event.create('mymod:motion_y')
        .icon(palladium.createItemIcon('minecraft:blaze_rod'))
        .addProperty('motion_scale', 'float', 1, 'Motion Scale')
        .tick((entity, entry, holder, enabled) => {
            if (enabled) {
                let motionscale = entry.getPropertyByName('motion_scale');
                let motion = entity.getDeltaMovement().multiply(1, 0, 1).add(0, motionscale, 0);
                entity.setDeltaMovement(motion);

                if (entity.isPlayer()) {
                    entity.connection.send(new ClientboundSetEntityMotionPacket(entity));
                }
            }
        });

    event.create('mymod:motion_xz')
        .icon(palladium.createItemIcon('minecraft:blaze_rod'))
        .addProperty('motion_scale', 'float', 1, 'Motion Scale')
        .tick((entity, entry, holder, enabled) => {
            if (enabled) {
                let motionscale = entry.getPropertyByName('motion_scale');
                let motion = entity.getLookAngle().scale(motionscale);
                entity.setDeltaMovement(motion);
                entity.motionY = -1;

                if (entity.isPlayer()) {
                    entity.connection.send(new ClientboundSetEntityMotionPacket(entity));
                }
            }
        });
});