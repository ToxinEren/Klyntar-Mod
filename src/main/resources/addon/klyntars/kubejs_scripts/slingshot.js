let ClientboundSetEntityMotionPacket = Java.loadClass('net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket');
let Text = Java.loadClass('net.minecraft.network.chat.Component');

StartupEvents.registry('palladium:abilities', (event) => {
    event.create('klyntars:slingshot')
        .icon(palladium.createItemIcon('minecraft:observer'))
        .addProperty('range', 'integer', 5, 'Detection distance')
        .addProperty('max_charge', 'float', 4.5, 'Max Launch Strength')
        .addProperty('charge_speed', 'float', 0.08, 'How fast it charges')
        .addProperty('forward_force', 'float', 1.0, 'Forward Multiplier')
        .addProperty('upward_force', 'float', 0.5, 'Upward Kick Amount')
        .addProperty('min_shake', 'float', 0.5, 'Starting Shake Intensity')
        .addProperty('max_shake', 'float', 4.0, 'Max Shake Intensity')
        .addProperty('beam_particle', 'string', 'minecraft:cloud', 'Beam Particle ID')
        .addProperty('start_sound', 'string', 'minecraft:entity.leash_knot.place', 'Start Sound')
        .addProperty('launch_sound_1', 'string', 'minecraft:entity.ender_dragon.flap', 'Launch Sound 1')
        .addProperty('launch_sound_2', 'string', 'minecraft:entity.firework_rocket.large_blast', 'Launch Sound 2')

        .tick((entity, entry, holder, enabled) => {
            if (entity.level.isClientSide() || !entity.isPlayer()) return;

            let data = entity.persistentData;

            if (enabled) {
                let range = entry.getPropertyByName('range');
                let forward = entity.getForward();
                let right = new Vec3(-forward.z(), 0, forward.x()).normalize();
                let left = right.scale(-1);

                let getHit = (vec) => {
                    // Start ray 1.5 blocks away to avoid hitting the player
                    for (let i = 1.5; i <= range; i += 0.5) {
                        let cx = vec.x() * i;
                        let cy = entity.eyeHeight + (vec.y() * i);
                        let cz = vec.z() * i;
                        let block = entity.level.getBlock(entity.x + cx, entity.y + cy, entity.z + cz);
                        if (block && !block.air && !block.id.contains("air")) return { x: cx, y: cy, z: cz };
                    }
                    return null;
                };

                let dR = getHit(forward.add(right).normalize());
                let dL = getHit(forward.add(left).normalize());

                // --- LOGIC GATE ---
                // If not currently slingshotting, check if we can start
                if (!data.getBoolean('is_slingshotting')) {
                    if (entity.onGround() && dR && dL) {
                        data.putBoolean('is_slingshotting', true);
                        data.putFloat('locked_yaw', entity.yaw);
                        data.putFloat('locked_pitch', entity.pitch);
                        data.putDouble('locked_x', entity.x);
                        data.putDouble('locked_y', entity.y);
                        data.putDouble('locked_z', entity.z);
                        data.putFloat('charge_amount', 0.0);
                        entity.runCommandSilent(`playsound ${entry.getPropertyByName('start_sound')} player @a ~ ~ ~ 1 1.2`);
                    } else {
                        // Error message ONLY if we aren't already in the middle of a charge
                        entity.setStatusMessage(Text.literal("§ccannot perform slingshot you are either not on ground or nothing to attach to"));
                        return;
                    }
                }

                // --- CHARGING STATE ---
                // Once the gate is open, we only care about the wall connection
                if (dR && dL) {
                    let currentCharge = data.getFloat('charge_amount');
                    let maxCharge = entry.getPropertyByName('max_charge');
                    let chargeSpeed = entry.getPropertyByName('charge_speed');

                    if (currentCharge < maxCharge) {
                        data.putFloat('charge_amount', currentCharge + chargeSpeed);
                    }

                    let chargePercent = Math.min(currentCharge / maxCharge, 1.0);

                    // Screen Shake logic from your script
                    let currentIntensity = entry.getPropertyByName('min_shake') + (chargePercent * (entry.getPropertyByName('max_shake') - entry.getPropertyByName('min_shake')));
                    let shakePitch = (Math.random() * currentIntensity - currentIntensity / 2);
                    let shakeYaw = (Math.random() * currentIntensity - currentIntensity / 2);

                    // One-command TP to keep the player still and add the jitter
                    entity.runCommandSilent(`tp @s ${data.getDouble('locked_x')} ${data.getDouble('locked_y')} ${data.getDouble('locked_z')} ${data.getFloat('locked_yaw') + shakeYaw} ${data.getFloat('locked_pitch') + shakePitch}`);

                    // UI Bar
                    let totalSquares = 8;
                    let filledCount = Math.floor(chargePercent * totalSquares);
                    let bar = "";
                    let isFull = currentCharge >= maxCharge;
                    let color = isFull ? "§c" : "§f";

                    for (let i = 0; i < totalSquares; i++) {
                        bar += (isFull || i < filledCount) ? "■ " : "□ ";
                    }
                    entity.setStatusMessage(Text.literal(`§lPOWER: ${color}${bar}`));

                    // Particles
                    let pType = entry.getPropertyByName('beam_particle');
                    [dR, dL].forEach(hit => {
                        for (let j = 4; j <= 20; j++) {
                            let r = j / 20;
                            entity.runCommandSilent(`particle ${pType} ${entity.x + hit.x * r} ${entity.y + entity.eyeHeight + (hit.y - entity.eyeHeight) * r} ${entity.z + hit.z * r} 0 0 0 0 1`);
                        }
                    });
                } else {
                    // Fail State: If walls are lost during charge
                    data.putBoolean('is_slingshotting', false);
                    data.remove('charge_amount');
                    entity.setStatusMessage(Text.literal("§cAttachment Lost!"));
                }

            } else {
                // --- RELEASE & LAUNCH ---
                if (data.getBoolean('is_slingshotting')) {
                    let launchPower = data.getFloat('charge_amount');
                    if (launchPower > 0.2) {
                        let look = entity.getLookAngle();
                        let fMult = entry.getPropertyByName('forward_force');
                        let uMult = entry.getPropertyByName('upward_force');

                        // Use the properties to define the arc
                        let motion = new Vec3(look.x() * fMult, look.y() + uMult, look.z() * fMult).normalize().scale(launchPower);

                        entity.setDeltaMovement(motion);
                        if (entity.connection != null) {
                            entity.connection.send(new ClientboundSetEntityMotionPacket(entity));
                        }
                        entity.runCommandSilent(`playsound ${entry.getPropertyByName('launch_sound_1')} player @a ~ ~ ~ 1 1`);
                        entity.runCommandSilent(`playsound ${entry.getPropertyByName('launch_sound_2')} player @a ~ ~ ~ 1 0.8`);
                        entity.runCommandSilent(`particle minecraft:explosion_emitter ~ ~ ~ 0 0 0 0 1`);
                    }
                    // Full Cleanup
                    data.putBoolean('is_slingshotting', false);
                    data.remove('charge_amount');
                    data.remove('locked_yaw');
                    entity.setStatusMessage(Text.literal(""));
                }
            }
        });
});