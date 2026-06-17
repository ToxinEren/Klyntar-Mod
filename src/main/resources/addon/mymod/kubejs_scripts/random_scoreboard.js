//credit to WolfDude24
StartupEvents.registry('palladium:abilities', (event) => {
    event.create('mymod:random_scoreboard')
        .icon(palladium.createItemIcon('minecraft:clock'))
        .addProperty("min_value", "float", 1, "Minimum random value")
        .addProperty("max_value", "float", 100, "Maximum random value")
        .addProperty("objective_score", "string", "crisispack.random", "Scoreboard objective to assign the random value")

        .tick((entity, entry, holder, enabled) => {
            if (enabled) {
                let min_value = entry.getPropertyByName("min_value");
                let max_value = entry.getPropertyByName("max_value");
                let objective_score = entry.getPropertyByName("objective_score");

                let random = Math.random();
                let rounded = Math.floor(random * (max_value - min_value + 1)) + min_value;

                palladium.scoreboard.setScore(entity, objective_score, rounded);
            }
        });
});
