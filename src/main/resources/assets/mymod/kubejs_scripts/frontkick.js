PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/frontkick', 'mymod:martialarts', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:martialarts', 'frontkick', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setXRotDegrees(-100)






                .animate('easeInOutCubic', progress);
        }
          if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setXRotDegrees(15)






                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(30)
                .setXRotDegrees(15)





                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(-30)
                .setXRotDegrees(15)







                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZRotDegrees(0)
                .setXRotDegrees(0)





        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')




        }




    });
});
