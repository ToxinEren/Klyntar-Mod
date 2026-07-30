PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/venomspiderpunch2', 'mymod:venomspidey', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venomspidey', 'venompunch2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .scaleY(10)
                .setZ(-20)
                .setXRotDegrees(-90)



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
                .setYRotDegrees(10)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .scaleY(10)
                .setZ(-2).multiplier(progress)
                .setYRotDegrees(-20)
                .setZRotDegrees(-30)
                .setXRotDegrees(-10)
                .animate('easeInOutCubic', progress);
        }

    });
});
