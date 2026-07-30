let ClientboundSetEntityMotionPacket = Java.loadClass('net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket');

StartupEvents.registry('palladium:abilities', (event) => {
    event.create('klyntars:web_swing')
        .icon(palladium.createItemIcon('minecraft:cobweb'))
        .addProperty('reach', 'integer', 35, 'Max rope length.')
        .addProperty('swing_speed', 'float', 1.25, 'Constant velocity magnitude.')
        // --- ASSIST PROPERTIES ---
        .addProperty('use_swing_assist', 'boolean', true, 'Allow steering with crosshair.')
        .addProperty('steering_power', 'float', 0.15, 'Strength of the steering assist.')

        .addProperty('particle_type', 'string', 'minecraft:cloud', 'Particle ID.')
        .addProperty('exit_boost', 'float', 1.4, 'Final launch power.')
        .addProperty('sound_attach', 'string', 'minecraft:entity.arrow.shoot', 'Sound when web hits.')
        .addProperty('sound_swing', 'string', 'minecraft:entity.leash_knot.place', 'Sound during the arc.')
        .addProperty('sound_release', 'string', 'minecraft:entity.player.small_fall', 'Sound when letting go.')

        .tick((entity, entry, holder, enabled) => {
            if (entity.level.isClientSide() || !entity.isPlayer()) return;

            // --- 1. RELEASE LOGIC ---
            if (!enabled) {
                if (entity.persistentData.getBoolean('is_swinging')) {
                    let look = entity.getLookAngle();
                    let boost = entry.getPropertyByName('exit_boost');
                    let current = entity.getDeltaMovement();

                    entity.setDeltaMovement(current.scale(1.1).add(look.x() * boost, 0.3, look.z() * boost));

                    if (entity.connection != null) {
                        entity.connection.send(new ClientboundSetEntityMotionPacket(entity));
                    }

                    let sRelease = entry.getPropertyByName('sound_release');
                    entity.runCommandSilent(`playsound ${sRelease} player @a ${entity.x} ${entity.y} ${entity.z} 1 1.2`);

                    entity.persistentData.putBoolean('is_swinging', false);
                    entity.persistentData.remove('web_anchor_x');
                    entity.persistentData.remove('web_anchor_y');
                    entity.persistentData.remove('web_anchor_z');
                    entity.persistentData.remove('swing_radius');
                }
                return;
            }

            try {
                // --- 2. ANCHOR LOCKING ---
                if (!entity.persistentData.getBoolean('is_swinging')) {
                    let ray = entity.rayTrace(entry.getPropertyByName('reach'));
                    if (ray != null && ray.block != null) {
                        entity.persistentData.putBoolean('is_swinging', true);
                        entity.persistentData.putDouble('web_anchor_x', ray.block.x + 0.5);
                        entity.persistentData.putDouble('web_anchor_y', ray.block.y + 0.5);
                        entity.persistentData.putDouble('web_anchor_z', ray.block.z + 0.5);

                        let d = Math.sqrt(Math.pow(ray.block.x + 0.5 - entity.x, 2) + Math.pow(ray.block.y + 0.5 - entity.y, 2) + Math.pow(ray.block.z + 0.5 - entity.z, 2));
                        entity.persistentData.putDouble('swing_radius', d);

                        let sAttach = entry.getPropertyByName('sound_attach');
                        entity.runCommandSilent(`playsound ${sAttach} player @a ${entity.x} ${entity.y} ${entity.z} 0.8 1.5`);
                    } else return;
                }

                let ax = entity.persistentData.getDouble('web_anchor_x');
                let ay = entity.persistentData.getDouble('web_anchor_y');
                let az = entity.persistentData.getDouble('web_anchor_z');
                let radius = entity.persistentData.getDouble('swing_radius');

                let dx = ax - entity.x;
                let dy = ay - entity.y;
                let dz = az - entity.z;
                let dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                // --- 3. PHYSICS ---
                if (dist > radius - 0.5) {
                    let motion = entity.getDeltaMovement();
                    let nx = dx / dist;
                    let ny = dy / dist;
                    let nz = dz / dist;

                    let targetSpeed = entry.getPropertyByName('swing_speed');

                    // Basic tension calculation
                    let dot = (motion.x() * nx) + (motion.y() * ny) + (motion.z() * nz);
                    let tx = motion.x() - (dot < 0 ? dot * nx : 0);
                    let ty = motion.y() - (dot < 0 ? dot * ny : 0);
                    let tz = motion.z() - (dot < 0 ? dot * nz : 0);

                    // --- APPLY OPTIONAL ASSIST ---
                    if (entry.getPropertyByName('use_swing_assist')) {
                        let look = entity.getLookAngle();
                        let steering = entry.getPropertyByName('steering_power');
                        tx += look.x() * steering;
                        tz += look.z() * steering;
                    }

                    let pull = (dist - radius) * 0.8;
                    let finalX = tx + (nx * pull);
                    let finalY = ty + (ny * pull) + 0.05;
                    let finalZ = tz + (nz * pull);

                    // Force constant speed regardless of assist status
                    let finalMotion = new Vec3(finalX, finalY, finalZ).normalize().scale(targetSpeed);

                    if (motion.y() < 0.1 && motion.y() > -0.1 && entity.level.time % 6 == 0) {
                        let sSwing = entry.getPropertyByName('sound_swing');
                        entity.runCommandSilent(`playsound ${sSwing} player @a ${entity.x} ${entity.y} ${entity.z} 0.5 1.8`);
                    }

                    entity.setDeltaMovement(finalMotion);
                    entity.fallDistance = 0;

                    if (entity.connection != null) {
                        entity.connection.send(new ClientboundSetEntityMotionPacket(entity));
                    }

                    // --- 4. VISUALS ---
                    let pType = entry.getPropertyByName('particle_type') || 'minecraft:cloud';
                    for (let i = 0; i < 130; i++) {
                        let r = i / 130;

                        entity.level.spawnParticles(
                            pType,
                            true,
                            entity.x + dx * r,
                            (entity.y + 2) + dy * r,
                            entity.z + dz * r,
                            0, 0, 0,
                            0.1, 100
                        )
                    }
                }
            } catch (err) {
                console.error("Web Swing Error: " + err);
            }
        });
});