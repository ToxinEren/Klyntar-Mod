PalladiumEvents.registerAnimations((event) => {
    event.register('throgaddon/antispinattack', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:antivenom', 'spinattack', builder.getPartialTicks());


        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(-360)
                .animate('easeInOutCubic', progress);
        }

    });
});
