//Made originally by phantompig, but converted to use Scoreboards by FSang18, and tweeked a lil by BulbulFrog

let $Util = Java.loadClass('net.minecraft.Util');

StartupEvents.registry('palladium:abilities', event => {
    event.create('mymod:grab')
        .icon(palladium.createItemIcon('minecraft:player_head'))

        .documentationDescription('Grab: grab entities and float them in front of you. Big shoutout to phantompig and Fsang18 for this')
        .addProperty('range_objective', 'string', 'FSang.Range', 'Scoreboard objective that defines telekinesis range.')
        .addProperty('strength', 'float', 0.8, 'The strength/speed of the telekinesis.')
        .addProperty('damage', 'float', 2, 'The damage to apply when hitting an entity with a block via telekinesis.')

        .addUniqueProperty('held_entity', 'uuid', $Util.NIL_UUID)

        .firstTick((entity, entry, holder, enabled) => {
            if (!enabled) return;

            const range = getRangeFromScore(entity, entry);

            let rayTrace = entity.rayTrace(range, false);

            if (rayTrace.entity != null) {
                entry.setUniquePropertyByName('held_entity', rayTrace.entity.uuid);
            }
        })

        .tick((entity, entry, holder, enabled) => {
            if (!enabled) return;

            const range = getRangeFromScore(entity, entry);

            let heldEntity = getHeldEntity(entity.level, entry);
            if (heldEntity == null) return;

            let targetPos = entity.getEyePosition().add(entity.getLookAngle().scale(range));
            let boundingBox = entity.getBoundingBox();

            if (heldEntity.type == 'minecraft:block_display') {
                boundingBox = AABB.ofSize(heldEntity.position(), 1, 1, 1);
                targetPos = entity.rayTrace(range).hit ?? targetPos;
                heldEntity.setPosition(targetPos.x() - 0.5, targetPos.y() - 0.5, targetPos.z() - 0.5);

                entity.level.getEntities(heldEntity, boundingBox).forEach(collidedEntity => {
                    if (collidedEntity.living) collidedEntity.attack(entry.getPropertyByName('damage'));
                });
            } else {
                heldEntity.setDeltaMovement(
                    targetPos.subtract(heldEntity.getEyePosition()).scale(entry.getPropertyByName('strength'))
                );
                heldEntity.resetFallDistance();
            }

            entity.level.sendParticles(
                'minecraft:enchant',
                heldEntity.x, heldEntity.y, heldEntity.z,
                1,
                boundingBox.getXsize(), boundingBox.getYsize(), boundingBox.getZsize(),
                0
            );
        })

        .lastTick((entity, entry, holder, enabled) => {
            let heldEntity = getHeldEntity(entity.level, entry);
            if (heldEntity == null) return;

            if (heldEntity.type == 'minecraft:block_display') blockDisplayToBlock(heldEntity);

            entry.setUniquePropertyByName('held_entity', $Util.NIL_UUID);
        });
});

function getRangeFromScore(entity, entry) {
    const objectiveName = entry.getPropertyByName('range_objective');
    let range = palladium.scoreboard.getScore(entity, objectiveName, 0);
    if (range <= 0) range = 1;
    return range;
}

function getHeldEntity(level, entry) {
    let uuid = entry.getPropertyByName('held_entity');
    if (uuid == $Util.NIL_UUID) return;
    return getEntity(level, uuid);
}
function getEntity(level, uuid) {
    if (uuid == null || uuid == $Util.NIL_UUID) return null;
    let entities = level.getEntities();
    for (let i = 0; i < entities.size(); i++) {
        if (entities.get(i).uuid.equals(uuid)) {
            return entities.get(i);
        }
    }
}

function spawnFallingBlock(block) {
    let entity = block.level.createEntity('minecraft:block_display');
    entity.setPosition(block);
    entity.mergeNbt({ block_state: { Name: block.id, Properties: block.properties } });
    block.set('air');
    entity.spawn();
    return entity;
}
function blockDisplayToBlock(entity) {
    if (entity.type != 'minecraft:block_display') return;
    let block = entity.block;

    if (block.id == 'minecraft:bedrock') block = block.up;
    block.level.destroyBlock(block.pos, true);

    let props = {};
    if (entity.nbt.block_state.Properties != null) {
        for (let [key, value] of Object.entries(entity.nbt.block_state.Properties)) {
            props[key] = value;
        }
    }

    block.set(entity.nbt.block_state.Name, props);
    block.level.markAndNotifyBlock(
        block.pos,
        block.level.getChunk(block.pos),
        block.blockState,
        block.blockState,
        3,
        512
    );
    entity.discard();
}
