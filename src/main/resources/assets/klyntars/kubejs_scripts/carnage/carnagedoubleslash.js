PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/carnagedoubleslash', 'klyntars:carnage', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:carnage', 'doubleslash', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-100)
                .setYRotDegrees(70)



                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-80)
                .setYRotDegrees(-70)

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
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setYRotDegrees(10)
                .animate('easeInOutCubic', progress);
        } if (progress > 0.5 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-40)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.2 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-40)
                .animate('easeInOutCubic', progress);
        }

    });
});
