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
import net.runelite.client.callback.ClientThread;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@PluginDescriptor (
	name = "Potion Storage Bars"
)

public class PotionBarPlugin extends Plugin  {
	@Inject
	private ClientThread clientThread;

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
	}

	private List<PotionPanel> potionPanels;

	@Provides
	PotionBarConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(PotionBarConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if (configChanged.getGroup().equals(PotionBarConfig.GROUP)) {
			clientThread.invokeLater(this::updateProgressBars);
		}
	}

	@Override
	protected void shutDown() throws Exception  {
		potionPanels.clear();
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

			//Skip past errors
			if (wItem.getItemId() == -1 || Strings.isNullOrEmpty(wDoses.getText())) {
				continue;
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

			//Create a copy of the doses text because im an idiot that couldnt figure out how to reorder the widgets properly
			Widget text = w.createChild(WidgetType.TEXT);
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
			//Figure out how wide the progress bar should be
			String str = panel.dosesOriginal.getText();


			boolean isUnf = panel.item.getName().contains("(unf)");
			boolean isMix = panel.item.getName().contains("mix");
			boolean isWeaponPoison = panel.item.getName().contains("Weapon poison");

			int fullDoses;
			if (isUnf) {
				fullDoses = 1;
			} else if (isMix) {
				fullDoses = 2;
			} else if (isWeaponPoison) {
				fullDoses = 1;
			} else {
				fullDoses = 4;
			}

			//Get how many doses
			int doseCount;
			if (isUnf || isWeaponPoison) {
				doseCount = Integer.parseInt(str.replace("Quantity: ", ""));
			} else {
				doseCount = Integer.parseInt(str.replace("Doses: ", ""));
			}

			int barWidth = Math.round(((float)(doseCount)/ config.barScale()) * 145);
			panel.foregroundBar.setOriginalWidth(Math.min(barWidth, 145));

			//Set colour of bar
			int colour;
			if (config.barColours()) {
				colour = itemManager.getImage(panel.item.getItemId()).getRGB(13, 24);
			} else {
				colour = 30770;
			}
			panel.foregroundBar.setTextColor(colour);

			//Update doses text
			String doseText = "";
			int potCount = (int)Math.floor((float)doseCount/(float)fullDoses);

			switch(config.doseDisplay()) {
				case POTS:
					doseText = "Pots: " + potCount;
					break;
				case DOSES:
					doseText = "Doses: " + doseCount;
					break;

				case POTS_AND_DOSES:
					doseText = "Pots: " + potCount + " (" + doseCount + ")";
					break;

				case DOSES_AND_POTS:
					doseText = "Doses: " + doseCount + " (" + potCount + ")";
					break;
			}

			panel.dosesDisplay.setText(doseText);

			panel.dosesDisplay.revalidate();
			panel.foregroundBar.revalidate();
		}
	}
}
