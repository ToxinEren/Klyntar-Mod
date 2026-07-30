PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/duelpistolaim', 'klyntars:dp_duel_pistols', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:dp_duel_pistols', 'aim', builder.getPartialTicks());


        {

            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-90)
                    .animate('InOutCubic', progress)
            }


            if (progress > 0.0 && !builder.isFirstPerson()) {

                builder.get('left_arm')
                    .setXRotDegrees(-90)
                    .animate('InOutCubic', progress)

                    ;
            }
        }




    });
});