StartupEvents.registry('palladium:condition_serializer', (event) => {
    event.create('mymod:disable_if_have_power_or_namespace')
        .addProperty('powers', 'string_array', [], 'A list of specific superpower IDs to check for.')
        .addProperty('namespaces', 'string_array', [], 'A list of power namespaces to check for (e.g., "endosym").')
        .addProperty('ignore_powers', 'string_array', [], 'A list of superpower IDs to ignore during the check.')
        .test((entity, props) => {
            if (!props) return false;
            const powerList = props.get("powers");
            const namespaceList = props.get("namespaces");
            const ignoreList = props.get("ignore_powers");
            for (const powerId of powerList) {
                if (palladium.superpowers.hasSuperpower(entity, powerId)) {
                    return false;
                }
            }
            if (namespaceList.length === 0) {
                return true; 
            }
            for (const ns of namespaceList) {
                if (String(ns || '').trim() === '') continue; 
                let powerIds = palladium.powers.getPowerIdsForNamespace(entity, ns).map(p => p.toString());
                if (powerIds.length === 0) {
                    continue;
                }
                for (const id of powerIds) {
                    if (!ignoreList.includes(id)) {
                        return false;
                    }
                }
            }
            return true;
        });
});