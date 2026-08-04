PalladiumEvents.registerAnimations((event) => {
 event.registerForPower('klyntar/righttalon2','klyntars:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:venom', 'righttalon2', builder.getPartialTicks());
        // le gambe restano libere se il player si muove, cosi' cammina mentre attacca
        const legs = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:venom', 'barragelegs', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-120)
                .setZRotDegrees(-60)


                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-35)
                .setZRotDegrees(-30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setXRotDegrees(15)
                .animate('easeInOutCubic', progress * legs);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setXRotDegrees(-20)
                .animate('easeInOutCubic', progress * legs);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(70)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(-50)
                .animate('easeInOutCubic', progress);
        }

    });
});
