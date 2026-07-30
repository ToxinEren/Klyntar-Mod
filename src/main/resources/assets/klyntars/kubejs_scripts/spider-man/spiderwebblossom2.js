PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/spiderwebblossom2', 'klyntars:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:spiderman', 'webblossomanimation2', builder.getPartialTicks());


        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-90)

                .animate('easeInOutCubic', progress);
        }







    });
});
