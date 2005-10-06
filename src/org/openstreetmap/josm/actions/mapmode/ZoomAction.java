package org.openstreetmap.josm.actions.mapmode;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Layer;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;

/**
 * Enable the zoom mode within the MapFrame. 
 * 
 * Holding down the left mouse button select a rectangle with the same aspect 
 * ratio than the current layer.
 * Holding down left and right let the user move the former selected rectangle.
 * Releasing the left button zoom to the selection.
 * 
 * Rectangle selections with either height or width smaller than 3 pixels 
 * are ignored.
 * 
 * @author imi
 */
public class ZoomAction extends MapMode implements SelectionEnded {

	/**
	 * Shortcut to the mapview.
	 */
	private final Layer mv;
	/**
	 * Manager that manages the selection rectangle with the aspect ratio of the
	 * Layer.
	 */
	private final SelectionManager selectionManager;


	/**
	 * Construct a ZoomAction without a label.
	 * @param mapFrame The MapFrame, whose zoom mode should be enabled.
	 */
	public ZoomAction(MapFrame mapFrame) {
		super("Zoom", "zoom", "Zoom in by dragging", KeyEvent.VK_Z, mapFrame);
		mv = mapFrame.layer;
		selectionManager = new SelectionManager(this, true, mv);
	}

	/**
	 * Zoom to the rectangle on the map.
	 */
	public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
		if (r.width >= 3 && r.height >= 3) {
			double scale = mv.getScale() * r.getWidth()/mv.getWidth();
			GeoPoint newCenter = mv.getPoint(r.x+r.width/2, r.y+r.height/2, false);
			mv.zoomTo(newCenter, scale);
		}
	}

	@Override
	public void registerListener() {
		super.registerListener();
		selectionManager.register(mv);
	}

	@Override
	public void unregisterListener() {
		super.unregisterListener();
		selectionManager.unregister(mv);
	}
}
