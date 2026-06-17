PalladiumEvents.registerAnimations((event) => {
   event.registerForPower('throgaddon/flyingknee','mymod:martialarts', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:martialarts', 'flyingknee', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZ(-2)
                .setY(3.5)






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
                .setXRotDegrees(0)
                .setZRotDegrees(0)



        }
  



    });
});
