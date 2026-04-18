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
    public static  Supplier<AttachmentType<Boolean>> IN_VOID = VOID_ATTACHMENTS.register(
            "in_void",//创建一个Supplier类型的布尔类附件 Supplier可以理解为异步执行 先不给你值 等需要的时候再给
            ()-> AttachmentType.builder(()->false)
                    .sync(ByteBufCodecs.BOOL)//这个附件是一个布尔值 把它同步到服务器上 不然的话客户端不知道你进虚空 会有显示bug
                    .build()//需要附件的时候创建一个默认值为false的附件 .build完成创建
    );//用刚刚定义的注册起注册一个IN_VOID附件 参数（注册id，注册值）
    public static void register(IEventBus bus) {
        VOID_ATTACHMENTS.register(bus);
    }
}
