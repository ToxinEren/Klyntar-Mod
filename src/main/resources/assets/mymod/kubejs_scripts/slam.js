PalladiumEvents.registerAnimations((event) => {
    event.register('throgaddon/slam', 10, (builder) => {
        // animation part
const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:ironfist', 'slam', builder.getPartialTicks());

if (builder.isFirstPerson()) {
if (progress > 0.0) {

    ;
}
}      
else {
if (progress > 0.0) {
      builder.get('body')
    .setXRotDegrees(-60)
    .animate('InOutCubic', progress)
    builder.get('right_leg')
    .setXRotDegrees(-40)
    .animate('InOutCubic', progress)
     builder.get('left_leg')
    .setXRotDegrees(-40)
    .animate('InOutCubic', progress)


;
}}
    });
});