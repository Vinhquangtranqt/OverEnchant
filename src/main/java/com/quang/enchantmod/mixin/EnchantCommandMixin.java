package com.quang.enchantmod.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.server.commands.EnchantCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(EnchantCommand.class)
public class EnchantCommandMixin {
	@Redirect(
			method = "enchant",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
			)
	)
	private static ItemStack enchantmod$convertBookToEnchantedBook(LivingEntity target) {
		ItemStack item = target.getMainHandItem();
		if (item.is(Items.BOOK)) {
			ItemStack enchantedBook = item.transmuteCopy(Items.ENCHANTED_BOOK);
			target.setItemSlot(EquipmentSlot.MAINHAND, enchantedBook);
			return enchantedBook;
		}

		return item;
	}

	@Redirect(
			method = "enchant",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"
			)
	)
	private static int enchantmod$allowLevel255(Enchantment enchantment) {
		return Enchantment.MAX_LEVEL;
	}

	@Redirect(
			method = "enchant",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;isEnchantmentCompatible(Ljava/util/Collection;Lnet/minecraft/core/Holder;)Z"
			)
	)
	private static boolean enchantmod$allowConflictingEnchantments(
			Collection<Holder<Enchantment>> existing,
			Holder<Enchantment> enchantment
	) {
		return true;
	}

	@Redirect(
			method = "enchant",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentsForCrafting(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/enchantment/ItemEnchantments;"
			)
	)
	private static ItemEnchantments enchantmod$getStoredBookEnchantments(ItemStack item) {
		if (item.is(Items.BOOK) || item.is(Items.ENCHANTED_BOOK)) {
			return item.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		}

		return EnchantmentHelper.getEnchantmentsForCrafting(item);
	}

	@Redirect(
			method = "enchant",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/ItemStack;enchant(Lnet/minecraft/core/Holder;I)V"
			)
	)
	private static void enchantmod$storeEnchantmentsOnBooks(
			ItemStack item,
			Holder<Enchantment> enchantment,
			int level
	) {
		if (item.is(Items.BOOK) || item.is(Items.ENCHANTED_BOOK)) {
			ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(
					item.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
			);
			stored.set(enchantment, level);
			item.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
			return;
		}

		item.enchant(enchantment, level);
	}
}
