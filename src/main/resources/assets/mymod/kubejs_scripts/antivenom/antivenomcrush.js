PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/antivenomcrush', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:antivenom', 'venomcrush', builder.getPartialTicks());

        if (builder.isFirstPerson()) {
            if (progress > 0.0) {

                ;
            }
        }
        else {
            if (progress > 0.0) {
                builder.get('left_arm')
                    .setXRotDegrees(-90)
                    .setYRotDegrees(35)
                    .animate('easeInOutCubic', progress);


                ;
            }
        }
    });
});