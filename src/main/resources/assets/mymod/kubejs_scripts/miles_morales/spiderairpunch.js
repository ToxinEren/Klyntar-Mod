PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/spidermilesairpunch', 'mymod:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spidermanmiles', 'airpunch', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZ(-2)
                .setY(3.5)






                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-115)





                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setXRotDegrees(-25)
                .setYRotDegrees(25)





                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(30)







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
