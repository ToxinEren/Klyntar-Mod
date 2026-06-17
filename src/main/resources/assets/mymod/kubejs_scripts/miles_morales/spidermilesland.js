PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spidermilesland', 'mymod:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spidermanmiles', 'spiderland', builder.getPartialTicks());




        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('chest')

                .setXRotDegrees(50)
                .setY(8)


                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')

                .setXRotDegrees(-10)
                .setZRotDegrees(-10)
                .setY(12)
                .setX(-1.3)





                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')

                .setXRotDegrees(0)
                .setZRotDegrees(-100)
                .setY(8)


                .animate('easeInOutCubic', progress);
        } if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')

                .setXRotDegrees(15)
                .setZRotDegrees(-15)
                .setX(-5)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setXRotDegrees(0)

                .setZRotDegrees(-50)
                .setX(2)
                .setY(15)


                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('head')
                .setY(8)
                .setXRotDegrees(0)

                .animate('easeInOutCubic', progress);
        }




    });
});
