PalladiumEvents.registerAnimations((event) => {
    event.register('throgaddon/slampunch2', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:ironfist', 'slampunch2', builder.getPartialTicks());

        if (builder.isFirstPerson()) {
            if (progress > 0.0) {

                ;
            }
        }
        else {
            {
                if (progress > 0.5) {
                    builder.get('right_arm')
                        .setZ(6).multiplier(progress)
                        .animate('InOutCubic', progress)
                }
            }
        }
    });
});