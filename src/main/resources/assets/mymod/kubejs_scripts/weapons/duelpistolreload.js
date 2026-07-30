PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/duelpistolreload', 'mymod:dp_duel_pistols', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:dp_duel_pistols', 'reload', builder.getPartialTicks());


        {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-115)
                    .animate('InOutCubic', progress)
                builder.get('left_arm')
                    .setXRotDegrees(-115)
                    .animate('InOutCubic', progress)

                    ;
            }
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setYRotDegrees(55)
                .setZRotDegrees(10)
                .animate('InOutCubic', progress)
            builder.get('left_arm')
                .setYRotDegrees(-55)
                .setZRotDegrees(-10)
                .animate('InOutCubic', progress)


        }


    });
});