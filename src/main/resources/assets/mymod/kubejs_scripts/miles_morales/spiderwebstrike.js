PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spidermilesmanrightstab', 'mymod:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spidermanmiles', 'webstrikepunch', builder.getPartialTicks());


        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZRotDegrees(50)



                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZRotDegrees(-50)



                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')

                .setXRotDegrees(0)





        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')

                .setXRotDegrees(0)





        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-120)
                .setZRotDegrees(-30)
                .animate('easeInOutCubic', progress);

        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-120)
                .setZRotDegrees(30)
                .animate('easeInOutCubic', progress);

        }

    });
});
