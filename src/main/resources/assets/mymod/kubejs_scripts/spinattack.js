PalladiumEvents.registerAnimations((event) => {
     event.registerForPower('klyntar/spinattack','mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'spinattack', builder.getPartialTicks());


        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(-360)
                .animate('easeInOutCubic', progress);
        }

    });
});
