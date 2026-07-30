PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/spiderwebblossom', 'klyntars:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:spiderman', 'webblossomanimation', builder.getPartialTicks());

        if (abilityUtil.isEnabled(builder.getPlayer(), "klyntars:spiderman", "webblossomanimation")) {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('body')

                    .setYRotDegrees(-360)




                    .animate('easeInOutCubic', progress);
            }
        }







    });
});
