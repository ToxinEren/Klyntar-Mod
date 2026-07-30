PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/spidermilesairpose', 'mymod:spidermanmiles', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:spidermanmiles', 'airpose', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setZ(-3)
                .setY(6.5)
                .setX(-3)
                .setYRotDegrees(25)
                .setXRotDegrees(15)






                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setZ(-3)
                .setY(5)
                .setX(3)
                .setYRotDegrees(-25)
                .setXRotDegrees(15)







                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(90)
                .setXRotDegrees(20)




                .animate('easeInOutCubic', progress);
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(-90)
                .setXRotDegrees(20)







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
