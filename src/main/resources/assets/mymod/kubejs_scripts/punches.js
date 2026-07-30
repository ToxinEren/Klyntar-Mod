PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/punching', 10, (builder) => {
        // animation part
const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:ironfist', 'punches3', builder.getPartialTicks());

if (builder.isFirstPerson()) {
if (progress > 0.0) {

    ;
}
}      
else {
if (progress > 0.0) {
    builder.get('right_arm')
    .setXRotDegrees(-90)
      builder.get('left_arm')
    .setXRotDegrees(-90)

;
}}
    });
});