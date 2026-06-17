PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spidermilesairdive2', 'mymod:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spidermanmiles', 'airdive2', builder.getPartialTicks());




        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')

                .setXRotDegrees(130)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')

                .setXRotDegrees(-200)
                .setZRotDegrees(20)


                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')

                .setXRotDegrees(-200)
                .setZRotDegrees(-20)


                .animate('easeInOutCubic', progress);
        } if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')

                .setXRotDegrees(35)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')

                .setXRotDegrees(25)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('head')

                .setXRotDegrees(30)

                .animate('easeInOutCubic', progress);
        }




    });
});
