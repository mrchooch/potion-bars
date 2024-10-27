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
import net.runelite.client.events.ConfigChanged;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@PluginDescriptor (
	name = "Potion Storage Bars"
)

public class PotionBarPlugin extends Plugin  {
	java.util.logging.Logger logger =  java.util.logging.Logger.getLogger(this.getClass().getName());
	@Inject
	private Client client;

	@Inject
	private PotionBarConfig config;

	@Inject
	private ItemManager itemManager;

	class PotionPanel {
		public Widget item;
		public Widget dosesOriginal;
		public Widget dosesDisplay;
		public Widget foregroundBar;
		public Widget backgroundBar;
	};

	private List<PotionPanel> potionPanels;

	@Provides
	PotionBarConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(PotionBarConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if (configChanged.getGroup().equals(PotionBarConfig.GROUP)) {
			createProgressBars();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event) {
		int scriptId = event.getScriptId();

		//If Potion Storage is opened
		if (scriptId == ScriptID.POTIONSTORE_BUILD)  {
			createProgressBars();
		}

		if (scriptId == ScriptID.POTIONSTORE_DOSE_CHANGE)  {
			updateProgressBars();
		}
	}

	private void createProgressBars() {
		potionPanels = new ArrayList<>();

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

			//Create background of the bar
			Widget barBackground = w.createChild(WidgetType.RECTANGLE);
			barBackground.setFilled(true);
			barBackground.setOriginalX(wDoses.getOriginalX());
			barBackground.setOriginalY(wDoses.getOriginalY());
			barBackground.setOriginalWidth(145);
			barBackground.setOriginalHeight(13);
			barBackground.setTextColor(2169878);

			//Create foreground of the bar
			Widget barForeground = w.createChild(WidgetType.RECTANGLE);
			barForeground.setFilled(true);
			barForeground.setOriginalX(wDoses.getOriginalX());
			barForeground.setOriginalY(wDoses.getOriginalY());
			barForeground.setOriginalHeight(13);
			barForeground.setTextColor(colour);

			//Create a copy of the doses text because im an idiot that couldnt figure out how to reorder the widgets properly
			Widget text = w.createChild(WidgetType.TEXT);
			text.setText(wDoses.getText());
			text.setOriginalHeight(wDoses.getOriginalHeight());
			text.setOriginalWidth(wDoses.getOriginalWidth());
			text.setOriginalX((wDoses.getOriginalX()) + 2);
			text.setOriginalY((wDoses.getOriginalY()) + 1);
			text.setFontId((wDoses.getFontId()));
			text.setTextColor((wDoses.getTextColor()));
			text.setTextShadowed(true);

			text.revalidate();
			barForeground.revalidate();
			barBackground.revalidate();

			PotionPanel panel = new PotionPanel();
			panel.item = wItem;
			panel.dosesOriginal = wDoses;
			panel.dosesDisplay = text;
			panel.foregroundBar = barForeground;
			panel.backgroundBar = barBackground;
			potionPanels.add(panel);
		}

		updateProgressBars();
	}

	private void updateProgressBars() {
		for (PotionPanel panel : potionPanels) {
			//Update doses text
			panel.dosesDisplay.setText(panel.dosesOriginal.getText());

			//Figure out how wide the progress bar should be
			String str = panel.dosesOriginal.getText();
			int doseCount = Integer.parseInt(str.replace("Doses: ", ""));
			int barWidth = Math.min(Math.round(((float)doseCount/ config.barScale()) * 145), 145);
			panel.foregroundBar.setOriginalWidth(barWidth);

			panel.dosesDisplay.revalidate();
			panel.foregroundBar.revalidate();
		}
	}
}
