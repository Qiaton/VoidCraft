package com.example.voidcraft.Item.custom.ModuleItem.ModuleType;

import com.example.voidcraft.Effect.VoidBlackHoleInstance;
import com.example.voidcraft.Effect.VoidRingInstance;

public class AnnihilationBlackHoleModule extends BlackHoleModule {
    private static final int CORE_COLOR = 0x010006;
    private static final int COLOR = 0x38105B;
    private static final VoidRingInstance.Preset ARRIVAL_LIGHT = makeArrivalLight(CORE_COLOR);
    private static final VoidBlackHoleInstance.Config PREVIEW_BLACK_HOLE = makePreviewBlackHole(CORE_COLOR, COLOR);

    public AnnihilationBlackHoleModule(Properties properties) {
        super(properties);
    }

    @Override
    public VoidBlackHoleInstance.Config getPreviewBlackHole() {
        return PREVIEW_BLACK_HOLE;
    }

    @Override
    protected VoidRingInstance.Preset getArrivalLight() {
        return ARRIVAL_LIGHT;
    }

    @Override
    protected int getCoreColor() {
        return CORE_COLOR;
    }

    @Override
    protected int getColor() {
        return COLOR;
    }

    @Override
    protected float getCoreDamage(Stats stats) {
        return 1.0F + stats.level() * 0.6F;
    }

    @Override
    protected boolean canHurtPlayers() {
        return true;
    }

    @Override
    protected boolean canPullPlayers() {
        return true;
    }
}
