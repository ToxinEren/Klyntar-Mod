PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/ironarmblock', 'mymod:ironspider', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:ironspider', 'ironblock', builder.getPartialTicks());


        {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-70)
                    .setYRotDegrees(-15)
                    .setZRotDegrees(20)
                    .setX(-7).multiplier(progress)
                    .animate('InOutCubic', progress)
                builder.get('left_arm')
                    .setXRotDegrees(-70)
                    .setYRotDegrees(15)
                    .setZRotDegrees(-20)
                    .setX(7).multiplier(progress)

                    .animate('InOutCubic', progress)
                    ;
            }
        }
    });
});