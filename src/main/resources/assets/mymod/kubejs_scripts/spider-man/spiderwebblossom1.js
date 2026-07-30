PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/spiderwebblossom', 'mymod:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spiderman', 'webblossomanimation', builder.getPartialTicks());

        if (abilityUtil.isEnabled(builder.getPlayer(), "mymod:spiderman", "webblossomanimation")) {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('body')

                    .setYRotDegrees(-360)




                    .animate('easeInOutCubic', progress);
            }
        }







    });
});
