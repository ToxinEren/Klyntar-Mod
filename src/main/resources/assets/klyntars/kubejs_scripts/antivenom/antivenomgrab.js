PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/antivenomgrab', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:antivenom', 'venomgrabani', builder.getPartialTicks());

        if (builder.isFirstPerson()) {
            if (progress > 0.0) {

                ;
            }
        }
        else {
            if (progress > 0.0) {
                builder.get('right_arm')
                    .setXRotDegrees(-90)
                    .setYRotDegrees(-35)
                    .animate('easeInOutCubic', progress);


                ;
            }
        }
    });
});