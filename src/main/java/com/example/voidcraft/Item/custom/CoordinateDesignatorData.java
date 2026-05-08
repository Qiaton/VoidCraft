package com.example.voidcraft.Item.custom;

import com.example.voidcraft.Block.entity.BoundVoidPosition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Optional;

// 坐标制定器的数据存在物品栈上：当前模式 + 第一次点击记录的目标。
public record CoordinateDesignatorData(Mode mode, Optional<BoundVoidPosition> firstTarget) {
    public static final CoordinateDesignatorData DEFAULT = new CoordinateDesignatorData(Mode.OUTPUT, Optional.empty());
    public static final Codec<CoordinateDesignatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.OUTPUT).forGetter(CoordinateDesignatorData::mode),
            BoundVoidPosition.CODEC.optionalFieldOf("firstTarget").forGetter(CoordinateDesignatorData::firstTarget)
    ).apply(instance, CoordinateDesignatorData::new));

    public CoordinateDesignatorData withMode(Mode mode) {
        // 切换模式时清空第一次选择，避免新模式误用旧坐标。
        return new CoordinateDesignatorData(mode, Optional.empty());
    }

    public CoordinateDesignatorData withFirstTarget(BoundVoidPosition target) {
        return new CoordinateDesignatorData(this.mode, Optional.of(target));
    }

    public CoordinateDesignatorData clearFirstTarget() {
        return new CoordinateDesignatorData(this.mode, Optional.empty());
    }

    public enum Mode {
        // OUTPUT：先点输出端再点输入端；INPUT：先点输入端再点输出端；UNBIND：打开解绑面板。
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
            // 潜行右键按固定顺序轮换模式。
            Mode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static Mode byId(String id) {
            // 读到未知模式时回到 OUTPUT，保证旧物品数据还能使用。
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
