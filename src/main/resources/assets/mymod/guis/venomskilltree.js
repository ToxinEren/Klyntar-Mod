
let Minecraft = Java.loadClass("net.minecraft.client.Minecraft");
const RenderSystem = Java.loadClass("com.mojang.blaze3d.systems.RenderSystem");
PalladiumEvents.renderPowerScreen(event => {
    let entity = Minecraft.getInstance().player
    if (!entity) return;
    let height = event.screen.height / 2
    let width = event.screen.width / 2
    if (event.tab.toString().includes("mymod:")) {
        let power = event.tab.toString().replace("mymod:", "")
        let colour = palladium.getProperty(entity, `throgaddon.${power}Colour`)
        let size = 257
        event.guiGraphics.blit(new ResourceLocation(`mymod:textures/gui/skilltrees/${power}/hud${colour}.png`),
            ((width) - 126), ((height) - 100), (0), (0), (size), (size), (size), (size));
    }
}) // Made by ShadowLegacy557
