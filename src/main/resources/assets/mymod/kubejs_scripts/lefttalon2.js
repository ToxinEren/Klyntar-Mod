PalladiumEvents.registerAnimations((event) => {
 event.registerForPower('klyntar/lefttalon2','mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'lefttalon2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-120)
                .setZRotDegrees(60)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-35)
                .setZRotDegrees(30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(-40)
                .animate('easeInOutCubic', progress);
        }
          if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setXRotDegrees(-30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setXRotDegrees(15)
                .animate('easeInOutCubic', progress);
        }
         if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(50)
                .animate('easeInOutCubic', progress);
        }

    });
});
