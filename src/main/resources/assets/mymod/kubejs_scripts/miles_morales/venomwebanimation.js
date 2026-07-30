PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/milesvenomweb', 'mymod:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spidermanmiles', 'venomwebanimation', builder.getPartialTicks());


        if (abilityUtil.isEnabled(builder.getPlayer(), "mymod:spidermanmiles", "venomweb")) {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-40)
                    .setZRotDegrees(-40)



                    .animate('easeInOutCubic', progress);
            }
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('left_arm')
                    .setXRotDegrees(-40)
                    .setZRotDegrees(40)

                    .animate('easeInOutCubic', progress);
            }

            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_leg')
                    .setXRotDegrees(-40)
                    .animate('easeInOutCubic', progress);
            }
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('left_leg')
                    .setXRotDegrees(-40)
                    .animate('easeInOutCubic', progress);
            }
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('body')
                    .setXRotDegrees(-40)
                    .animate('easeInOutCubic', progress);
            }
        }

    });
});
