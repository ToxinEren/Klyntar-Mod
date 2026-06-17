PalladiumEvents.registerAnimations((event) => {
   event.registerForPower('throgaddon/spiderjumpkick','mymod:spiderman', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spiderman', 'jumpkick', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZRotDegrees(100)




                .animate('easeInOutCubic', progress);
        }
         if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZRotDegrees(30)




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
            builder.get('right_leg')
                .setXRotDegrees(0)


        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(0)
                


        }
       

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(90)

                .animate('easeInOutCubic', progress);
        }
        


    });
});
