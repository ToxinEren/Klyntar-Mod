PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/spidermilesspinairkick', 'klyntars:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:spidermanmiles', 'spinairkick', builder.getPartialTicks());

        if (abilityUtil.isEnabled(builder.getPlayer(), "klyntars:spidermanmiles", "spinairkick")) {
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
                .setZRotDegrees(20)



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
