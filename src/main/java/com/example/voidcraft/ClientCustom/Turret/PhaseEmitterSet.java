package com.example.voidcraft.ClientCustom.Turret;

import com.example.voidcraft.Custom.Behavior.Turret.PhaseEmitterSlot;
import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidBlackHoleManager;
import com.example.voidcraft.Item.custom.ModuleItem.ModuleType.PhaseTurretModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PhaseEmitterSet {
    // 轨道和跟随参数集中在这里，后续调炮台球手感时不需要进渲染实例内部找数值。
    private static final double THIRD_PERSON_FOLLOW_STRENGTH = 0.55D;
    private static final double FIRST_PERSON_FOLLOW_STRENGTH = 0.99D;
    private static final double FIRST_PERSON_ORBIT_DISTANCE = 0.3D;
    private static final double FIRST_PERSON_ORBIT_RADIUS = 1D;
    private static final double FIRST_PERSON_ORBIT_SPEED = 0.11D;
    private static final double THIRD_PERSON_ORBIT_HORIZONTAL_RADIUS = 1.15D;
    private static final double THIRD_PERSON_ORBIT_VERTICAL_RADIUS = 0.85D;
    private static final double THIRD_PERSON_ORBIT_HEIGHT_RATIO = 0.68D;
    private static final double THIRD_PERSON_ORBIT_BACK_OFFSET = 0.55D;
    private static final double THIRD_PERSON_ORBIT_SPEED = 0.09D;

    // 炮台球本质复用持久黑洞视觉；颜色来自 PhaseTurretModule.VisualColors，保证手动/辅助一致。
    public static final VoidBlackHoleInstance.Config PHASE_FRAGMENT_PORTAL =
            VoidBlackHoleInstance.Config.builder()
                    .durationTicks(34)
                    .centerYOffset(0.70F)

                    // 半径 0.7m，scale=1.0 时最大视觉半径约 0.7 格。
                    .startHalfHeight(0.08F)
                    .startHalfWidth(0.08F)
                    .peakHalfHeight(0.35F)
                    .peakHalfWidth(0.35F)
                    .endHalfHeight(0.10F)
                    .endHalfWidth(0.10F)
                    .peakHoldTicks(18)

                    // 相位传送门：核心淡绿色，吸积盘/外圈正常绿色。
                    .coreColor(PhaseTurretModule.VisualColors.ORB_CORE)
                    .color(PhaseTurretModule.VisualColors.ORB_RIM)
                    .coreAlpha(0.92F)
                    .rimAlpha(0.26F)
                    .diskAlpha(0F)

                    .coreAlphaScale(0.72F)
                    .rimAlphaScale(0.35F)
                    .shaderRimAlphaScale(0.72F)
                    .horizonAlphaScale(0F)
                    .centerShadowScale(0.0F)
                    .hideFromOwnerInFirstPerson(false)

                    // 吸积盘偏薄，像稳定的发射孔，不像爆炸黑洞。
                    .diskInnerRadius(0.48F)
                    .diskOuterRadius(1.36F)
                    .diskVerticalScale(0.13F)
                    .diskDepthScale(0.38F)
                    .diskPitchFadeStart(0.66F)
                    .diskPitchFadeEnd(0.94F)

                    // 扭曲区域倍率 1.2。
                    .distortionWidthScale(1.80F)
                    .distortionHeightScale(1.80F)
                    .distortionAlpha(7F)
                    .distortionThickness(3.20F)
                    .distortionAmplitude(4.40F)

                    // 轻微旋涡 + 向内吸入，表现相位空间正在吐出碎片。
                    .swirlStrength(0.38F)
                    .suctionStrength(0.44F)
                    .noiseFrequency(3.60F)
                    .noiseScrollSpeed(2.80F)

                    .coreFollowCameraPitch(true)
                    .diskFollowCameraPitch(false)
                    .distortionFollowCameraPitch(true)
                    .occludedByBlocks(true)
                    .build();


    private VoidBlackHoleInstance leftTop;
    private VoidBlackHoleInstance rightBottom;
    private VoidBlackHoleInstance leftBottom;
    private VoidBlackHoleInstance rightTop;

    public PhaseEmitterSet() {
    }

    Vec3 getEmitterPos(Player owner, PhaseEmitterSlot slot) {
        return computeEmitterPos(owner, slot);
    }

    Vec3 getCurrentEmitterPos(Player owner, PhaseEmitterSlot slot, float partialTick) {
        VoidBlackHoleInstance emitter = getEmitter(slot);
        if (emitter != null) {
            return emitter.getCenter(partialTick);
        }

        return computeEmitterPos(owner, slot);
    }

    static Vec3 computeEmitterPos(Player owner, PhaseEmitterSlot slot) {
        Minecraft mc = Minecraft.getInstance();

        boolean isSelf = owner == mc.player;
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();

        if (isSelf && isFirstPerson) {
            return getFirstPersonEmitterPos(slot);
        }

        return getThirdPersonEmitterPos(owner, slot);
    }

    private static Vec3 getFirstPersonEmitterPos(PhaseEmitterSlot slot) {
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
        double orbitAngle = getClientOrbitTime(mc) * FIRST_PERSON_ORBIT_SPEED + getOrbitPhase(slot);

        return cameraPos
                .add(look.scale(FIRST_PERSON_ORBIT_DISTANCE))
                .add(right.scale(Math.cos(orbitAngle) * FIRST_PERSON_ORBIT_RADIUS))
                .add(up.scale(Math.sin(orbitAngle) * FIRST_PERSON_ORBIT_RADIUS));
    }

    public void create(Player owner) {
        leftTop = createEmitter(owner, getEmitterPos(owner, PhaseEmitterSlot.LEFT_TOP));
        rightBottom = createEmitter(owner, getEmitterPos(owner, PhaseEmitterSlot.RIGHT_BOTTOM));
        leftBottom = createEmitter(owner, getEmitterPos(owner, PhaseEmitterSlot.LEFT_BOTTOM));
        rightTop = createEmitter(owner, getEmitterPos(owner, PhaseEmitterSlot.RIGHT_TOP));
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

    private void updateEmitters(Player owner, double followStrength) {
        updateEmitter(leftTop, getEmitterPos(owner, PhaseEmitterSlot.LEFT_TOP), followStrength);
        updateEmitter(rightBottom, getEmitterPos(owner, PhaseEmitterSlot.RIGHT_BOTTOM), followStrength);
        updateEmitter(leftBottom, getEmitterPos(owner, PhaseEmitterSlot.LEFT_BOTTOM), followStrength);
        updateEmitter(rightTop, getEmitterPos(owner, PhaseEmitterSlot.RIGHT_TOP), followStrength);
    }

    public void remove() {
        removeEmitter(leftTop);
        removeEmitter(rightBottom);
        removeEmitter(leftBottom);
        removeEmitter(rightTop);

        leftTop = null;
        rightBottom = null;
        leftBottom = null;
        rightTop = null;
    }

    private static Vec3 getThirdPersonEmitterPos(Player owner, PhaseEmitterSlot slot) {
        Vec3 forward = getOwnerForwardVector(owner);
        Vec3 right = getRightVector(forward);
        Vec3 center = owner.position()
                .add(0.0D, owner.getBbHeight() * THIRD_PERSON_ORBIT_HEIGHT_RATIO, 0.0D)
                .add(forward.scale(-THIRD_PERSON_ORBIT_BACK_OFFSET));
        double orbitAngle = owner.level().getGameTime() * THIRD_PERSON_ORBIT_SPEED + getOrbitPhase(slot);

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
        return switch (slot) {
            case LEFT_TOP -> 0.0D;
            case RIGHT_TOP -> Math.PI * 0.5D;
            case RIGHT_BOTTOM -> Math.PI;
            case LEFT_BOTTOM -> Math.PI * 1.5D;
        };
    }

    private VoidBlackHoleInstance createEmitter(Player owner, Vec3 center) {
        return VoidBlackHoleManager.addPersistentBlackHole(
                owner.getId(),
                center,
                1.0F,
                PHASE_FRAGMENT_PORTAL
        );
    }

    private static boolean isLocalFirstPersonOwner(Player owner) {
        Minecraft mc = Minecraft.getInstance();
        return owner == mc.player && mc.options.getCameraType().isFirstPerson();
    }

    private void updateEmitter(VoidBlackHoleInstance emitter, Vec3 center, double followStrength) {
        if (emitter == null) {
            return;
        }

        emitter.setTargetCenter(center, followStrength);
    }

    private VoidBlackHoleInstance getEmitter(PhaseEmitterSlot slot) {
        return switch (slot) {
            case LEFT_TOP -> leftTop;
            case RIGHT_BOTTOM -> rightBottom;
            case LEFT_BOTTOM -> leftBottom;
            case RIGHT_TOP -> rightTop;
        };
    }

    private void removeEmitter(VoidBlackHoleInstance emitter) {
        if (emitter == null) {
            return;
        }

        VoidBlackHoleManager.removeBlackHole(emitter);
    }
}
