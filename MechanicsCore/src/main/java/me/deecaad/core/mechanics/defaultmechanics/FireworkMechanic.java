package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.entity.FakeEntity;
import me.deecaad.core.file.*;
import me.deecaad.core.file.serializers.ColorSerializer;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.utils.DistanceUtil;
import me.deecaad.core.utils.ReflectionUtil;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FireworkMechanic extends Mechanic {

    public static class FireworkData implements InlineSerializer<FireworkData> {

        private FireworkEffect effect;

        /**
         * Default constructor for serializer
         */
        public FireworkData() {
        }

        public FireworkData(FireworkEffect effect) {
            this.effect = effect;
        }

        public FireworkEffect getEffect() {
            return effect;
        }

        @Override
        public String getKeyword() {
            return "Firework";
        }

        @NotNull
        @Override
        public FireworkData serialize(SerializeData data) throws SerializerException {
            FireworkEffect.Type type = data.of("Shape").getEnum(FireworkEffect.Type.class, FireworkEffect.Type.BURST);
            boolean trail = data.of("Trail").getBool(false);
            boolean flicker = data.of("Flicker").getBool(false);

            // We allow either one color format '#ffffff' or multi color
            // format '[#ffffff, #ffffff]'
            List<Color> colors = new ArrayList<>();
            List<Color> fadeColors = new ArrayList<>();

            try {
                List<MapConfigLike.Holder> temp = data.of("Color").assertExists().assertType(List.class).get();
                for (MapConfigLike.Holder holder : temp)
                    colors.add(ColorSerializer.fromString(data.move("Color"), holder.value().toString()));

            } catch (SerializerTypeException ex) {
                ColorSerializer color = data.of("Color").serialize(new ColorSerializer());
                if (color == null)
                    throw data.exception("Color", "Could not determine 'color'", "Try using 'color=RED' or 'color=[RED, GREEN]'");

                // Using List.of() saves space vs. ArrayList
                colors = List.of(color.getColor());
            }

            try {
                List<MapConfigLike.Holder> temp = data.of("Fade_Color").assertType(List.class).get(List.of());
                for (MapConfigLike.Holder holder : temp)
                    fadeColors.add(ColorSerializer.fromString(data.move("Fade_Color"), holder.value().toString()));

            } catch (SerializerTypeException ex) {
                ColorSerializer color = data.of("Fade_Color").serialize(new ColorSerializer());
                if (color != null)
                    fadeColors = List.of(color.getColor());
            }

            FireworkEffect effect = FireworkEffect.builder().with(type).withColor(colors).trail(trail).flicker(flicker).withFade(fadeColors).build();
            return new FireworkData(effect);
        }
    }

    private ItemStack fireworkItem;
    private int flightTime; // THIS IS A COPY OF THE VALUE IN 'fireworkItem'
    private Targeter viewers;
    private List<Condition> viewerConditions;

    /**
     * Default constructor for serializer.
     */
    public FireworkMechanic() {
    }

    public FireworkMechanic(ItemStack fireworkItem, int flightTime, Targeter viewers, List<Condition> viewerConditions) {
        this.fireworkItem = fireworkItem;
        this.flightTime = flightTime;
        this.viewers = viewers;
        this.viewerConditions = viewerConditions;
    }

    public ItemStack getFireworkItem() {
        return fireworkItem;
    }

    public int getFlightTime() {
        return flightTime;
    }

    public Targeter getViewers() {
        return viewers;
    }

    public List<Condition> getViewerConditions() {
        return viewerConditions;
    }

    @Override
    public void use0(CastData cast) {

        // Determine which players will see the firework
        List<Player> players;
        if (viewers == null)
            players = DistanceUtil.getPlayersInRange(cast.getTargetLocation());
        else {
            players = new LinkedList<>();
            OUTER:
            for (CastData target : viewers.getTargets(cast)) {
                if (!(target.getTarget() instanceof Player player))
                    continue;

                for (Condition condition : viewerConditions)
                    if (!condition.isAllowed(target))
                        continue OUTER;

                players.add(player);
            }
        }

        // No need to generate a fake entity if nobody is going to see it.
        if (players.isEmpty()) {
            return;
        }

        FakeEntity fakeEntity = CompatibilityAPI.getCompatibility().getEntityCompatibility().generateFakeEntity(cast.getTargetLocation(), EntityType.FIREWORK, fireworkItem);
        if (flightTime > 1) fakeEntity.setMotion(0.001, 0.3, -0.001);
        fakeEntity.show();

        // If we need to explode the firework instantly, make sure to return
        if (flightTime <= 0) {
            fakeEntity.playEffect(EntityEffect.FIREWORK_EXPLODE);
            fakeEntity.remove();
            return;
        }

        // Schedule a task to explode the firework later.
        new BukkitRunnable() {
            public void run() {
                fakeEntity.playEffect(EntityEffect.FIREWORK_EXPLODE);
                fakeEntity.remove();
            }
        }.runTaskLater(MechanicsCore.getPlugin(), flightTime);
    }

    @Override
    public String getKeyword() {
        return "Firework";
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://github.com/WeaponMechanics/MechanicsMain/wiki/FireworkMechanic";
    }

    @NotNull
    @Override
    public Mechanic serialize(SerializeData data) throws SerializerException {
        ItemStack fireworkItem = new ItemStack(ReflectionUtil.getMCVersion() >= 13 ? Material.FIREWORK_ROCKET : Material.valueOf("FIREWORK"));
        FireworkMeta meta = (FireworkMeta) fireworkItem.getItemMeta();
        List<FireworkEffect> effects = data.of("Effects").getImpliedList(new FireworkData()).stream().map(FireworkData::getEffect).toList();
        int flightTime = data.of("Flight_Time").getInt(0);
        meta.addEffects(effects);
        meta.setPower(flightTime);
        fireworkItem.setItemMeta(meta);

        Targeter viewers = data.of("Viewers").getRegistry(Mechanics.TARGETERS, null);
        List<Condition> viewerConditions = data.of("Viewer_Conditions").getRegistryList(Mechanics.CONDITIONS);

        return applyParentArgs(data, new FireworkMechanic(fireworkItem, flightTime, viewers, viewerConditions));
    }
}
