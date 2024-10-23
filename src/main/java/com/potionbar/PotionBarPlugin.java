package com.potionbar;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.widgets.Widget;
import com.google.common.base.Strings;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.WidgetType;

@Slf4j
@PluginDescriptor (
	name = "Potion Storage Bars"
)

public class PotionBarPlugin extends Plugin  {
	@Inject
	private Client client;

	@Inject
	private PotionBarConfig config;

	@Inject
	private ItemManager itemManager;

	@Provides
	PotionBarConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(PotionBarConfig.class);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event) {
		int scriptId = event.getScriptId();

		//If Potion Storage is opened or updated
		if (scriptId == ScriptID.POTIONSTORE_BUILD || scriptId == ScriptID.POTIONSTORE_DOSE_CHANGE || scriptId == ScriptID.POTIONSTORE_DOSES || scriptId == ScriptID.POTIONSTORE_WITHDRAW_DOSES)  {
			Widget w = client.getWidget(ComponentID.BANK_POTIONSTORE_CONTENT);
			Widget[] children = w.getDynamicChildren();

			//Iterate through all potions in storage
			for (int i = 0; i + 4 < children.length; i += 5) {
				Widget wItem = children[i + 1];
				Widget wDoses = children[i + 3];

				//Skip past errors and unfinished potions
				if (wItem.getItemId() == -1 || Strings.isNullOrEmpty(wDoses.getText()) || wItem.getName().contains("(unf)")) {
					continue;
				}

				//Get colour to set foreground bar to, either potion colour or green
				int colour;
				if (config.barColours()) {
					colour = itemManager.getImage(wItem.getItemId()).getRGB(13, 16);
				} else {
					colour = 30770;
				}

				buildProgressBar(w, wDoses, colour);
			}
		}
	}

	private void buildProgressBar(Widget parent, Widget doses, int colour) {
		//Create background of the bar
		Widget barBackground = parent.createChild(WidgetType.RECTANGLE);
		barBackground.setFilled(true);
		barBackground.setOriginalX(doses.getOriginalX());
		barBackground.setOriginalY(doses.getOriginalY());
		barBackground.setOriginalWidth(145);
		barBackground.setOriginalHeight(13);
		barBackground.setTextColor(2169878);

		//Create foreground of the bar
		Widget barForeground = parent.createChild(WidgetType.RECTANGLE);
		barForeground.setFilled(true);
		barForeground.setOriginalX(doses.getOriginalX());
		barForeground.setOriginalY(doses.getOriginalY());
		barForeground.setOriginalHeight(13);
		barForeground.setTextColor(colour);

		//Figure out how wide the progress bar should be
		String str = doses.getText();
		int doseCount = Integer.parseInt(str.replace("Doses: ", ""));
		int barWidth = Math.min(Math.round(((float)doseCount/ config.barScale()) * 145), 145);
		barForeground.setOriginalWidth(barWidth);

		//Create a copy of the doses text because im an idiot that couldnt figure out how to reorder the widgets properly
		Widget text = parent.createChild(WidgetType.TEXT);
		text.setText(doses.getText());
		text.setOriginalHeight(doses.getOriginalHeight());
		text.setOriginalWidth(doses.getOriginalWidth());
		text.setOriginalX((doses.getOriginalX()) + 2);
		text.setOriginalY((doses.getOriginalY()) + 1);
		text.setFontId((doses.getFontId()));
		text.setTextColor((doses.getTextColor()));
		text.setTextShadowed(true);


	}
}
