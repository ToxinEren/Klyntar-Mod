PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/punching2', 10, (builder) => {
        // animation part
const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:ironfist', 'punches', builder.getPartialTicks());


if (builder.isFirstPerson()) {
if (progress > 0.0) {
        builder.get('right_arm')
    .setZ(6).multiplier(progress)
    .animate('InOutCubic', progress)

;
}}     

else {
if (progress > 0.0) {
    builder.get('right_arm')
    .setZ(5).multiplier(progress)
    .animate('InOutCubic', progress)
;
}}
    });
});