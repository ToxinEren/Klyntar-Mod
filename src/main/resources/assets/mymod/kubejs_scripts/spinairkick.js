PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spinairkick', 'mymod:martialarts', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:martialarts', 'spinairkick', builder.getPartialTicks());

        if (abilityUtil.isEnabled(builder.getPlayer(), "mymod:martialarts", "spinairkick")) {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('body')

                    .setYRotDegrees(-360)




                    .animate('easeInOutCubic', progress);
            }
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZRotDegrees(100)



                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZRotDegrees(-70)



                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')

                .setXRotDegrees(0)





        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')

                .setZRotDegrees(75)
                .animate('easeInOutCubic', progress);

        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')

                .setZRotDegrees(-40)
                .animate('easeInOutCubic', progress);

        }





    });
});
