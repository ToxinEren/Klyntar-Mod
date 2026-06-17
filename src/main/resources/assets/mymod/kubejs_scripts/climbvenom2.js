PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomclimb2', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'climbup2', builder.getPartialTicks());


        {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-90)
                    .animate('InOutCubic', progress)
                builder.get('left_arm')
                    .setXRotDegrees(-150)

                    .animate('InOutCubic', progress)
                    ;
                builder.get('left_leg')
                    .setXRotDegrees(-50)



                    .animate('InOutCubic', progress)
                    ;
                builder.get('right_leg')

                    .setXRotDegrees(-80)


                    .animate('InOutCubic', progress)
                    ;
            }
        }
    });
});