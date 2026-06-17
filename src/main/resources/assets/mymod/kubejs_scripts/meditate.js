PalladiumEvents.registerAnimations((event) => {
    event.register('throgaddon/meditate', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:ironfist', 'meditate', builder.getPartialTicks());

        if (builder.isFirstPerson()) {
            if (progress > 0.0) {

                ;
            }
        }
        else {
            if (progress > 0.0) {
                builder.get('right_arm')
                    .setXRotDegrees(-50)
                    .setYRotDegrees(-25)
                    .animate('InOutCubic', progress)
                builder.get('left_arm')
                    .setXRotDegrees(-50)
                    .setYRotDegrees(25)
                    .animate('InOutCubic', progress)
                builder.get('right_leg')
                    .setXRotDegrees(-50)
                    .setYRotDegrees(-50)
                    .setZRotDegrees(-27.5)
                    .setX(-5).multiplier(progress)
                    .setZ(2).multiplier(progress)
                    .setY(10).multiplier(progress)
                    .animate('InOutCubic', progress)

                builder.get('left_leg')
                    .setXRotDegrees(-50)
                    .setYRotDegrees(50)
                    .setZRotDegrees(27.5)
                    .setX(5).multiplier(progress)
                    .setZ(2).multiplier(progress)
                    .setY(8).multiplier(progress)
                    .animate('InOutCubic', progress)

                    ;
            }
        }
    });
});