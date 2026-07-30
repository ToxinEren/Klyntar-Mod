PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/rewind', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:martialarts', 'rewind', builder.getPartialTicks());


        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')

                .setYRotDegrees(0)




                .multiplier(progress);
        }


    });
});
