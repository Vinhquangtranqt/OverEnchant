# OverEnchant

A Fabric mod for Minecraft Java Edition 26.3. Apply any registered enchantment
to any item at levels 1 through 255, including combinations vanilla normally
rejects.

## Commands

- `/overenchant apply <enchantment> <level>` applies an enchantment to the item
  in your main hand. Applying more enchantments preserves the existing ones.
- `/overenchant book <enchantment> <level>` gives you a named enchanted book.
- `/overenchant books <level>` gives you one book for every enchantment
  registered in the current game, including enchantments added by other mods.
- `/enchant @s minecraft:wind_burst 255` uses vanilla syntax to enchant the
  item in your main hand. The mod removes vanilla's level, item-support, and
  enchantment-conflict restrictions for this command.
- Using `/enchant` while holding either a regular book or an enchanted book
  adds or replaces that enchantment in its stored enchantments, preserving
  other enchantments. A regular book becomes an enchanted book.
- `/give @s wind_bust_255` gives a Wind Burst 255 enchanted book.

Use namespaced IDs such as `minecraft:sharpness` and levels from `1` to `255`.
The enchantment argument provides suggestions as you type. Book names use
Roman numerals, for example `Fire Aspect CCLV`.

Anvils accept incompatible enchantments and items that vanilla does not
normally support. Their displayed XP cost is capped at 39, so the anvil never
shows "Too Expensive."

## Build

Run `gradlew.bat build` on Windows. The mod jar is created in
`build/libs/`. Install it with Fabric Loader 0.20.1 or later and Fabric API
for Minecraft 26.3.
