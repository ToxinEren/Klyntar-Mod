PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomspiderjump', 'mymod:venomspidey', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venomspidey', 'jump', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')

                .setXRotDegrees(25)






                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')

                .setXRotDegrees(15)







                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(15)
                .setXRotDegrees(20)




                .animate('easeInOutCubic', progress);
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(-15)
                .setXRotDegrees(20)







                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setXRotDegrees(10)







                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')







        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')





        }

    });
});
