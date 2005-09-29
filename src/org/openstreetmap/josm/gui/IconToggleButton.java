package org.openstreetmap.josm.gui;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

/**
 * Just a toggle button, with smaller border and icon only to display in
 * MapFrame toolbars.
 *
 * @author imi
 */
public class IconToggleButton extends JToggleButton {

	/**
	 * Construct the toggle button with the given action.
	 */
	public IconToggleButton(JComponent acceleratorReceiver, Action action) {
		super(action);
		setText(null);
		Object o = action.getValue(Action.LONG_DESCRIPTION);
		if (o != null)
			setToolTipText(o.toString());
	}
}
