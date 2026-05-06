package com.example.voidcraft.Custom.Behavior.BlackHole;

import com.example.voidcraft.Effect.VoidBlackHoleInstance;

public class BlackHoleInstance {
    private float peakHalf = 0.0F;
    private int coreColor = 0;
    private int rimColor = 0;
    private float centerYOffset = 0.0F;
    public VoidBlackHoleInstance.Config hole = VoidBlackHoleInstance.Config.builder()
            .durationTicks(100)
            .centerYOffset(centerYOffset)
            .coreFollowCameraPitch(true)
            .diskFollowCameraPitch(true)
            .distortionFollowCameraPitch(true)
            .startHalfHeight(0.0F)
            .startHalfWidth(0.0F)
            .peakHalfHeight(peakHalf)
            .peakHalfWidth(peakHalf)
            .coreColor(coreColor)
            .coreColor(rimColor)
            .coreAlpha(0.9F)
            .rimAlpha(0.7F)
            .coreAlphaScale(1F)
            .rimAlphaScale(1F)
            .build();

}
