StartupEvents.registry('mob_effect', event => {
  event.create('mymod:venom_infection')
    .effectTick((entity, lvl) => {
      if (!entity.server) return
      superpowerUtil.addSuperpower(entity, "mymod:venom");
    })
    .color("#111111")
})
