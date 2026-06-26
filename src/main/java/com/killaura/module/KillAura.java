package com.killaura.module;

import com.killaura.config.KillAuraConfig;
import com.killaura.util.EntityUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class KillAura {
    private static final KillAura INSTANCE = new KillAura();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private PlayerEntity target = null;
    private int attackCooldown = 0;
    private int minAttackDelay = 1; // ticks between attacks, calculated from CPS
    private int tickCounter = 0;
    private boolean critJumpScheduled = false;

    // AutoTotem state machine
    private TotemSwapState totemState = TotemSwapState.IDLE;
    private int totemSlot = -1;
    private int totemActionTick = 0;

    private enum TotemSwapState {
        IDLE,
        PICKUP_TOTEM,
        PLACE_OFFHAND,
        RETURN_OLD_OFFHAND
    }

    public static KillAura getInstance() {
        return INSTANCE;
    }

    public void toggle() {
        KillAuraConfig cfg = KillAuraConfig.getInstance();
        cfg.enabled = !cfg.enabled;
        KillAuraConfig.save();
    }

    public void setEnabled(boolean enabled) {
        KillAuraConfig.getInstance().enabled = enabled;
        KillAuraConfig.save();
    }

    public void tick(MinecraftClient client) {
        KillAuraConfig cfg = KillAuraConfig.getInstance();
        if (!cfg.enabled) {
            resetState();
            return;
        }

        PlayerEntity player = client.player;
        if (player == null || !player.isAlive()) {
            resetState();
            return;
        }

        // Update timers
        tickCounter++;
        if (attackCooldown > 0) attackCooldown--;

        // CPS-based delay
        minAttackDelay = Math.max(1, (int) Math.round(20.0 / cfg.cps));

        // Find target
        target = EntityUtils.getNearestPlayerWithinRange(client, cfg.range);
        if (target == null || !target.isAlive() || target == player) {
            target = null;
            releaseUseKey();
            critJumpScheduled = false;
            // If no target, still allow auto-totem to process
            processAutoTotem();
            return;
        }

        // Aim at target
        if (target != null) {
            smoothAim(player, target, cfg.aimSpeed);
        }

        // Auto-Sprint (disable when jumping for crit)
        if (cfg.autoSprint) {
            if (!critJumpScheduled && !player.isSprinting()) {
                mc.options.sprintKey.setPressed(true);
            }
        } else {
            mc.options.sprintKey.setPressed(false);
        }

        // Auto-Crit jump scheduling
        if (cfg.autoCrit && player.isOnGround() && !critJumpScheduled && attackCooldown <= 0) {
            // Schedule a crit jump: jump now, attack next tick while in air
            player.jump();
            critJumpScheduled = true;
            attackCooldown = 1; // small delay before next attack
            return; // skip attack this tick
        }

        // Attack logic
        boolean canAttack = attackCooldown <= 0 && player.getAttackCooldownProgress(0.5f) >= 0.99f;
        if (canAttack) {
            // If we previously jumped for crit, now we are in air: attack with crit
            if (critJumpScheduled) {
                // Check we are actually falling (for crit)
                if (!player.isOnGround() && player.fallDistance > 0.0f) {
                    performAttack(player, target);
                    critJumpScheduled = false;
                } else {
                    // landed or cancelled; reset
                    critJumpScheduled = false;
                }
            } else if (!cfg.autoCrit) {
                // Normal attack without crit
                performAttack(player, target);
            } else {
                // Auto-crit is on but we couldn't jump; just attack normally
                performAttack(player, target);
            }
        }

        // Auto-block (hold right click to block with shield) when not attacking
        if (cfg.autoBlock && canBlock(player)) {
            if (attackCooldown > minAttackDelay / 2) { // roughly half of cooldown, block
                mc.options.useKey.setPressed(true);
            } else {
                mc.options.useKey.setPressed(false);
            }
        } else {
            mc.options.useKey.setPressed(false);
        }

        // Auto-totem processing
        processAutoTotem();
    }

    private void performAttack(PlayerEntity player, PlayerEntity target) {
        // Release use key before attack to avoid interaction cancellation
        mc.options.useKey.setPressed(false);

        // Actually attack
        mc.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);

        // Set cooldown based on CPS
        attackCooldown = minAttackDelay;
    }

    private void smoothAim(PlayerEntity player, PlayerEntity target, double speed) {
        Vec3d targetPos = target.getPos().add(0, target.getEyeHeight(target.getPose()), 0);
        Vec3d eyePos = player.getPos().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d delta = targetPos.subtract(eyePos);

        double yaw = Math.toDegrees(Math.atan2(-delta.x, delta.z));
        double pitch = Math.toDegrees(-Math.atan2(delta.y, Math.sqrt(delta.x*delta.x + delta.z*delta.z)));

        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();

        // Normalize yaw difference
        float yawDelta = (float) MathHelper.wrapDegrees(yaw - currentYaw);
        float pitchDelta = (float) MathHelper.wrapDegrees(pitch - currentPitch);

        float moveSpeed = (float) (speed * 5.0f); // adjust sensitivity
        player.setYaw(currentYaw + MathHelper.clamp(yawDelta, -moveSpeed, moveSpeed));
        player.setPitch(MathHelper.clamp(currentPitch + MathHelper.clamp(pitchDelta, -moveSpeed, moveSpeed), -90f, 90f));
    }

    private boolean canBlock(PlayerEntity player) {
        return player.getOffHandStack().getItem() == Items.SHIELD && !player.getItemCooldownManager().isCoolingDown(Items.SHIELD);
    }

    private void releaseUseKey() {
        mc.options.useKey.setPressed(false);
    }

    private void resetState() {
        target = null;
        critJumpScheduled = false;
        mc.options.useKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    // ----- Auto Totem -----
    private void processAutoTotem() {
        KillAuraConfig cfg = KillAuraConfig.getInstance();
        if (!cfg.autoTotem) {
            totemState = TotemSwapState.IDLE;
            return;
        }
        PlayerEntity player = mc.player;
        if (player == null) return;

        if (player.getHealth() > 6.0f) {
            // Health is safe, reset state
            totemState = TotemSwapState.IDLE;
            return;
        }

        // Check if offhand already has a totem
        if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            totemState = TotemSwapState.IDLE;
            return;
        }

        if (totemState == TotemSwapState.IDLE) {
            // Find a totem in inventory
            totemSlot = findTotemInInventory(player);
            if (totemSlot == -1) return; // no totem found
            // Begin swap sequence
            totemState = TotemSwapState.PICKUP_TOTEM;
            totemActionTick = 0;
            clickSlot(totemSlot, SlotActionType.PICKUP); // pick up totem
            totemActionTick = 1; // wait for next tick
        }

        if (totemState == TotemSwapState.PICKUP_TOTEM) {
            totemActionTick++;
            if (totemActionTick >= 2) { // proceed next tick
                // Place in offhand (slot 45)
                clickSlot(45, SlotActionType.PICKUP);
                totemState = TotemSwapState.PLACE_OFFHAND;
                totemActionTick = 0;
            }
        } else if (totemState == TotemSwapState.PLACE_OFFHAND) {
            totemActionTick++;
            if (totemActionTick >= 2) {
                // Return old offhand item back to the original totem slot
                clickSlot(totemSlot, SlotActionType.PICKUP);
                totemState = TotemSwapState.IDLE;
                totemSlot = -1;
            }
        }
    }

    private int findTotemInInventory(PlayerEntity player) {
        // Check entire inventory (main + hotbar)
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                return i; // slot 0-35
            }
        }
        return -1;
    }

    private void clickSlot(int slot, SlotActionType action) {
        if (mc.player == null) return;
        // Use the player's own screen handler (syncId 0) when no screen is open
        mc.interactionManager.clickSlot(0, slot, 0, action, mc.player);
    }
}
