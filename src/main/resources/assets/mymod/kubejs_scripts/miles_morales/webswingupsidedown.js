PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/webswinganimatiomiles3.1', 'mymod:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spidermanmiles', 'webswinganimation3.1', builder.getPartialTicks());


        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')

                .setXRotDegrees(115)
                .setY(20)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')

                .setXRotDegrees(-50)


                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')

                .setXRotDegrees(0)
                .setZRotDegrees(0)


                .animate('easeInOutCubic', progress);
        } if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')

                .setXRotDegrees(-30)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')

                .setXRotDegrees(-20)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('head')

                .setXRotDegrees(30)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(-40)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(40)
                .animate('easeInOutCubic', progress);
        }

    });
});
