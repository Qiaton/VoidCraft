package com.example.voidcraft.ClientCustom.Turret;

import com.example.voidcraft.Custom.Behavior.Turret.PhaseEmitterSlot;
import com.example.voidcraft.Effect.VoidRingManager;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.PhaseTurretModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PhaseEmitterSet {
    // 轨道和跟随参数集中在这里，后续调炮台球手感时不需要进渲染实例内部找数值。
    private static final double THIRD_PERSON_FOLLOW_STRENGTH = 0.55D;
    private static final double FIRST_PERSON_FOLLOW_STRENGTH = 0.99D;
    private static final double FIRST_PERSON_ORBIT_DISTANCE = 0.3D;
    private static final double FIRST_PERSON_ORBIT_RADIUS = 1D;
    private static final double FIRST_PERSON_ORBIT_SPEED = 0.1D;
    private static final double THIRD_PERSON_ORBIT_HORIZONTAL_RADIUS = 1.15D;
    private static final double THIRD_PERSON_ORBIT_VERTICAL_RADIUS = 0.85D;
    private static final double THIRD_PERSON_ORBIT_HEIGHT_RATIO = 0.68D;
    private static final double THIRD_PERSON_ORBIT_BACK_OFFSET = 0.55D;
    private static final double THIRD_PERSON_ORBIT_SPEED = 0.1D;
    private static final double ORBIT_SPEED_PER_LEVEL = 0.12D;
    private static final double MAX_ORBIT_SPEED_MULTIPLIER = 1.85D;

    private final int emitterCount;
    private final int orbColorLevel;
    private final double orbitSpeedMultiplier;
    private final List<EmitterState> emitters;

    public PhaseEmitterSet() {
        this(PhaseEmitterSlot.configuredCount());
    }

    public PhaseEmitterSet(int emitterCount) {
        this(emitterCount, 1);
    }

    public PhaseEmitterSet(int emitterCount, int orbColorLevel) {
        this.emitterCount = PhaseEmitterSlot.normalizeCount(emitterCount);
        this.orbColorLevel = Math.max(1, orbColorLevel);
        this.orbitSpeedMultiplier = getOrbitSpeedMultiplier(this.orbColorLevel);
        this.emitters = new ArrayList<>(this.emitterCount);
    }

    public int getEmitterCount() {
        return this.emitterCount;
    }

    public int getOrbColorLevel() {
        return this.orbColorLevel;
    }

    Vec3 getEmitterPos(Player owner, PhaseEmitterSlot slot) {
        return computeEmitterPos(owner, slot, this.orbitSpeedMultiplier);
    }

    Vec3 getCurrentEmitterPos(Player owner, PhaseEmitterSlot slot, float partialTick) {
        EmitterState emitter = getEmitter(slot);
        if (emitter != null) {
            return emitter.getCenter(partialTick);
        }

        return computeEmitterPos(owner, slot, this.orbitSpeedMultiplier);
    }

    static Vec3 computeEmitterPos(Player owner, PhaseEmitterSlot slot) {
        return computeEmitterPos(owner, slot, 1.0D);
    }

    private static Vec3 computeEmitterPos(Player owner, PhaseEmitterSlot slot, double orbitSpeedMultiplier) {
        Minecraft mc = Minecraft.getInstance();

        boolean isSelf = owner == mc.player;
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();

        if (isSelf && isFirstPerson) {
            return getFirstPersonEmitterPos(slot, orbitSpeedMultiplier);
        }

        return getThirdPersonEmitterPos(owner, slot, orbitSpeedMultiplier);
    }

    private static Vec3 getFirstPersonEmitterPos(PhaseEmitterSlot slot, double orbitSpeedMultiplier) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        Vec3 cameraPos = camera.position();
        Vec3 look = new Vec3(
                camera.forwardVector().x(),
                camera.forwardVector().y(),
                camera.forwardVector().z()
        );

        Vec3 up = new Vec3(
                camera.upVector().x(),
                camera.upVector().y(),
                camera.upVector().z()
        );

        Vec3 left = new Vec3(
                camera.leftVector().x(),
                camera.leftVector().y(),
                camera.leftVector().z()
        );

        Vec3 right = left.scale(-1);
        double orbitAngle = getClientOrbitTime(mc) * FIRST_PERSON_ORBIT_SPEED * orbitSpeedMultiplier
                + getOrbitPhase(slot);

        return cameraPos
                .add(look.scale(FIRST_PERSON_ORBIT_DISTANCE))
                .add(right.scale(Math.cos(orbitAngle) * FIRST_PERSON_ORBIT_RADIUS))
                .add(up.scale(Math.sin(orbitAngle) * FIRST_PERSON_ORBIT_RADIUS));
    }

    public void create(Player owner) {
        this.emitters.clear();
        for (PhaseEmitterSlot slot : PhaseEmitterSlot.fireOrder(this.emitterCount)) {
            this.emitters.add(createEmitter(owner, getEmitterPos(owner, slot)));
        }
    }

    public void playToggleFlash(Player owner) {
        for (PhaseEmitterSlot slot : PhaseEmitterSlot.fireOrder(this.emitterCount)) {
            Vec3 center = getCurrentEmitterPos(owner, slot, 1.0F);
            VoidRingManager.addRing(center, 1.0F, PhaseTurretModule.getToggleFlashPreset());
        }
    }

    public void update(Player owner) {
        double followStrength = isLocalFirstPersonOwner(owner)
                ? FIRST_PERSON_FOLLOW_STRENGTH
                : THIRD_PERSON_FOLLOW_STRENGTH;
        updateEmitters(owner, followStrength);
    }

    public void updateFirstPersonNow(Player owner) {
        if (!isLocalFirstPersonOwner(owner)) {
            return;
        }

        updateEmitters(owner, FIRST_PERSON_FOLLOW_STRENGTH);
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, Vec3 cameraPos, float partialTick, int light, boolean shaderCompat) {
        for (EmitterState emitter : this.emitters) {
            PhaseEmitterOrbRenderer.render(
                    poseStack,
                    buffer,
                    emitter.getCenter(partialTick),
                    cameraPos,
                    emitter.radius(),
                    emitter.coreColor(),
                    emitter.rimColor(),
                    light,
                    shaderCompat
            );
        }
    }

    private void updateEmitters(Player owner, double followStrength) {
        for (PhaseEmitterSlot slot : PhaseEmitterSlot.fireOrder(this.emitterCount)) {
            updateEmitter(getEmitter(slot), getEmitterPos(owner, slot), followStrength);
        }
    }

    public void remove() {
        this.emitters.clear();
    }

    private static Vec3 getThirdPersonEmitterPos(Player owner, PhaseEmitterSlot slot, double orbitSpeedMultiplier) {
        Vec3 forward = getOwnerForwardVector(owner);
        Vec3 right = getRightVector(forward);
        Vec3 center = owner.position()
                .add(0.0D, owner.getBbHeight() * THIRD_PERSON_ORBIT_HEIGHT_RATIO, 0.0D)
                .add(forward.scale(-THIRD_PERSON_ORBIT_BACK_OFFSET));
        double orbitAngle = owner.level().getGameTime() * THIRD_PERSON_ORBIT_SPEED * orbitSpeedMultiplier
                + getOrbitPhase(slot);

        return center
                .add(right.scale(Math.cos(orbitAngle) * THIRD_PERSON_ORBIT_HORIZONTAL_RADIUS))
                .add(0.0D, Math.sin(orbitAngle) * THIRD_PERSON_ORBIT_VERTICAL_RADIUS, 0.0D);
    }

    private static Vec3 getOwnerForwardVector(Player owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
        if (flatLook.lengthSqr() < 0.0001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }

        return flatLook.normalize();
    }

    private static Vec3 getRightVector(Vec3 forward) {
        return new Vec3(-forward.z, 0.0D, forward.x);
    }

    private static double getClientOrbitTime(Minecraft mc) {
        if (mc.level == null) {
            return 0.0D;
        }

        return mc.level.getGameTime() + mc.gameRenderer.getMainCamera().getPartialTickTime();
    }

    private static double getOrbitPhase(PhaseEmitterSlot slot) {
        return slot.orbitPhase();
    }

    private static double getOrbitSpeedMultiplier(int level) {
        return Math.min(MAX_ORBIT_SPEED_MULTIPLIER, 1.0D + Math.max(0, level - 1) * ORBIT_SPEED_PER_LEVEL);
    }

    private EmitterState createEmitter(Player owner, Vec3 center) {
        return new EmitterState(
                center,
                PhaseTurretModule.VisualSizes.ORB_PEAK_HALF_SIZE,
                getOrbCoreColor(),
                getOrbRimColor()
        );
    }

    private int getOrbCoreColor() {
        return PhaseTurretModule.VisualColors.orbCoreForLevel(this.orbColorLevel);
    }

    private int getOrbRimColor() {
        return PhaseTurretModule.VisualColors.orbRimForLevel(this.orbColorLevel);
    }

    private static boolean isLocalFirstPersonOwner(Player owner) {
        Minecraft mc = Minecraft.getInstance();
        return owner == mc.player && mc.options.getCameraType().isFirstPerson();
    }

    private void updateEmitter(EmitterState emitter, Vec3 center, double followStrength) {
        if (emitter == null) {
            return;
        }

        emitter.moveToward(center, followStrength);
    }

    private EmitterState getEmitter(PhaseEmitterSlot slot) {
        int index = slot.fireIndex();
        if (index < 0 || index >= this.emitters.size()) {
            return null;
        }

        return this.emitters.get(index);
    }

    private static final class EmitterState {
        private Vec3 previousCenter;
        private Vec3 center;
        private final float radius;
        private final int coreColor;
        private final int rimColor;

        private EmitterState(Vec3 center, float radius, int coreColor, int rimColor) {
            this.previousCenter = center;
            this.center = center;
            this.radius = radius;
            this.coreColor = coreColor;
            this.rimColor = rimColor;
        }

        private Vec3 getCenter(float partialTick) {
            return this.previousCenter.lerp(this.center, Mth.clamp(partialTick, 0.0F, 1.0F));
        }

        private void moveToward(Vec3 targetCenter, double followStrength) {
            double strength = Mth.clamp(followStrength, 0.0D, 1.0D);
            this.previousCenter = this.center;
            this.center = strength >= 0.999D ? targetCenter : this.center.lerp(targetCenter, strength);
        }

        private float radius() {
            return this.radius;
        }

        private int coreColor() {
            return this.coreColor;
        }

        private int rimColor() {
            return this.rimColor;
        }
    }
}
