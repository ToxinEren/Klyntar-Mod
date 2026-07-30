PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/duelpistolshoot', 'klyntars:dp_duel_pistols', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:dp_duel_pistols', 'shootanimation', builder.getPartialTicks());



        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZ(-6).multiplier(progress)
                .setX(3).multiplier(progress)
                .setY(3).multiplier(progress)

                .setZRotDegrees(-30)

                .animate('InOutCubic', progress)
            builder.get('left_arm')
                .setZ(-6).multiplier(progress)
                .setX(-3).multiplier(progress)
                .setY(3).multiplier(progress)
                .setZRotDegrees(30)


                .animate('InOutCubic', progress)


        }


    });
});