package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LeapMechanic extends Mechanic {

    private double speed;
    private double verticalMultiplier;

    /**
     * Default constructor for serializer.
     */
    public LeapMechanic() {
    }

    public LeapMechanic(double speed, double verticalMultiplier) {
        this.speed = speed;
        this.verticalMultiplier = verticalMultiplier;
    }

    public double getSpeed() {
        return speed;
    }

    public double getVerticalMultiplier() {
        return verticalMultiplier;
    }

    @Override
    protected void use0(CastData cast) {
        Vector velocity = cast.getTargetLocation().subtract(cast.getSourceLocation()).toVector();
        velocity.setY(velocity.getY() * verticalMultiplier);
        velocity.normalize().multiply(speed);
        cast.getSource().setVelocity(velocity);
    }

    @Override
    public String getKeyword() {
        return "Leap";
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://github.com/WeaponMechanics/MechanicsMain/wiki/LeapMechanic";
    }

    @NotNull
    @Override
    public Mechanic serialize(SerializeData data) throws SerializerException {
        double speed = data.of("Speed").assertExists().getDouble();
        double verticalMultiplier = data.of("Vertical_Multiplier").getDouble(1.0);

        return applyParentArgs(data, new LeapMechanic(speed, verticalMultiplier));
    }
}
