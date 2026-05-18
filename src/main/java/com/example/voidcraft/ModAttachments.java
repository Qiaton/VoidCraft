package com.example.voidcraft;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {       //模组的附件中心
    //延迟创造一个附件类型 类型为？（差不多是泛型）
    public static final DeferredRegister<AttachmentType<?>> VOID_ATTACHMENTS = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES,//create参数为（类型，id）
            VoidCraft.MODID
    );
    public static Supplier<AttachmentType<Boolean>> IN_PHASE = VOID_ATTACHMENTS.register(
            "in_phase",//原来的虚空状态改名成相位状态，保留免伤、特效、不可交互等规则
            ()-> AttachmentType.builder(()->false)
                    .sync(ByteBufCodecs.BOOL)
                    .build()
    );
    public static Supplier<AttachmentType<Boolean>> IN_VOID = VOID_ATTACHMENTS.register(
            "in_void",//新的虚空状态，用来控制不受方块碰撞；开启时也会带上相位状态
            ()-> AttachmentType.builder(()->false)
                    .sync(ByteBufCodecs.BOOL)//同步给客户端，避免本地玩家移动和渲染状态不同步
                    .build()//需要附件的时候创建一个默认值为false的附件 .build完成创建
    );//用刚刚定义的注册起注册一个IN_VOID附件 参数（注册id，注册值）
    public static Supplier<AttachmentType<Float>> VOID_SPEED = VOID_ATTACHMENTS.register(
            "void_speed",
            ()->AttachmentType.builder(()->0.3F)
                    .sync(ByteBufCodecs.FLOAT)
                    .build()
    );
    public static void register(IEventBus bus) {
        VOID_ATTACHMENTS.register(bus);
    }
}
