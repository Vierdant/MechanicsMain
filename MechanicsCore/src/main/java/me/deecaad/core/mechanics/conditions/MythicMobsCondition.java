package me.deecaad.core.mechanics.conditions;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import org.jetbrains.annotations.NotNull;

public class MythicMobsCondition extends Condition {

    private String name;

    /**
     * Default constructor for serializer.
     */
    public MythicMobsCondition() {
    }

    public MythicMobsCondition(String name) {
        this.name = name;
    }

    @Override
    public boolean isAllowed0(CastData cast) {
        if (cast.getTarget() == null) return false;

        ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(cast.getTarget().getUniqueId()).orElse(null);
        return mythicMob != null && mythicMob.getName().equals(name);
    }

    @Override
    public String getKeyword() {
        return "MythicMobsEntity";
    }

    @NotNull
    @Override
    public Condition serialize(SerializeData data) throws SerializerException {
        String type = data.of("Entity").assertExists().get();

        return applyParentArgs(data, new MythicMobsCondition(type));
    }
}
