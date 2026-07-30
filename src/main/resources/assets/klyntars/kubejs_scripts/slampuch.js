PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/slampunch', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:ironfist', 'slampunch', builder.getPartialTicks());

        if (builder.isFirstPerson()) {
            if (progress > 0.0) {

                ;
            }
        }
        else {
            {
                if (progress > 0.5) {
                    builder.get('right_arm')
                        .setXRotDegrees(-90)
                        .animate('InOutCubic', progress)
                }
            }
        }
    });
});