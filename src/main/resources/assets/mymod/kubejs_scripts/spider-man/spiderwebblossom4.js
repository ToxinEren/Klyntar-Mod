PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spiderwebblossom4', 'mymod:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spiderman', 'webblossomanimation4', builder.getPartialTicks());

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZRotDegrees(30)






                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZ(-3)
                .setY(8)
                .setX(3)
                .setYRotDegrees(-25)
                .setXRotDegrees(15)







                .animate('easeInOutCubic', progress);
        }





    });
});
