PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/toxinclimb', 5, (builder) => {
        if (abilityUtil.isEnabled(builder.getPlayer(), "klyntars:toxin", "climb_hanging")) {
            if (!builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-105)

                builder.get('left_arm')
                    .setXRotDegrees(-105)

                builder.get('right_leg')
                    .setXRotDegrees(-10)

                builder.get('left_leg')
                    .setXRotDegrees(-10)
            }
            if (builder.isFirstPerson()) {
            }
        };
        if (abilityUtil.isEnabled(builder.getPlayer(), "klyntars:toxin", "climb_ceiling")) {
            if (!builder.isFirstPerson()) {
                builder.get('head')
                    .setXRotDegrees(-55)
                    .setYRotDegrees(0)
                    .setZRotDegrees(0)

                builder.get('body')
                    .setXRotDegrees(-90)
                    .setZRotDegrees(180)
                    .moveY(22)
                    .moveZ(15)

                builder.get('right_leg')
                    .setXRot(-0.4)

                builder.get('left_leg')
                    .setXRot(-0.4)
            }
            if (builder.isFirstPerson()) {
            }
        };
        if (abilityUtil.isEnabled(builder.getPlayer(), "klyntars:toxin", "climb_ceiling_hold")) {
            if (!builder.isFirstPerson()) {
                builder.get('head')
                    .setXRotDegrees(-55)
                    .setYRotDegrees(0)
                    .setZRotDegrees(0)

                builder.get('body')
                    .setXRotDegrees(-90)
                    .setZRotDegrees(180)
                    .moveY(22)
                    .moveZ(15)

                builder.get('right_leg')
                    .setXRot(-0.4)

                builder.get('left_leg')
                    .setXRot(-0.4)
            }
            if (builder.isFirstPerson()) {
            }
        };
    });
});

PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/toxinclimb1', 10, (builder) => {

        let progress_climb1 = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:toxin', 'climb_1', builder.getPartialTicks());
        if (progress_climb1 > 0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-105 + 15)
                .animate('easeInOutSine', progress_climb1);

            builder.get('left_arm')
                .setXRotDegrees(-105 - 15)
                .animate('easeInOutSine', progress_climb1);

            builder.get('right_leg')
                .setXRotDegrees(-10 - 20)
                .animate('easeInOutSine', progress_climb1);

            builder.get('left_leg')
                .setXRotDegrees(-10 + 20)
                .animate('easeInOutSine', progress_climb1);
        }
        if (progress_climb1 > 0.0 && builder.isFirstPerson()) {
        }
    });
});

PalladiumEvents.registerAnimations((event) => {
    event.register('klyntar/toxinclimb2', 10, (builder) => {

        let progress_climb2 = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'klyntars:toxin', 'climb_2', builder.getPartialTicks());
        if (progress_climb2 > 0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-105 - 15)
                .animate('easeInOutSine', progress_climb2);

            builder.get('left_arm')
                .setXRotDegrees(-105 + 15)
                .animate('easeInOutSine', progress_climb2);

            builder.get('right_leg')
                .setXRotDegrees(-10 + 20)
                .animate('easeInOutSine', progress_climb2);

            builder.get('left_leg')
                .setXRotDegrees(-10 - 20)
                .animate('easeInOutSine', progress_climb2);
        }
        if (progress_climb2 > 0.0 && builder.isFirstPerson()) {
        }
    });
});
