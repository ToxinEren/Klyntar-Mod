PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomblock', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venomblock', builder.getPartialTicks());


        {
            if (progress > 0.0 && !builder.isFirstPerson()) {
                builder.get('right_arm')
                    .setXRotDegrees(-70)
                    .setYRotDegrees(-15)
                    .setZRotDegrees(20)
                    .setX(-7).multiplier(progress)
                    .animate('InOutCubic', progress)
                builder.get('left_arm')
                    .setXRotDegrees(20)
                    .setYRotDegrees(-20)
                    .setZRotDegrees(-20)
                    .setX(7).multiplier(progress)

                    .animate('InOutCubic', progress)
                    ;
            }
        }
    });
});
PalladiumEvents.registerAnimations((event) => {
    event.register('throgaddon/venomclimb', 5, (builder) => {
        if (abilityUtil.isEnabled(builder.getPlayer(), "mymod:venom", "climb_hanging")) {
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
        if (abilityUtil.isEnabled(builder.getPlayer(), "mymod:venom", "climb_ceiling")) {
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
        if (abilityUtil.isEnabled(builder.getPlayer(), "mymod:venom", "climb_ceiling_hold")) {
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
    event.register('throgaddon/venomclimb1', 10, (builder) => {

        let progress_climb1 = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'climb_1', builder.getPartialTicks());
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
    event.register('throgaddon/venomclimb2', 10, (builder) => {

        let progress_climb2 = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'climb_2', builder.getPartialTicks());
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
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomcrush', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venomcrush', builder.getPartialTicks());

        if (builder.isFirstPerson()) {
            if (progress > 0.0) {

                ;
            }
        }
        else {
            if (progress > 0.0) {
                builder.get('left_arm')
                    .setXRotDegrees(-90)
                    .setYRotDegrees(35)
                    .animate('easeInOutCubic', progress);


                ;
            }
        }
    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomgrab', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venomgrabani', builder.getPartialTicks());

        if (builder.isFirstPerson()) {
            if (progress > 0.0) {

                ;
            }
        }
        else {
            if (progress > 0.0) {
                builder.get('right_arm')
                    .setXRotDegrees(-90)
                    .setYRotDegrees(-35)
                    .animate('easeInOutCubic', progress);


                ;
            }
        }
    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomslam', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venomslam', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-150)
                .scaleZ(1.5)
                .scaleX(1.5)
                .scaleY(1.5)

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
                .setZRotDegrees(40)
                .scaleZ(1.3)
                .scaleX(1.3)
                .scaleY(1.3)

                .animate('easeInOutCubic', progress);
        }
    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomslam2', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venomslam2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-70)
                .scaleZ(1.5)
                .scaleX(1.5)
                .scaleY(1.5)


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
                .setYRotDegrees(30)
                .setXRotDegrees(-30)
                .animate('easeInOutCubic', progress)
                .animate('easeInOutCubic', progress)

                ;
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(-70)
                .scaleZ(1.3)
                .scaleX(1.3)
                .scaleY(1.3)
                .animate('easeInOutCubic', progress);
        }

    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomsmash', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venomsmash', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-150)
                .scaleZ(1.5)
                .scaleX(1.5)
                .scaleY(1.5)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-150)
                .scaleZ(1.5)
                .scaleX(1.5)
                .scaleY(1.5)
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
                .setXRotDegrees(30)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(40)
                .scaleZ(1.3)
                .scaleX(1.3)
                .scaleY(1.3)

                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(-40)
                .scaleZ(1.3)
                .scaleX(1.3)
                .scaleY(1.3)

                .animate('easeInOutCubic', progress);
        }
    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venomsmash2', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venomsmash2', builder.getPartialTicks());



        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('right_arm')
                .setXRotDegrees(-70)
                .scaleZ(1.5)
                .scaleX(1.5)
                .scaleY(1.5)


                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && !builder.isFirstPerson()) {
            builder.get('left_arm')
                .setXRotDegrees(-70)
                .scaleZ(1.5)
                .scaleX(1.5)
                .scaleY(1.5)
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
                .setXRotDegrees(-30)
                .animate('easeInOutCubic', progress)
                .animate('easeInOutCubic', progress)

                ;
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('right_arm')
                .setZRotDegrees(-70)
                .scaleZ(1.3)
                .scaleX(1.3)
                .scaleY(1.3)
                .animate('easeInOutCubic', progress);
        }
        if (progress > 0.0 && builder.isFirstPerson()) {
            builder.get('left_arm')
                .setZRotDegrees(70)
                .scaleZ(1.3)
                .scaleX(1.3)
                .scaleY(1.3)
                .animate('easeInOutCubic', progress);
        }

    });
});
PalladiumEvents.registerAnimations((event) => {
    event.registerForPower('throgaddon/venompunch', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venompunch1', builder.getPartialTicks());



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
    event.registerForPower('throgaddon/venompunch2', 'mymod:venom', 10, (builder) => {
        // animation part
        const progress = animationUtil.getAnimationTimerAbilityValue(builder.getPlayer(), 'mymod:venom', 'venompunch2', builder.getPartialTicks());



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
