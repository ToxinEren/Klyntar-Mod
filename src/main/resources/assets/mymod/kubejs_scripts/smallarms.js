PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/smallarms', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'smallarms', builder.getPartialTicks());



        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .scaleY(0.8)
                .scaleX(0.8)
                .scaleZ(0.8)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .scaleY(0.8)
                .scaleX(0.8)
                .scaleZ(0.8)
                .animate('easeInOutCubic', progress);
        }

    });
});
