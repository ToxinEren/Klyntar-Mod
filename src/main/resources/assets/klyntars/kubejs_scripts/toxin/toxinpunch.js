// Il pugno simbionte per toxin: stesse pose di quello di Venom.
// Serve un file per potere perche' registerForPower vuole l'id scritto dentro,
// ed e' il motivo per cui l'animazione non partiva su questa forma.
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/toxinpunch', 'klyntars:toxin', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:toxin', 'venompunch1', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-90)
                .setZ(6).multiplier(progress)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(30)
                .setZRotDegrees(-30)

                .animate('easeInOutCubic', progress);
        }

        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setXRotDegrees(15)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setXRotDegrees(-30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(-30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')

                .setZ(10).multiplier(progress)

                .animate('easeInOutCubic', progress);
        }

    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('klyntar/toxinpunch2', 'klyntars:toxin', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:toxin', 'venompunch2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')

                .setZ(-5)
                .setXRotDegrees(-90)



                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(30)
                .setZRotDegrees(-30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_leg')
                .setXRotDegrees(15)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_leg')
                .setXRotDegrees(-20)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('body')
                .setYRotDegrees(5)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZ(-2).multiplier(progress)
                .setYRotDegrees(-20)
                .setZRotDegrees(-30)
                .setXRotDegrees(-10)
                .animate('easeInOutCubic', progress);
        }

    });
});
