PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/duelpistolleft', 'mymod:dp_duel_pistols', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:dp_duel_pistols', 'leftraise', builder.getPartialTicks());


        {
            if (progress > 0.0 && !builder.isFirstPerson()) {

                builder.get('left_arm')
                    .setXRotDegrees(-150)
                    .animate('InOutCubic', progress)

                    ;
            }
        }
        if (progress > 0.0 && builder.isFirstPerson()) {

            builder.get('left_arm')

                .setYRotDegrees(-55)
                .setZRotDegrees(20)
                .animate('InOutCubic', progress)


        }


    });
});