PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomspiderdashmiles', 'mymod:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spidermanmiles', 'dash', builder.getPartialTicks());



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
                .setZRotDegrees(30)
                .setXRotDegrees(90)




                .animate('easeInOutCubic', progress);
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(-30)
                .setXRotDegrees(90)







                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setXRotDegrees(-15)







                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')







        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')





        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')

                .setY(-7).multiplier(progress)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('left_arm')

                .setY(-7).multiplier(progress)

                .animate('easeInOutCubic', progress);
        }


    });
});
