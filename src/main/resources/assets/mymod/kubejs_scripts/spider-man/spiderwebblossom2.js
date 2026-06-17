PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spiderwebblossom2', 'mymod:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spiderman', 'webblossomanimation2', builder.getPartialTicks());


        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-90)

                .animate('easeInOutCubic', progress);
        }







    });
});
