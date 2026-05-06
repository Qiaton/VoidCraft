package com.example.voidcraft.Item.custom;

import com.example.voidcraft.Block.entity.BoundVoidPosition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Optional;

public record CoordinateDesignatorData(Mode mode, Optional<BoundVoidPosition> firstTarget) {
    public static final CoordinateDesignatorData DEFAULT = new CoordinateDesignatorData(Mode.OUTPUT, Optional.empty());
    public static final Codec<CoordinateDesignatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.OUTPUT).forGetter(CoordinateDesignatorData::mode),
            BoundVoidPosition.CODEC.optionalFieldOf("firstTarget").forGetter(CoordinateDesignatorData::firstTarget)
    ).apply(instance, CoordinateDesignatorData::new));

    public CoordinateDesignatorData withMode(Mode mode) {
        return new CoordinateDesignatorData(mode, Optional.empty());
    }

    public CoordinateDesignatorData withFirstTarget(BoundVoidPosition target) {
        return new CoordinateDesignatorData(this.mode, Optional.of(target));
    }

    public CoordinateDesignatorData clearFirstTarget() {
        return new CoordinateDesignatorData(this.mode, Optional.empty());
    }

    public enum Mode {
        OUTPUT("output", "tooltip.void_craft.coordinate_designator.mode.output"),
        INPUT("input", "tooltip.void_craft.coordinate_designator.mode.input"),
        UNBIND("unbind", "tooltip.void_craft.coordinate_designator.mode.unbind");

        public static final Codec<Mode> CODEC = Codec.STRING.xmap(Mode::byId, Mode::getId);

        private final String id;
        private final String translationKey;

        Mode(String id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public String getId() {
            return id;
        }

        public Component getDisplayName() {
            return Component.translatable(this.translationKey);
        }

        public Mode next() {
            Mode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static Mode byId(String id) {
            if (id == null) {
                return OUTPUT;
            }

            String normalized = id.toLowerCase(Locale.ROOT);
            for (Mode mode : values()) {
                if (mode.id.equals(normalized)) {
                    return mode;
                }
            }
            return OUTPUT;
        }
    }
}
