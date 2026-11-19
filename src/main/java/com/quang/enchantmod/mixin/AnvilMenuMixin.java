package com.quang.enchantmod.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	@Redirect(
			method = "createResult",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;canStoreEnchantments(Lnet/minecraft/world/item/ItemStack;)Z"
			)
	)
	private boolean enchantmod$allowEnchantingEveryItem(ItemStack item) {
		return true;
	}

	@Redirect(
			method = "createResult",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/Enchantment;areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z"
			)
	)
	private boolean enchantmod$allowConflictingEnchantments(
			Holder<Enchantment> enchantment,
			Holder<Enchantment> existing
	) {
		return true;
	}

	@Redirect(
			method = "createResult",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"
			)
	)
	private int enchantmod$allowLevelsAboveVanillaLimit(Enchantment enchantment) {
		return Enchantment.MAX_LEVEL;
	}

	@Redirect(
			method = "createResult",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V"
			)
	)
	private void enchantmod$removeTooExpensiveLimit(DataSlot cost, int levelCost) {
		cost.set(Math.min(levelCost, 39));
	}

	@ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
	private int enchantmod$removeTooExpensiveRejection(int maximumCost) {
		return Integer.MAX_VALUE;
	}
}
