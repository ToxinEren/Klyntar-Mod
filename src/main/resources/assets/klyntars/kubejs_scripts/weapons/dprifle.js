PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/rifleaim2', 'klyntars:dp_rifle', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:dp_rifle', 'aim', builder.getPartialTicks());





        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setYRotDegrees(55)
                .setZRotDegrees(-45)
                .animate('InOutCubic', progress)
        }


        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')

                .setZ(-8).multiplier(progress)
                .setX(-8).multiplier(progress)

                .animate('InOutCubic', progress)



        }


    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/rifleaim3rd', 'klyntars:dp_rifle', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:dp_rifle', 'aim', builder.getPartialTicks());


        {

            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-90)
                    .setY(4)
                    .setZ(3)
                    .setYRotDegrees(-25)

                    .animate('InOutCubic', progress)
            }



            if (progress > 0.0 && !builder.isFirstPerson()) {

                builder.get('body')
                    .setYRotDegrees(-25)
                    .animate('InOutCubic', progress)

                    ;
            }
            if (progress > 0.0 && !builder.isFirstPerson()) {

                builder.get('head')
                    .setYRotDegrees(-25)
                    .animate('InOutCubic', progress)

                    ;
            }
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setYRotDegrees(-25)

                .animate('InOutCubic', progress)
        }


        if (progress > 0.0 && !builder.isFirstPerson()) {

            builder.get('left_leg')
                .setYRotDegrees(-25)

                .animate('InOutCubic', progress)

                ;
        }




    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/riflereload', 'klyntars:dp_rifle', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:dp_rifle', 'aim2', builder.getPartialTicks());

        if (progress > 0.0 && builder.isFirstPerson()) {

            builder.get('left_arm')
                .setYRotDegrees(15)
                .setXRotDegrees(-25)

                .animate('InOutCubic', progress)




        }
        if (progress > 0.0 && builder.isFirstPerson()) {

            builder.get('left_arm')
                .setZ(-8).multiplier(progress)
                .setX(-2).multiplier(progress)
                .setY(1).multiplier(progress)
                .setZRotDegrees(45)

                .animate('InOutCubic', progress)


        }

        if (progress > 0.0 && !builder.isFirstPerson()) {

            builder.get('left_arm')
                .setXRotDegrees(-90)
                .setYRotDegrees(15)
                .animate('InOutCubic', progress)


        }


    });
});