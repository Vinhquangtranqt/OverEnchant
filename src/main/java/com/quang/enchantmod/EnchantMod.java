package com.quang.enchantmod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class EnchantMod implements ModInitializer {
	private static final SuggestionProvider<CommandSourceStack> ENCHANTMENT_SUGGESTIONS =
			(context, builder) -> SharedSuggestionProvider.suggest(
					enchantments(context.getSource()).keySet().stream().map(Object::toString),
					builder
			);

	@Override
	public void onInitialize() {
		EnchantmentEvents.ALLOW_ENCHANTING.register(
				(enchantment, target, context) -> TriState.TRUE
		);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("overenchant")
						.then(literal("apply")
								.then(enchantmentArgument()
										.then(argument("level", IntegerArgumentType.integer(1, 255))
												.executes(context -> applyToHeldItem(
														context.getSource(),
														getEnchantment(context, enchantments(context.getSource())),
														IntegerArgumentType.getInteger(context, "level")
												)))))
						.then(literal("book")
								.then(enchantmentArgument()
										.then(argument("level", IntegerArgumentType.integer(1, 255))
												.executes(context -> giveBook(
														context.getSource(),
														getEnchantment(context, enchantments(context.getSource())),
														IntegerArgumentType.getInteger(context, "level")
												)))))
						.then(literal("books")
								.then(argument("level", IntegerArgumentType.integer(1, 255))
										.executes(context -> giveAllBooks(
												context.getSource(),
												enchantments(context.getSource()),
												IntegerArgumentType.getInteger(context, "level")
										))))
			);

			var giveCommand = dispatcher.getRoot().getChild("give");
			var giveTargets = giveCommand == null ? null : giveCommand.getChild("targets");
			if (giveTargets == null) {
				throw new IllegalStateException("Could not find the vanilla /give targets argument.");
			}

			giveTargets.addChild(Commands.literal("wind_bust_255")
					.executes(EnchantMod::giveWindBurstBook)
					.build());
		});
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> enchantmentArgument() {
		return argument("enchantment", StringArgumentType.word()).suggests(ENCHANTMENT_SUGGESTIONS);
	}

	private static Registry<Enchantment> enchantments(CommandSourceStack source) {
		return source.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
	}

	private static Holder<Enchantment> getEnchantment(
			CommandContext<CommandSourceStack> context,
			Registry<Enchantment> registry
	) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "enchantment");
		Identifier id = Identifier.tryParse(name);
		if (id == null) {
			throw new SimpleCommandExceptionType(Component.literal("Invalid enchantment ID: " + name)).create();
		}

		return registry.get(id).orElseThrow(
				() -> new SimpleCommandExceptionType(Component.literal("Unknown enchantment: " + id)).create()
		);
	}

	private static int applyToHeldItem(CommandSourceStack source, Holder<Enchantment> enchantment, int level)
			throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ItemStack item = player.getMainHandItem();
		if (item.isEmpty()) {
			source.sendFailure(Component.literal("Hold an item in your main hand first."));
			return 0;
		}

		ItemEnchantments.Mutable enchantments =
				new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(item));
		enchantments.set(enchantment, level);
		EnchantmentHelper.setEnchantments(item, enchantments.toImmutable());

		source.sendSuccess(
				() -> Component.literal("Applied " + enchantment.value().description().getString() + " "
						+ toRoman(level) + " to " + item.getHoverName().getString() + "."),
				false
		);
		return 1;
	}

	private static int giveBook(CommandSourceStack source, Holder<Enchantment> enchantment, int level)
			throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ItemStack book = createBook(enchantment, level);
		giveOrDrop(player, book);
		source.sendSuccess(
				() -> Component.literal("Gave " + enchantment.value().description().getString() + " "
						+ toRoman(level) + "."),
				false
		);
		return 1;
	}

	private static int giveWindBurstBook(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
		Holder<Enchantment> windBurst = enchantments(source)
				.get(Identifier.parse("minecraft:wind_burst"))
				.orElseThrow(() -> new IllegalStateException("Minecraft enchantment minecraft:wind_burst is missing."));
		ItemStack book = createBook(windBurst, 255);
		for (ServerPlayer player : players) {
			giveOrDrop(player, book.copy());
		}

		source.sendSuccess(
				() -> Component.literal("Gave Wind Burst CCLV to " + players.size() + " player(s)."),
				true
		);
		return players.size();
	}

	private static int giveAllBooks(
			CommandSourceStack source,
			Registry<Enchantment> registry,
			int level
	) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		List<ItemStack> books = new ArrayList<>();
		for (Holder.Reference<Enchantment> enchantment : registry.listElements().toList()) {
			books.add(createBook(enchantment, level));
		}

		for (ItemStack book : books) {
			giveOrDrop(player, book);
		}

		int count = books.size();
		source.sendSuccess(
				() -> Component.literal("Gave " + count + " enchanted books at level " + toRoman(level) + "."),
				false
		);
		return count;
	}

	private static ItemStack createBook(Holder<Enchantment> enchantment, int level) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		ItemEnchantments.Mutable storedEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		storedEnchantments.set(enchantment, level);
		book.set(DataComponents.STORED_ENCHANTMENTS, storedEnchantments.toImmutable());
		book.set(
				DataComponents.CUSTOM_NAME,
				Component.literal(enchantment.value().description().getString() + " " + toRoman(level))
		);
		return book;
	}

	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false, Prediction.PREDICTED);
		}
	}

	private static String toRoman(int number) {
		int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
		String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			while (number >= values[i]) {
				result.append(numerals[i]);
				number -= values[i];
			}
		}
		return result.toString();
	}
}
