function getBlockInFront(entity, level) {
  let dir = entity.getHorizontalFacing();
  let pos = entity.blockPosition();
  switch (dir) {
    case 'north': return level.getBlock(pos.north());
    case 'south': return level.getBlock(pos.south());
    case 'east': return level.getBlock(pos.east());
    case 'west': return level.getBlock(pos.west());
  }
};

function getBlockBehind(entity, level) {
  let dir = entity.getHorizontalFacing();
  let pos = entity.blockPosition();
  switch (dir) {
    case 'north': return level.getBlock(pos.south());
    case 'south': return level.getBlock(pos.north());
    case 'east': return level.getBlock(pos.west());
    case 'west': return level.getBlock(pos.east());
  }
};


function getBlockRight(entity, level) {
  let dir = entity.getHorizontalFacing();
  let pos = entity.blockPosition();
  switch (dir) {
    case 'north': return level.getBlock(pos.west());
    case 'south': return level.getBlock(pos.east());
    case 'east': return level.getBlock(pos.north());
    case 'west': return level.getBlock(pos.south());
  }
};

function getBlockLeft(entity, level) {
  let dir = entity.getHorizontalFacing();
  let pos = entity.blockPosition();
  switch (dir) {
    case 'north': return level.getBlock(pos.east());
    case 'south': return level.getBlock(pos.west());
    case 'east': return level.getBlock(pos.south());
    case 'west': return level.getBlock(pos.north());
  }
};

function isCloseToBlock(entity, block, max) {
  let dx = entity.x - block.x;
  let dy = entity.y - block.y;
  let dz = entity.z - block.z;
  return Math.sqrt(dx * dx + dy * dy + dz * dz) < max;
};

let ClientboundSetEntityMotionPacket = Java.loadClass('net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket');
let jumped = false;

StartupEvents.registry('palladium:abilities', (event) => {
  event.create('klyntars:climb')
    .icon(palladium.createItemIcon('minecraft:ladder'))
    .addProperty('climb_speed', 'float', 0.2, 'climbing speed')
    .addProperty('block_distance', 'float', 1.7, 'distance of block')
    .addProperty('data', 'string', 'oms.climb', 'persistent data name')
    .lastTick((entity, entry, holder, enabled) => {
      if (enabled && entity.isPlayer()) {
        let data_name = entry.getPropertyByName('data');
        entity.persistentData[data_name] = '';
        entity.setNoGravity(false);
      }
    })

    .tick((entity, entry, holder, enabled) => {
      if (enabled && entity.isPlayer()) {
        let climb_speed = entry.getPropertyByName('climb_speed');
        let block_dist = entry.getPropertyByName('block_distance');
        let data_name = entry.getPropertyByName('data');

        let x = entity.x;
        let y = Math.floor(entity.y);
        let z = entity.z;

        let blockFront = getBlockInFront(entity, entity.level);
        let blockBehind = getBlockBehind(entity, entity.level);
        let blockRight = getBlockRight(entity, entity.level);
        let blockLeft = getBlockLeft(entity, entity.level);
        let blockAbove = entity.level.getBlock(x, (y + entity.getEyeHeight() + 1), z);
        let blockBelow = entity.level.getBlock(x, y - 0.1, z);

        let isBlockFrontSolid = blockFront && !blockFront.hasTag('klyntars:block_pass') && isCloseToBlock(entity, blockFront, block_dist);
        let isBlockBehindSolid = blockBehind && !blockBehind.hasTag('klyntars:block_pass') && isCloseToBlock(entity, blockBehind, block_dist);
        let isBlockRightSolid = blockRight && !blockRight.hasTag('klyntars:block_pass') && isCloseToBlock(entity, blockRight, block_dist);
        let isBlockLeftSolid = blockLeft && !blockLeft.hasTag('klyntars:block_pass') && isCloseToBlock(entity, blockLeft, block_dist);
        let isBlockAboveSolid = blockAbove && !blockAbove.hasTag('klyntars:block_pass') && isCloseToBlock(entity, blockAbove, 2.5);
        let isBlockBelowSolid = blockBelow && !blockBelow.hasTag('klyntars:block_pass');

        let isNearWall = isBlockFrontSolid || isBlockBehindSolid || isBlockRightSolid || isBlockLeftSolid;

        if (isNearWall || isBlockAboveSolid) {
          let motion = new Vec3d(0, 0, 0);
          let lookAngle = entity.getLookAngle().scale(climb_speed);
          let forward_key = palladium.getProperty(entity, 'forward_key_down') == true;
          let backwards_key = palladium.getProperty(entity, 'backwards_key_down') === true;
          let right_key = palladium.getProperty(entity, 'right_key_down') === true;
          let left_key = palladium.getProperty(entity, 'left_key_down') === true;
          let isDown = palladium.scoreboard.getScore(entity, 'oms.player.jump_key', 0) === 1;
          let jumping = isDown && !jumped;
          jumped = isDown;

          let horizontalLook = new Vec3d(lookAngle.x(), 0, lookAngle.z()).normalize();
          let strafe = new Vec3d(-horizontalLook.z(), 0, horizontalLook.x()).normalize();

          if (jumping && isNearWall) {
            let jumpAngle = entity.getLookAngle().scale(1.1);
            motion = motion.add(jumpAngle.x(), jumpAngle.y(), jumpAngle.z());
          };

          if (isBlockFrontSolid) {
            entity.persistentData[data_name] = 'hanging';
            if (forward_key || backwards_key || right_key || left_key) {
              entity.persistentData[data_name] = 'hanging_move';
              if (forward_key) {
                motion = motion.add(0, climb_speed, 0);
              };
              if (backwards_key && !isBlockBelowSolid) {
                motion = motion.add(lookAngle.x(), -climb_speed, lookAngle.z());
              };
              if (right_key) {
                motion = motion.add(strafe.scale(climb_speed / 2));
              }
              if (left_key) {
                motion = motion.add(strafe.scale(-climb_speed / 2));
              }
            } else {
              motion = motion.add(0, 0, 0);
            }
          } else {
            motion = motion.add(0, 0, 0);
          };
          if (isBlockAboveSolid && !entity.onGround()) {
            entity.persistentData[data_name] = 'ceiling';
            if (forward_key || backwards_key || right_key || left_key) {
              if (forward_key) {
                motion = motion.add(lookAngle.x(), 0.02, lookAngle.z());
              };
              if (backwards_key) {
                motion = motion.add(-lookAngle.x(), 0.02, -lookAngle.z());
              };
              if (right_key) {
                motion = motion.add(strafe.scale(climb_speed / 2));
                motion = motion.add(0, 0.02, 0);
              }
              if (left_key) {
                motion = motion.add(strafe.scale(-climb_speed / 2));
                motion = motion.add(0, 0.02, 0);
              }
            } else {
              motion = motion.add(0, 0.05, 0);
            }
          };

          entity.setMotion(motion.x(), motion.y(), motion.z());
          entity.setNoGravity(true);
          entity.fallDistance = 0.0;
          entity.connection.send(new ClientboundSetEntityMotionPacket(entity));
        } else {
          entity.persistentData[data_name] = '';
          entity.setNoGravity(false);
        }
      }
    });
});

StartupEvents.registry('palladium:condition_serializer', (event) => {
  event.create('klyntars:is_climb_stage')
    .addProperty('data', 'string', 'oms.climb', 'persistent data name')
    .addProperty('stage', 'string', 'hanging', 'hanging | hanging_move | ceiling')
    .test((entity, props) => {
      let data_name = props.get('data')
      let stage = props.get('stage')
      if (entity.persistentData[data_name] == `${stage}`) {
        return true;
      } else {
        return false;
      }
    })
});