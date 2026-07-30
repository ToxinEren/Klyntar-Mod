StartupEvents.registry('mob_effect', event => {
  event.create('klyntars:venom_infection')
    .effectTick((entity, lvl) => {
      if (!entity.server) return
      superpowerUtil.addSuperpower(entity, "klyntars:venom");
    })
    .color("#111111")
})
