// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

import au.com.objectix.jgridshift.NTv2GridShift;

/**
 * Datum based of NTV2 grid shift file.
 */
public class NTv2Datum extends AbstractDatum {

    protected NTv2GridShiftFileWrapper nadgrids;

    public NTv2Datum(String name, String proj4Id, Ellipsoid ellps, NTv2GridShiftFileWrapper nadgrids) {
        super(name, proj4Id, ellps);
        this.nadgrids = nadgrids;
    }

    @Override
    public LatLon toWGS84(LatLon ll) {
        NTv2GridShift gs = new NTv2GridShift(ll.lat(), ll.lon());
        nadgrids.getShiftFile().gridShiftForward(gs);
        return new LatLon(ll.lat() + gs.getLatShiftDegrees(), ll.lon() + gs.getLonShiftPositiveEastDegrees());
    }

    @Override
    public LatLon fromWGS84(LatLon ll) {
        NTv2GridShift gs = new NTv2GridShift(ll.lat(), ll.lon());
        nadgrids.getShiftFile().gridShiftReverse(gs);
        return new LatLon(ll.lat() + gs.getLatShiftDegrees(), ll.lon() + gs.getLonShiftPositiveEastDegrees());
    }
}
