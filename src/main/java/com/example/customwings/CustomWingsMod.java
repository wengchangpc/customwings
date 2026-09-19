package com.example.customwings;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CustomWingsMod.MODID)
public class CustomWingsMod {
    public static final String MODID = "customwings";

    public CustomWingsMod() {
        WingsConfig.load();
    }

    /** 模组总线：注册翅膀模型几何 + 把翅膀层挂到玩家渲染器上（仅客户端）。 */
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(WingsLayer.LAYER, WingsLayer::createLayer);
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            EntityRendererProvider.Context ctx = event.getContext();
            event.getSkins().forEach((skin, renderer) -> {
                if (renderer instanceof PlayerRenderer pr) {
                    pr.addLayer(new WingsLayer(pr, ctx.bakeLayer(WingsLayer.LAYER)));
                }
            });
            System.out.println("[CustomWings] 翅膀渲染层已挂载！");
        }
    }

    /** 游戏总线：客户端指令 /customwings */
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("customwings")
                .then(Commands.literal("toggle").executes(ctx -> {
                    WingsConfig.enabled = !WingsConfig.enabled;
                    WingsConfig.save();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            WingsConfig.enabled ? "[CustomWings] 翅膀已开启！" : "[CustomWings] 翅膀已关闭。"), false);
                    return 1;
                }))
                .then(Commands.literal("color")
                    .then(Commands.argument("mode", StringArgumentType.word()).executes(ctx -> {
                        String mode = StringArgumentType.getString(ctx, "mode");
                        if (!mode.equalsIgnoreCase("rainbow") && !(mode.startsWith("#") && mode.length() == 7)) {
                            ctx.getSource().sendFailure(Component.literal(
                                    "[CustomWings] 无效颜色！用法: /customwings color rainbow 或 /customwings color #FF8800"));
                            return 0;
                        }
                        WingsConfig.colorMode = mode;
                        WingsConfig.save();
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "[CustomWings] 配色已切换为 " + mode), false);
                        return 1;
                    })))
                .then(Commands.literal("size")
                    .then(Commands.argument("value", FloatArgumentType.floatArg(0.3F, 3.0F)).executes(ctx -> {
                        WingsConfig.size = FloatArgumentType.getFloat(ctx, "value");
                        WingsConfig.save();
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "[CustomWings] 翅膀大小已设为 " + WingsConfig.size), false);
                        return 1;
                    })))
                .then(Commands.literal("speed")
                    .then(Commands.argument("value", FloatArgumentType.floatArg(0.01F, 1.0F)).executes(ctx -> {
                        WingsConfig.flapSpeed = FloatArgumentType.getFloat(ctx, "value");
                        WingsConfig.save();
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "[CustomWings] 扇动速度已设为 " + WingsConfig.flapSpeed), false);
                        return 1;
                    })))
                .then(Commands.literal("reload").executes(ctx -> {
                    WingsConfig.load();
                    ctx.getSource().sendSuccess(() -> Component.literal("[CustomWings] 配置已重新加载！"), false);
                    return 1;
                })));
        }
    }
}
