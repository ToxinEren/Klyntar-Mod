StartupEvents.registry('palladium:abilities', (event) => {
    event.create('mymod:screen_shake')
        .icon(palladium.createItemIcon('minecraft:coal'))
        .addProperty("base_intensity", "float", 1.0, "base intensity")
        .addProperty("max_intensity", "float", 5.0, "max intensity")
        .tick((entity, entry, holder, enabled) => {
            if (enabled && entity.isPlayer()) {
                const base_intensity = entry.getPropertyByName("base_intensity")
                const max_intensity = entry.getPropertyByName("max_intensity")
                let pitch = (Math.random() * max_intensity - max_intensity / 2) * base_intensity
                let yaw = (Math.random() * max_intensity - max_intensity / 2) * base_intensity
                entity.server.runCommandSilent(`execute as ${entity.getGameProfile().getName()} at @s run tp @s ~ ~ ~ ~${pitch} ~${yaw}`);
            }
        });
});