package net.fabricmc.example.mixin;

import net.fabricmc.example.ExampleMod;
import net.fabricmc.example.ItemUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
//@Mixin(TitleScreen.class)
public class ExampleMixin {
	@Inject(at = @At("HEAD"), method = "init()V")
	private void init(CallbackInfo info) {
		final var MC = MinecraftClient.getInstance();
		ExampleMod.LOGGER.info("MC.getWindow().getX(): " + MC.getWindow().getX() + "MC.getWindow().getHeight(): " + MC.getWindow().getHeight());
		ItemUI ui = new ItemUI();
		MinecraftClient.getInstance().setScreen(ui);
	}
}
