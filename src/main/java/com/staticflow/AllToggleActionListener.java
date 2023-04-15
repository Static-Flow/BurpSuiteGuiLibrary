package main.java.com.staticflow;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static main.java.com.staticflow.BurpGuiControl.initializeCustomSettingsMenu;

/**
 * This custom {@link ActionListener} attaches to the "All" Settings filter button and calls {@link BurpGuiControl#initializeCustomSettingsMenu} when clicked.
 */
public class AllToggleActionListener implements ActionListener {

    private final String name;
    private final Component settingsTree;

    public AllToggleActionListener(String name, Component settingsTree) {
        this.name = name;
        this.settingsTree = settingsTree;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        initializeCustomSettingsMenu(name,settingsTree);
    }
}
