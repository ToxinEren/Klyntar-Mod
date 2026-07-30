PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/spiderwebblossom3', 'klyntars:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:spiderman', 'webblossomanimation3', builder.getPartialTicks());

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-90)

                .animate('easeInOutCubic', progress);
        }





    });
});
