PalladiumEvents.registerAnimations((event) => {
 event.registerForPower('klyntar/carnagelefttalon2','klyntars:carnage', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:carnage', 'lefttalon2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-30)
                .setYRotDegrees(30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(30)
                .setZRotDegrees(30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(-60)
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
