PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/carnageblock', 'klyntars:carnage', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:carnage', 'carnageblock', builder.getPartialTicks());


        {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-70)
                    .setYRotDegrees(-10)
                    .setZRotDegrees(20)
                    .setX(-7).multiplier(progress)
                    .animate('InOutCubic', progress)
                builder.get('left_arm')
                    .setXRotDegrees(-70)
                    .setYRotDegrees(10)
                    .setZRotDegrees(-20)
                    .setX(7).multiplier(progress)

                    .animate('InOutCubic', progress)
                    ;
                builder.get('left_leg')
                    .setYRotDegrees(10)
                    .setZRotDegrees(-10)


                    .animate('InOutCubic', progress)
                    ;
                builder.get('right_leg')

                    .setYRotDegrees(10)
                    .setZRotDegrees(10)


                    .animate('InOutCubic', progress)
                    ;
            }
        }
    });
});