PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/glidepose', 'klyntars:greengoblin', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:greengoblin', 'glidepose', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZRotDegrees(15)
                .setXRotDegrees(-15)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZRotDegrees(-15)
                .setXRotDegrees(-15)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setXRotDegrees(-15)
                .setZRotDegrees(0)
                .setYRotDegrees(0)

        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('head')
                .setXRotDegrees(0)
                .setZRotDegrees(0)
                .setYRotDegrees(0)

        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')





                .animate('easeInOutCubic', progress);
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')








                .animate('easeInOutCubic', progress);
        }




    });
});
