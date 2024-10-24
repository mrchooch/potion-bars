package com.potionbar;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("potionStorageBars")
public interface PotionBarConfig extends Config
{
	@ConfigItem(
		keyName = "barScale",
		name = "Full Bar Doses",
		description = "The amount of doses to fill a bar"
	)
	default int barScale()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "barColours",
			name = "Potion Coloured Bars",
			description = "The colour of a bar will match it's potion"
	)
	default boolean barColours()
	{
		return true;
	}
}
