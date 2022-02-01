package net.fabricmc.bookbot;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.Text;

import java.util.*;

@Environment(EnvType.CLIENT)
public class Bookbot implements ClientModInitializer {

	public static void sendChat(String text) {
		sendChat(new StringBuilder(text));
	}

	final static int UTF8_MIN_4 = 0x10000;
	final static int UTF8_MAX_4 = 0x1FFFFF;

	public static String generateUtf8(int size) {
		char[] data = new char[size];
		Random random = new Random();

		for(int i = 0; i < size; i++) {
			data[i] = (char) random.nextInt(UTF8_MIN_4, UTF8_MAX_4);
		}

		return String.copyValueOf(data);
	}

	public static void sendChat(StringBuilder builder) {
		builder.insert(0, "[BookBot] ");
		InGameHud hud = MinecraftClient.getInstance().inGameHud;
		hud.addChatMessage(MessageType.CHAT, Text.of(builder.toString()), UUID.randomUUID());
	}

	@Override
	public void onInitializeClient() {
		ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("bookbot").executes(context -> {
			sendChat("Usage: /bookbot <file | random>");
			return 1;
		}).then(
			ClientCommandManager.argument("action", StringArgumentType.greedyString())
			.executes(context -> {
				String action = StringArgumentType.getString(context, "action");
				ItemStack held = context.getSource().getPlayer().getMainHandStack();

				if(action.equalsIgnoreCase("random")) {
					if(!held.isOf(Items.WRITABLE_BOOK)) {
						sendChat("Please hold a book...");
						return 1;
					}

					final int MAX_PAGES_COUNT = 100;
					final int MAX_LENGTH_PAGE = 256;
					final int MAX_LENGTH_TITLE = 32;

					int slot = context.getSource().getPlayer().getInventory().selectedSlot;
					String title = generateUtf8(MAX_LENGTH_TITLE);
					String[] pages = new String[MAX_PAGES_COUNT];
					NbtList pagesNbt = new NbtList();

					for(int i = 0; i < MAX_PAGES_COUNT; i++) {
						pages[i] = generateUtf8(MAX_LENGTH_PAGE);
						pagesNbt.add(NbtString.of(pages[i]));
					}

					BookUpdateC2SPacket packet = new BookUpdateC2SPacket(
							slot, List.of(pages), Optional.of(title));

					held.setSubNbt("pages", pagesNbt);
					assert MinecraftClient.getInstance().player != null;
					MinecraftClient.getInstance().player.networkHandler.sendPacket(packet);
				} else if(action.equalsIgnoreCase("file")) {
					sendChat("Not implemented yet.");
				} else {
					sendChat("Available actions: ['file', 'random']");
				}

				return 1;
			})));
	}
}
