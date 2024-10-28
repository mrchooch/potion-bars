package com.potionbar;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(PotionBarConfig.GROUP)
public interface PotionBarConfig extends Config  {
	String GROUP = "potionBars";

	@ConfigItem(
		keyName = "barScale",
		name = "Full Bar Doses",
		description = "The amount of doses to fill a bar",
			position = 1
	)
	default int barScale() {
		return 100;
	}

	@ConfigItem(
			keyName = "barColours",
			name = "Potion Coloured Bars",
			description = "The colour of a bar will match its' potion",
			position = 2
	)
	default boolean barColours() {
		return true;
	}

	@ConfigItem(
			keyName = "doseDisplay",
			name = "Count Full Potions",
			description = "Instead of doses, show the number of 4 dose potions",
			position = 3
	)
	default boolean doseDisplay() {
		return false;
	}
}
