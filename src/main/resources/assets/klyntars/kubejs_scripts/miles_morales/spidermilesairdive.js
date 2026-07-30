PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/spidermilesairdive', 'klyntars:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:spidermanmiles', 'airdive', builder.getPartialTicks());




        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')

                .setXRotDegrees(-160)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')

                .setXRotDegrees(25)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')

                .setXRotDegrees(25)

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

                .setXRotDegrees(-25)

                .animate('easeInOutCubic', progress);
        }




    });
});
