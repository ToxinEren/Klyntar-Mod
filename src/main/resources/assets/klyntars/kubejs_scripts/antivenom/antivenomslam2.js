PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/antivenomslam2', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:antivenom', 'venomslam2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-70)
                .scaleZ(1.5)
                .scaleX(1.5)
                .scaleY(1.5)


                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(30)
                .setZRotDegrees(-30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setXRotDegrees(15)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setXRotDegrees(-20)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(30)
                .setXRotDegrees(-30)
                .animate('easeInOutCubic', progress)
                .animate('easeInOutCubic', progress)

                ;
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(-70)
                .scaleZ(1.3)
                .scaleX(1.3)
                .scaleY(1.3)
                .animate('easeInOutCubic', progress);
        }

    });
});
