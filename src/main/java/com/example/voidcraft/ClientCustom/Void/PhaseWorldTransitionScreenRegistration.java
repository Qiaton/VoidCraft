package com.example.voidcraft.ClientCustom.Void;

import com.example.voidcraft.world.PhaseDimensions;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;

public final class PhaseWorldTransitionScreenRegistration {
    private PhaseWorldTransitionScreenRegistration() {
    }

    public static void registerPhaseWorldTransitionScreen(RegisterDimensionTransitionScreenEvent event) {
        event.registerIncomingEffect(PhaseDimensions.PHASE_MIRROR, PhaseWorldTransitionScreen::new);
        event.registerOutgoingEffect(PhaseDimensions.PHASE_MIRROR, PhaseWorldTransitionScreen::new);
    }
}
