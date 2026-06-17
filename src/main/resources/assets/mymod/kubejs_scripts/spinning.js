PalladiumEvents.registerAnimations((event) => {
    event.register('throgaddon/spinning', 10, (builder) => {
        // animation part
const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:throg', 'spinblock', builder.getPartialTicks());

if (builder.isFirstPerson()) {
if (progress > 0.0) {

    ;
}
}      
else {
if (progress > 0.0 && !builder.isFirstPerson()) {
    builder.get('right_arm')
    .setXRotDegrees(-90)

;
}}
    });
});