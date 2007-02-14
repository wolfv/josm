package org.openstreetmap.josm.gui.layer.markerlayer;

import java.util.Map;

import org.openstreetmap.josm.data.coor.LatLon;

/**
 * This interface has to be implemented by anyone who wants to create markers.
 * 
 * When reading a gpx file, all implementations of MarkerMaker registered with 
 * the Marker are consecutively called until one returns a Marker object.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 */
public interface MarkerProducers {
	/**
	 * Returns a Marker object if this implementation wants to create one for the
	 * given input data, or <code>null</code> otherwise.
	 * 
	 * @param ll lat/lon for the marker position
	 * @param data A map of all tags found in the <wpt> node of the gpx file. 
	 * @return A Marker object, or <code>null</code>.
	 */
	public Marker createMarker(LatLon ll, Map<String,String> data);
}
