PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/roar','mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'roar', builder.getPartialTicks());

        if (builder.isFirstPerson()) {
            if (progress > 0.0) {

                ;
            }
        }
        else {
            if (progress > 0.0) {
                builder.get('right_arm')
                    .setXRotDegrees(-45)
                    .setYRotDegrees(45)
                    .animate('easeInOutCubic', progress)
                builder.get('left_arm')
                    .setXRotDegrees(-45)
                    .setYRotDegrees(-45)
                    .animate('easeInOutCubic', progress)
                builder.get('head')
                    .setXRotDegrees(-50)
                    .animate('easeInOutCubic', progress);


                ;
            }
        }
    });
});