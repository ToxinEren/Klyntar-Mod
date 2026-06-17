
//made by Dreyse(discord:AkumaDreyse)
StartupEvents.registry('palladium:condition_serializer', (event) => {
    event.create('mymod:is_first_person')
        .test((entity) => {
            if (!entity.isPlayer()) return false;

            if (!entity.persistentData) return false;

            if (!("firstPersonMode" in entity.persistentData)) return false;

            let isFirstPerson = Boolean(entity.persistentData.firstPersonMode);

            return isFirstPerson;
        });
});

