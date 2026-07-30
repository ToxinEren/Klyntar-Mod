PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/block', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:ironfist', 'fistblock', builder.getPartialTicks());


        {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-80)
                    .setZRotDegrees(-30)
                    .animate('InOutCubic', progress)
                builder.get('left_arm')
                    .setXRotDegrees(-60)
                    .setZRotDegrees(-50)
                    .animate('InOutCubic', progress)
                    ;
            }
        }
    });
});