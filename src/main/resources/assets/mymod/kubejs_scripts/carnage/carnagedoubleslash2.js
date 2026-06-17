PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/carnagedoubleslash2', 'mymod:carnage', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:carnage', 'doubleslash2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-90)
                .setYRotDegrees(-70)


                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-90)
                .setYRotDegrees(70)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(0)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setYRotDegrees(10)
                .setZRotDegrees(10)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setYRotDegrees(10)
                .setZRotDegrees(-10)
                .animate('easeInOutCubic', progress);
        } if (progress > 0.5 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(50)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.2 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(50)
                .animate('easeInOutCubic', progress);
        }


    });
});
