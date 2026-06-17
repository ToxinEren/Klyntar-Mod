PalladiumEvents.registerAnimations((event) => {
    event.register('throgaddon/punching3', 10, (builder) => {
        // animation part
const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:ironfist', 'punches2', builder.getPartialTicks());

if (builder.isFirstPerson()) {
if (progress > 0.0) {
        builder.get('left_arm')
    .setZ(6).multiplier(progress)
    .animate('InOutCubic', progress)

;
}}     

else {     

if (progress > 0.0) {
    builder.get('left_arm')
    .setZ(5).multiplier(progress)
    .animate('InOutCubic', progress)
;
}}
    });
});