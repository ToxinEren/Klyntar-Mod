PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spiderslingshot', 'mymod:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spiderman', 'slingshotanimation', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-90)
                .setYRotDegrees(30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-90)
                .setYRotDegrees(-30)
                .animate('easeInOutCubic', progress);
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setXRotDegrees(0)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setXRotDegrees(0)
                .animate('easeInOutCubic', progress);
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setXRotDegrees(20)

                .animate('easeInOutCubic', progress);
        }


    });
});
