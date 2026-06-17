//Made by FSang18
StartupEvents.registry('mob_effect', event => {
  event.create('mymod:upward')
    .effectTick((entity, lvl) => {
      if (!entity.server) return
      //This line allows your effect to grant a specific power when enabled and will not be removed unless you code it to do so in the given power
      superpowerUtil.addSuperpower(entity, "mymod:upward");
    })
    //Replace HEXCODE below with a color's Hex code
    .color("#ffffffff")
}) 