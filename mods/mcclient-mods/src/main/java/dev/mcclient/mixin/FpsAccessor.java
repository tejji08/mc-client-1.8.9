package dev.mcclient.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the frame counter the client already keeps for the F3 overlay.
 *
 * <p>It is a private static field, so an accessor mixin is the only way in without recomputing a
 * number the game already has.
 *
 * <p>Nothing but {@code @Accessor} methods may live here: a single ordinary method makes Mixin
 * classify this as an interface mixin, which then refuses the target for not being an interface.
 */
@Mixin(MinecraftClient.class)
public interface FpsAccessor {

    @Accessor("currentFps")
    static int getCurrentFps() {
        throw new AssertionError("mixin accessor not applied");
    }
}
