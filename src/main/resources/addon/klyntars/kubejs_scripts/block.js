StartupEvents.registry('palladium:abilities', event => {
  event.create('klyntars:block')
    .icon(palladium.createItemIcon('minecraft:shield'))
    .documentationDescription('Blocks forward damage.')
    .tick((entity, entry, holder, enabled) => {
      if (enabled && entity.isPlayer()) {
        let target = entity.rayTrace(5).entity;
        if (target !== null) {
          entity.invulnerableTime = 20;
        } else if (target == null) {
          entity.invulnerableTime = 0;
        }
      }
    })
});