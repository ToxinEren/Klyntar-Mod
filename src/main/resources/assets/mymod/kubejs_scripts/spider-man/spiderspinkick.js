PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spiderspinkick', 'mymod:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spiderman', 'spinkick', builder.getPartialTicks());

        if (abilityUtil.isEnabled(builder.getPlayer(), "mymod:spiderman", "spinkick")) {

            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('body')

                    .setYRotDegrees(-360)




                    .animate('easeInOutCubic', progress);
            }
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZRotDegrees(120)



                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZRotDegrees(-15)



                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')

                .setXRotDegrees(0)





        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')

                .setZRotDegrees(90)
                .animate('easeInOutCubic', progress);

        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')

                .setZRotDegrees(-15)
                .animate('easeInOutCubic', progress);

        }




    });
});
