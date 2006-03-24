package org.openstreetmap.josm.tools;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Label that contains a clickable link.
 * @author Imi
 */
public class UrlLabel extends JEditorPane implements HyperlinkListener {

	public UrlLabel(String url) {
		setContentType("text/html");
		setText("<html><a href=\""+url+"\">"+url+"</a></html>");
		setEditable(false);
		setOpaque(false);
		addHyperlinkListener(this);
	}

	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			OpenBrowser.displayUrl("http://wiki.eigenheimstrasse.de/wiki/JOSM");
		}
	}
}
