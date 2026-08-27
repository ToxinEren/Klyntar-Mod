PalladiumEvents.registerAnimations((event) => {
     event.registerForPower('klyntar/spideyspinattack','klyntars:venomspidey', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:venomspidey', 'spinattack', builder.getPartialTicks());


        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(-360)
                .animate('easeInOutCubic', progress);
        }

    });
});
