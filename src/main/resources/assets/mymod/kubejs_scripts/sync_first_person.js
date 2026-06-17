
let lastFirstPersonMode = null;

ClientEvents.tick(event => {
    let player = event.player;
    if (!player) return;

    let isFirstPerson = Client.options.getCameraType() === "FIRST_PERSON";

    // Only send update if it changed (to reduce unnecessary event spam)
    if (isFirstPerson !== lastFirstPersonMode) {
        lastFirstPersonMode = isFirstPerson;
        // Send the data to the server
        player.sendData("mymod:sync_first_person", { firstPersonMode: isFirstPerson });
    }
});
