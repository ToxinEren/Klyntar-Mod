PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/antispinattack2', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:antivenom', 'spinattack2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(90)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(-90)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.2 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .scaleY(2)
                .animate('easeInOutCubic', progress);

        }
        if (progress > 0.2 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .scaleY(2)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.5 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(50)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.2 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-50)
                .animate('easeInOutCubic', progress);
        }
    });
});
