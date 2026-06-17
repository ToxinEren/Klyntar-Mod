PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/dpkatanasaim', 'mymod:dp_katanas', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:dp_katanas', 'firstperson', builder.getPartialTicks());





        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setYRotDegrees(55)
                .setZRotDegrees(-15)
                .animate('InOutCubic', progress)
        }

        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .setYRotDegrees(-55)
                .setZRotDegrees(15)

                .animate('InOutCubic', progress)


        }


    });
});