package com.killaura.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;
import java.util.List;

public class EntityUtils {

    public static PlayerEntity getNearestPlayerWithinRange(MinecraftClient client, double range) {
        if (client.player == null || client.world == null) return null;

        Vec3d eyePos = client.player.getEyePos();
        Box searchBox = new Box(
                eyePos.x - range, eyePos.y - range, eyePos.z - range,
                eyePos.x + range, eyePos.y + range, eyePos.z + range
        );

        List<PlayerEntity> players = client.world.getEntitiesByClass(
                PlayerEntity.class,
                searchBox,
                player -> player != client.player && player.isAlive() && hasLineOfSight(client.player, player)
        );

        return players.stream()
                .min(Comparator.comparingDouble(player -> player.squaredDistanceTo(client.player)))
                .orElse(null);
    }

    public static boolean hasLineOfSight(PlayerEntity observer, PlayerEntity target) {
        Vec3d start = observer.getEyePos();
        Vec3d end = target.getEyePos();
        // Simple check using canSee (works in vanilla logic)
        return observer.canSee(target);
    }
}
