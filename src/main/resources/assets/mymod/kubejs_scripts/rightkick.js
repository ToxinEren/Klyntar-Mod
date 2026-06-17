PalladiumEvents.registerAnimations((event) => {
   event.registerForPower('throgaddon/rightkick','mymod:martialarts', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:martialarts', 'rightkick', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZRotDegrees(100)




                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZRotDegrees(0)
                .setXRotDegrees(0)





        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(70)


                .animate('easeInOutCubic', progress);



        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setXRotDegrees(0)


        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(0)
                


        }
       

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setZRotDegrees(20)
                .setYRotDegrees(90)

                .animate('easeInOutCubic', progress);
        }


    });
});
