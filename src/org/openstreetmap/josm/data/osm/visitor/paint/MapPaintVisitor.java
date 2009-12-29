/* License: GPL. Copyright 2007 by Immanuel Scholz and others */
package org.openstreetmap.josm.data.osm.visitor.paint;

/* To enable debugging or profiling remove the double / signs */

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.IconElemStyle;
import org.openstreetmap.josm.gui.mappaint.LineElemStyle;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.SimpleNodeElemStyle;

public class MapPaintVisitor implements PaintVisitor {

    private Graphics2D g;
    private NavigatableComponent nc;

    private boolean zoomLevelDisplay;
    private boolean drawMultipolygon;
    private boolean drawRestriction;
    private boolean leftHandTraffic;
    private ElemStyles.StyleSet styles;
    private double circum;
    private double dist;
    private boolean useStyleCache;
    private static int paintid = 0;
    private EastNorth minEN;
    private EastNorth maxEN;
    private MapPainter painter;
    private MapPaintSettings paintSettings;

    private boolean inactive;

    protected boolean isZoomOk(ElemStyle e) {
        if (!zoomLevelDisplay) /* show everything if the user wishes so */
            return true;

        if(e == null) /* the default for things that don't have a rule (show, if scale is smaller than 1500m) */
            return (circum < 1500);

        return !(circum >= e.maxScale || circum < e.minScale);
    }

    public ElemStyle getPrimitiveStyle(OsmPrimitive osm) {
        if(!useStyleCache)
            return ((styles != null) ? styles.get(osm) : null);

        if(osm.mappaintStyle == null && styles != null) {
            osm.mappaintStyle = styles.get(osm);
            if(osm instanceof Way) {
                ((Way)osm).isMappaintArea = styles.isArea(osm);
            }
        }

        if (osm.mappaintStyle == null && osm instanceof Node) {
            osm.mappaintStyle = SimpleNodeElemStyle.INSTANCE;
        }

        if (osm.mappaintStyle == null && osm instanceof Way) {
            osm.mappaintStyle = LineElemStyle.UNTAGGED_WAY;
        }

        return osm.mappaintStyle;
    }

    public IconElemStyle getPrimitiveNodeStyle(OsmPrimitive osm) {
        if(!useStyleCache)
            return (styles != null) ? styles.getIcon(osm) : null;

            if(osm.mappaintStyle == null && styles != null) {
                osm.mappaintStyle = styles.getIcon(osm);
            }

            return (IconElemStyle)osm.mappaintStyle;
    }

    public boolean isPrimitiveArea(Way osm) {
        if(!useStyleCache)
            return styles.isArea(osm);

        if(osm.mappaintStyle == null && styles != null) {
            osm.mappaintStyle = styles.get(osm);
            osm.isMappaintArea = styles.isArea(osm);
        }
        return osm.isMappaintArea;
    }

    public void drawNode(Node n) {
        /* check, if the node is visible at all */
        if((n.getEastNorth().east()  > maxEN.east() ) ||
                (n.getEastNorth().north() > maxEN.north()) ||
                (n.getEastNorth().east()  < minEN.east() ) ||
                (n.getEastNorth().north() < minEN.north()))
            return;

        ElemStyle nodeStyle = getPrimitiveStyle(n);

        if (isZoomOk(nodeStyle)) {
            nodeStyle.paintPrimitive(n, paintSettings, painter, n.isSelected());
        }
    }

    public void drawWay(Way w, int fillAreas) {
        if(w.getNodesCount() < 2)
            return;

        if (w.hasIncompleteNodes())
            return;

        /* check, if the way is visible at all */
        double minx = 10000;
        double maxx = -10000;
        double miny = 10000;
        double maxy = -10000;

        for (Node n : w.getNodes())
        {
            if(n.getEastNorth().east() > maxx) {
                maxx = n.getEastNorth().east();
            }
            if(n.getEastNorth().north() > maxy) {
                maxy = n.getEastNorth().north();
            }
            if(n.getEastNorth().east() < minx) {
                minx = n.getEastNorth().east();
            }
            if(n.getEastNorth().north() < miny) {
                miny = n.getEastNorth().north();
            }
        }

        if ((minx > maxEN.east()) ||
                (miny > maxEN.north()) ||
                (maxx < minEN.east()) ||
                (maxy < minEN.north()))
            return;

        ElemStyle wayStyle = getPrimitiveStyle(w);

        if(!isZoomOk(wayStyle))
            return;

        if (wayStyle == null) {
            wayStyle = LineElemStyle.UNTAGGED_WAY;
        }

        if(wayStyle instanceof LineElemStyle) {
            wayStyle.paintPrimitive(w, paintSettings, painter, data.isSelected(w));
        } else if (wayStyle instanceof AreaElemStyle) {
            AreaElemStyle areaStyle = (AreaElemStyle) wayStyle;
            /* way with area style */
            if (fillAreas > dist)
            {
                painter.drawArea(getPolygon(w), (data.isSelected(w) ? paintSettings.getSelectedColor() : areaStyle.color), painter.getWayName(w));
                if(!w.isClosed()) {
                    putError(w, tr("Area style way is not closed."), true);
                }
            }
            areaStyle.getLineStyle().paintPrimitive(w, paintSettings, painter, data.isSelected(w));
        }
    }

    public Collection<PolyData> joinWays(Collection<Way> join, OsmPrimitive errs)
    {
        Collection<PolyData> res = new LinkedList<PolyData>();
        Way[] joinArray = join.toArray(new Way[join.size()]);
        int left = join.size();
        while(left != 0)
        {
            Way w = null;
            boolean selected = false;
            List<Node> n = null;
            boolean joined = true;
            while(joined && left != 0)
            {
                joined = false;
                for(int i = 0; i < joinArray.length && left != 0; ++i)
                {
                    if(joinArray[i] != null)
                    {
                        Way c = joinArray[i];
                        if(w == null)
                        { w = c; selected = data.isSelected(w); joinArray[i] = null; --left; }
                        else
                        {
                            int mode = 0;
                            int cl = c.getNodesCount()-1;
                            int nl;
                            if(n == null)
                            {
                                nl = w.getNodesCount()-1;
                                if(w.getNode(nl) == c.getNode(0)) {
                                    mode = 21;
                                } else if(w.getNode(nl) == c.getNode(cl)) {
                                    mode = 22;
                                } else if(w.getNode(0) == c.getNode(0)) {
                                    mode = 11;
                                } else if(w.getNode(0) == c.getNode(cl)) {
                                    mode = 12;
                                }
                            }
                            else
                            {
                                nl = n.size()-1;
                                if(n.get(nl) == c.getNode(0)) {
                                    mode = 21;
                                } else if(n.get(0) == c.getNode(cl)) {
                                    mode = 12;
                                } else if(n.get(0) == c.getNode(0)) {
                                    mode = 11;
                                } else if(n.get(nl) == c.getNode(cl)) {
                                    mode = 22;
                                }
                            }
                            if(mode != 0)
                            {
                                joinArray[i] = null;
                                joined = true;
                                if(data.isSelected(c)) {
                                    selected = true;
                                }
                                --left;
                                if(n == null) {
                                    n = w.getNodes();
                                }
                                n.remove((mode == 21 || mode == 22) ? nl : 0);
                                if(mode == 21) {
                                    n.addAll(c.getNodes());
                                } else if(mode == 12) {
                                    n.addAll(0, c.getNodes());
                                } else if(mode == 22)
                                {
                                    for(Node node : c.getNodes()) {
                                        n.add(nl, node);
                                    }
                                }
                                else /* mode == 11 */
                                {
                                    for(Node node : c.getNodes()) {
                                        n.add(0, node);
                                    }
                                }
                            }
                        }
                    }
                } /* for(i = ... */
            } /* while(joined) */
            if(n != null)
            {
                w = new Way(w);
                w.setNodes(n);
            }
            if(!w.isClosed())
            {
                if(errs != null)
                {
                    putError(errs, tr("multipolygon way ''{0}'' is not closed.",
                            w.getDisplayName(DefaultNameFormatter.getInstance())), true);
                }
            }
            PolyData pd = new PolyData(w);
            pd.selected = selected;
            res.add(pd);
        } /* while(left != 0) */

        return res;
    }

    public void drawSelectedMember(OsmPrimitive osm, ElemStyle style, boolean area,
            boolean areaselected)
    {
        if(osm instanceof Way)
        {
            if(style instanceof AreaElemStyle) {
                Way way = (Way)osm;
                AreaElemStyle areaStyle = (AreaElemStyle)style;
                areaStyle.getLineStyle().paintPrimitive(way, paintSettings, painter, true);
                if(area) {
                    painter.drawArea(getPolygon(way), (areaselected ? paintSettings.getSelectedColor() : areaStyle.color), painter.getWayName(way));
                }
            } else {
                style.paintPrimitive(osm, paintSettings, painter, true);
            }
        }
        else if(osm instanceof Node)
        {
            if(isZoomOk(style)) {
                style.paintPrimitive(osm, paintSettings, painter, true);
            }
        }
        osm.mappaintDrawnCode = paintid;
    }

    public void paintUnselectedRelation(Relation r) {

        if (drawMultipolygon && "multipolygon".equals(r.get("type")))
        {
            if(drawMultipolygon(r))
                return;
        }
        else if (drawRestriction && "restriction".equals(r.get("type")))
        {
            drawRestriction(r);
        }

        if(data.isSelected(r)) /* draw ways*/
        {
            for (RelationMember m : r.getMembers())
            {
                if (m.isWay() && m.getMember().isDrawable())
                {
                    drawSelectedMember(m.getMember(), styles != null ? getPrimitiveStyle(m.getMember())
                            : null, true, true);
                }
            }
        }
    }

    public void drawRestriction(Relation r) {
        Way fromWay = null;
        Way toWay = null;
        OsmPrimitive via = null;

        /* find the "from", "via" and "to" elements */
        for (RelationMember m : r.getMembers())
        {
            if (m.getMember().isDeleted()) {
                putError(r, tr("Deleted member ''{0}'' in relation.",
                        m.getMember().getDisplayName(DefaultNameFormatter.getInstance())), true);
            } else if(m.getMember().isIncomplete())
                return;
            else
            {
                if(m.isWay())
                {
                    Way w = m.getWay();
                    if(w.getNodesCount() < 2)
                    {
                        putError(r, tr("Way ''{0}'' with less than two points.",
                                w.getDisplayName(DefaultNameFormatter.getInstance())), true);
                    }
                    else if("from".equals(m.getRole())) {
                        if(fromWay != null) {
                            putError(r, tr("More than one \"from\" way found."), true);
                        } else {
                            fromWay = w;
                        }
                    } else if("to".equals(m.getRole())) {
                        if(toWay != null) {
                            putError(r, tr("More than one \"to\" way found."), true);
                        } else {
                            toWay = w;
                        }
                    } else if("via".equals(m.getRole())) {
                        if(via != null) {
                            putError(r, tr("More than one \"via\" found."), true);
                        } else {
                            via = w;
                        }
                    } else {
                        putError(r, tr("Unknown role ''{0}''.", m.getRole()), true);
                    }
                }
                else if(m.isNode())
                {
                    Node n = m.getNode();
                    if("via".equals(m.getRole()))
                    {
                        if(via != null) {
                            putError(r, tr("More than one \"via\" found."), true);
                        } else {
                            via = n;
                        }
                    } else {
                        putError(r, tr("Unknown role ''{0}''.", m.getRole()), true);
                    }
                } else {
                    putError(r, tr("Unknown member type for ''{0}''.", m.getMember().getDisplayName(DefaultNameFormatter.getInstance())), true);
                }
            }
        }

        if (fromWay == null) {
            putError(r, tr("No \"from\" way found."), true);
            return;
        }
        if (toWay == null) {
            putError(r, tr("No \"to\" way found."), true);
            return;
        }
        if (via == null) {
            putError(r, tr("No \"via\" node or way found."), true);
            return;
        }

        Node viaNode;
        if(via instanceof Node)
        {
            viaNode = (Node) via;
            if(!fromWay.isFirstLastNode(viaNode)) {
                putError(r, tr("The \"from\" way doesn't start or end at a \"via\" node."), true);
                return;
            }
            if(!toWay.isFirstLastNode(viaNode)) {
                putError(r, tr("The \"to\" way doesn't start or end at a \"via\" node."), true);
            }
        }
        else
        {
            Way viaWay = (Way) via;
            Node firstNode = viaWay.firstNode();
            Node lastNode = viaWay.lastNode();
            boolean onewayvia = false;

            String onewayviastr = viaWay.get("oneway");
            if(onewayviastr != null)
            {
                if("-1".equals(onewayviastr)) {
                    onewayvia = true;
                    Node tmp = firstNode;
                    firstNode = lastNode;
                    lastNode = tmp;
                } else {
                    onewayvia = OsmUtils.getOsmBoolean(onewayviastr);
                }
            }

            if(fromWay.isFirstLastNode(firstNode)) {
                viaNode = firstNode;
            } else if (!onewayvia && fromWay.isFirstLastNode(lastNode)) {
                viaNode = lastNode;
            } else {
                putError(r, tr("The \"from\" way doesn't start or end at the \"via\" way."), true);
                return;
            }
            if(!toWay.isFirstLastNode(viaNode == firstNode ? lastNode : firstNode)) {
                putError(r, tr("The \"to\" way doesn't start or end at the \"via\" way."), true);
            }
        }

        /* find the "direct" nodes before the via node */
        Node fromNode = null;
        if(fromWay.firstNode() == via) {
            fromNode = fromWay.getNode(1);
        } else {
            fromNode = fromWay.getNode(fromWay.getNodesCount()-2);
        }

        Point pFrom = nc.getPoint(fromNode);
        Point pVia = nc.getPoint(viaNode);

        /* starting from via, go back the "from" way a few pixels
           (calculate the vector vx/vy with the specified length and the direction
           away from the "via" node along the first segment of the "from" way)
         */
        double distanceFromVia=14;
        double dx = (pFrom.x >= pVia.x) ? (pFrom.x - pVia.x) : (pVia.x - pFrom.x);
        double dy = (pFrom.y >= pVia.y) ? (pFrom.y - pVia.y) : (pVia.y - pFrom.y);

        double fromAngle;
        if(dx == 0.0) {
            fromAngle = Math.PI/2;
        } else {
            fromAngle = Math.atan(dy / dx);
        }
        double fromAngleDeg = Math.toDegrees(fromAngle);

        double vx = distanceFromVia * Math.cos(fromAngle);
        double vy = distanceFromVia * Math.sin(fromAngle);

        if(pFrom.x < pVia.x) {
            vx = -vx;
        }
        if(pFrom.y < pVia.y) {
            vy = -vy;
        }

        /* go a few pixels away from the way (in a right angle)
           (calculate the vx2/vy2 vector with the specified length and the direction
           90degrees away from the first segment of the "from" way)
         */
        double distanceFromWay=10;
        double vx2 = 0;
        double vy2 = 0;
        double iconAngle = 0;

        if(pFrom.x >= pVia.x && pFrom.y >= pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg - 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 90));
            }
            iconAngle = 270+fromAngleDeg;
        }
        if(pFrom.x < pVia.x && pFrom.y >= pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg));
            } else {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 180));
            }
            iconAngle = 90-fromAngleDeg;
        }
        if(pFrom.x < pVia.x && pFrom.y < pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg - 90));
            }
            iconAngle = 90+fromAngleDeg;
        }
        if(pFrom.x >= pVia.x && pFrom.y < pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 180));
            } else {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg));
            }
            iconAngle = 270-fromAngleDeg;
        }

        IconElemStyle nodeStyle = getPrimitiveNodeStyle(r);

        if (nodeStyle == null) {
            putError(r, tr("Style for restriction {0} not found.", r.get("restriction")), true);
            return;
        }

        painter.drawRestriction(inactive || r.isDisabled() ? nodeStyle.getDisabledIcon() : nodeStyle.icon,
                pVia, vx, vx2, vy, vy2, iconAngle, data.isSelected(r));
    }

    class PolyData {
        public Polygon poly = new Polygon();
        public Way way;
        public boolean selected = false;
        private Point p = null;
        private Collection<Polygon> inner = null;
        PolyData(Way w)
        {
            way = w;
            for (Node n : w.getNodes())
            {
                p = nc.getPoint(n);
                poly.addPoint(p.x,p.y);
            }
        }
        public int contains(Polygon p)
        {
            int contains = p.npoints;
            for(int i = 0; i < p.npoints; ++i)
            {
                if(poly.contains(p.xpoints[i],p.ypoints[i])) {
                    --contains;
                }
            }
            if(contains == 0) return 1;
            if(contains == p.npoints) return 0;
            return 2;
        }
        public void addInner(Polygon p)
        {
            if(inner == null) {
                inner = new ArrayList<Polygon>();
            }
            inner.add(p);
        }
        public boolean isClosed()
        {
            return (poly.npoints >= 3
                    && poly.xpoints[0] == poly.xpoints[poly.npoints-1]
                                                       && poly.ypoints[0] == poly.ypoints[poly.npoints-1]);
        }
        public Polygon get()
        {
            if(inner != null)
            {
                for (Polygon pp : inner)
                {
                    for(int i = 0; i < pp.npoints; ++i) {
                        poly.addPoint(pp.xpoints[i],pp.ypoints[i]);
                    }
                    poly.addPoint(p.x,p.y);
                }
                inner = null;
            }
            return poly;
        }
    }
    void addInnerToOuters(Relation r, boolean incomplete, PolyData pdInner, LinkedList<PolyData> outerPolygons)
    {
        Way wInner = pdInner.way;
        if(wInner != null && !wInner.isClosed())
        {
            Point pInner = nc.getPoint(wInner.getNode(0));
            pdInner.poly.addPoint(pInner.x,pInner.y);
        }
        PolyData o = null;
        for (PolyData pdOuter : outerPolygons)
        {
            int c = pdOuter.contains(pdInner.poly);
            if(c >= 1)
            {
                if(c > 1 && pdOuter.way != null && pdOuter.way.isClosed())
                {
                    putError(r, tr("Intersection between ways ''{0}'' and ''{1}''.",
                            pdOuter.way.getDisplayName(DefaultNameFormatter.getInstance()), wInner.getDisplayName(DefaultNameFormatter.getInstance())), true);
                }
                if(o == null || o.contains(pdOuter.poly) > 0) {
                    o = pdOuter;
                }
            }
        }
        if(o == null)
        {
            if(!incomplete)
            {
                putError(r, tr("Inner way ''{0}'' is outside.",
                        wInner.getDisplayName(DefaultNameFormatter.getInstance())), true);
            }
            o = outerPolygons.get(0);
        }
        o.addInner(pdInner.poly);
    }

    public boolean drawMultipolygon(Relation r) {
        Collection<Way> inner = new LinkedList<Way>();
        Collection<Way> outer = new LinkedList<Way>();
        Collection<Way> innerclosed = new LinkedList<Way>();
        Collection<Way> outerclosed = new LinkedList<Way>();
        boolean incomplete = false;
        boolean drawn = false;

        // Fill inner and outer list with valid ways
        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isDeleted()) {
                putError(r, tr("Deleted member ''{0}'' in relation.",
                        m.getMember().getDisplayName(DefaultNameFormatter.getInstance())), true);
            } else if (m.getMember().isIncomplete()) {
                incomplete = true;
            } else if(m.getMember().isDrawable()) {
                if(m.isWay()) {
                    Way w = m.getWay();
                    if(w.getNodesCount() < 2) {
                        putError(r, tr("Way ''{0}'' with less than two points.",
                                w.getDisplayName(DefaultNameFormatter.getInstance())), true);
                    }
                    else if("inner".equals(m.getRole())) {
                        inner.add(w);
                    } else if("outer".equals(m.getRole())) {
                        outer.add(w);
                    } else {
                        putError(r, tr("No useful role ''{0}'' for Way ''{1}''.",
                                m.getRole(), w.getDisplayName(DefaultNameFormatter.getInstance())), true);
                        if(!m.hasRole()) {
                            outer.add(w);
                        } else if(data.isSelected(r)) {
                            // TODO Is this necessary?
                            drawSelectedMember(m.getMember(), styles != null
                                    ? getPrimitiveStyle(m.getMember()) : null, true, true);
                        }
                    }
                }
                else
                {
                    putError(r, tr("Non-Way ''{0}'' in multipolygon.",
                            m.getMember().getDisplayName(DefaultNameFormatter.getInstance())), true);
                }
            }
        }

        ElemStyle wayStyle = getPrimitiveStyle(r);

        // If area style was not found for relation then use style of ways
        if(styles != null && !(wayStyle instanceof AreaElemStyle)) {
            for (Way w : outer) {
                wayStyle = styles.getArea(w);
                if(wayStyle != null) {
                    break;
                }
            }
            r.mappaintStyle = wayStyle;
        }

        if (wayStyle instanceof AreaElemStyle) {
            boolean zoomok = isZoomOk(wayStyle);
            boolean visible = false;
            Collection<Way> outerjoin = new LinkedList<Way>();
            Collection<Way> innerjoin = new LinkedList<Way>();

            drawn = true;
            for (Way w : outer)
            {
                if(w.isClosed()) {
                    outerclosed.add(w);
                } else {
                    outerjoin.add(w);
                }
            }
            for (Way w : inner)
            {
                if(w.isClosed()) {
                    innerclosed.add(w);
                } else {
                    innerjoin.add(w);
                }
            }


            if(outerclosed.size() == 0 && outerjoin.size() == 0)
            {
                putError(r, tr("No outer way for multipolygon ''{0}''.",
                        r.getDisplayName(DefaultNameFormatter.getInstance())), true);
                visible = true; /* prevent killing remaining ways */
            }
            else if(zoomok)
            {
                LinkedList<PolyData> outerPoly = new LinkedList<PolyData>();
                for (Way w : outerclosed) {
                    outerPoly.add(new PolyData(w));
                }
                outerPoly.addAll(joinWays(outerjoin, incomplete ? null : r));
                for (Way wInner : innerclosed)
                {
                    PolyData pdInner = new PolyData(wInner);
                    // incomplete is probably redundant
                    addInnerToOuters(r, incomplete, pdInner, outerPoly);
                }
                for (PolyData pdInner : joinWays(innerjoin, incomplete ? null : r)) {
                    addInnerToOuters(r, incomplete, pdInner, outerPoly);
                }
                AreaElemStyle areaStyle = (AreaElemStyle)wayStyle;
                for (PolyData pd : outerPoly) {
                    Polygon p = pd.get();
                    if(!isPolygonVisible(p)) {
                        continue;
                    }

                    boolean selected = pd.selected || data.isSelected(pd.way) || data.isSelected(r);
                    painter.drawArea(p, selected ? paintSettings.getSelectedColor() : areaStyle.color, null);
                    visible = true;
                }
            }

            if(!visible)
                return drawn;
            for (Way wInner : inner)
            {
                ElemStyle innerStyle = getPrimitiveStyle(wInner);
                if(innerStyle == null)
                {
                    if (data.isSelected(wInner)) {
                        continue;
                    }
                    if(zoomok && (wInner.mappaintDrawnCode != paintid
                            || outer.size() == 0))
                    {
                        ((AreaElemStyle)wayStyle).getLineStyle().paintPrimitive(wInner, paintSettings, painter, (data.isSelected(wInner)
                                || data.isSelected(r)));
                    }
                    wInner.mappaintDrawnCode = paintid;
                }
                else
                {
                    if(data.isSelected(r))
                    {
                        drawSelectedMember(wInner, innerStyle,
                                !wayStyle.equals(innerStyle), data.isSelected(wInner));
                    }
                    if(wayStyle.equals(innerStyle))
                    {
                        putError(r, tr("Style for inner way ''{0}'' equals multipolygon.",
                                wInner.getDisplayName(DefaultNameFormatter.getInstance())), false);
                        if(!data.isSelected(r)) {
                            wInner.mappaintDrawnAreaCode = paintid;
                        }
                    }
                }
            }
            for (Way wOuter : outer)
            {
                ElemStyle outerStyle = getPrimitiveStyle(wOuter);
                if(outerStyle == null)
                {
                    // Selected ways are drawn at the very end
                    if (data.isSelected(wOuter)) {
                        continue;
                    }
                    if(zoomok)
                    {
                        ((AreaElemStyle)wayStyle).getLineStyle().paintPrimitive(wOuter, paintSettings, painter, (data.isSelected(wOuter) || data.isSelected(r)));
                    }
                    wOuter.mappaintDrawnCode = paintid;
                }
                else
                {
                    if(outerStyle instanceof AreaElemStyle
                            && !wayStyle.equals(outerStyle))
                    {
                        putError(r, tr("Style for outer way ''{0}'' mismatches.",
                                wOuter.getDisplayName(DefaultNameFormatter.getInstance())), true);
                    }
                    if(data.isSelected(r))
                    {
                        drawSelectedMember(wOuter, outerStyle, false, false);
                    }
                    else if(outerStyle instanceof AreaElemStyle) {
                        wOuter.mappaintDrawnAreaCode = paintid;
                    }
                }
            }
        }
        return drawn;
    }

    protected boolean isPolygonVisible(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        if (bounds.width == 0 && bounds.height == 0) return false;
        if (bounds.x > nc.getWidth()) return false;
        if (bounds.y > nc.getHeight()) return false;
        if (bounds.x + bounds.width < 0) return false;
        if (bounds.y + bounds.height < 0) return false;
        return true;
    }

    protected Polygon getPolygon(Way w)
    {
        Polygon polygon = new Polygon();

        for (Node n : w.getNodes())
        {
            Point p = nc.getPoint(n);
            polygon.addPoint(p.x,p.y);
        }
        return polygon;
    }

    protected Point2D getCentroid(Polygon p)
    {
        double cx = 0.0, cy = 0.0, a = 0.0;

        // usually requires points[0] == points[npoints] and can then use i+1 instead of j.
        // Faked it slowly using j.  If this is really gets used, this should be fixed.
        for (int i = 0;  i < p.npoints;  i++) {
            int j = i+1 == p.npoints ? 0 : i+1;
            a += (p.xpoints[i] * p.ypoints[j]) - (p.ypoints[i] * p.xpoints[j]);
            cx += (p.xpoints[i] + p.xpoints[j]) * (p.xpoints[i] * p.ypoints[j] - p.ypoints[i] * p.xpoints[j]);
            cy += (p.ypoints[i] + p.ypoints[j]) * (p.xpoints[i] * p.ypoints[j] - p.ypoints[i] * p.xpoints[j]);
        }
        return new Point2D.Double(cx / (3.0*a), cy / (3.0*a));
    }

    protected double getArea(Polygon p)
    {
        double sum = 0.0;

        // usually requires points[0] == points[npoints] and can then use i+1 instead of j.
        // Faked it slowly using j.  If this is really gets used, this should be fixed.
        for (int i = 0;  i < p.npoints;  i++) {
            int j = i+1 == p.npoints ? 0 : i+1;
            sum = sum + (p.xpoints[i] * p.ypoints[j]) - (p.ypoints[i] * p.xpoints[j]);
        }
        return Math.abs(sum/2.0);
    }

    DataSet data;

    <T extends OsmPrimitive> Collection<T> selectedLast(final DataSet data, Collection <T> prims) {
        ArrayList<T> sorted = new ArrayList<T>(prims);
        Collections.sort(sorted,
                new Comparator<T>() {
            public int compare(T o1, T o2) {
                boolean s1 = data.isSelected(o1);
                boolean s2 = data.isSelected(o2);
                if (s1 && !s2)
                    return 1;
                if (!s1 && s2)
                    return -1;
                return o1.compareTo(o2);
            }
        });
        return sorted;
    }

    /* Shows areas before non-areas */
    public void visitAll(DataSet data, boolean virtual, Bounds bounds) {
        BBox bbox = new BBox(bounds);
        this.data = data;

        useStyleCache = Main.pref.getBoolean("mappaint.cache", true);
        int fillAreas = Main.pref.getInteger("mappaint.fillareas", 10000000);
        LatLon ll1 = nc.getLatLon(0, 0);
        LatLon ll2 = nc.getLatLon(100, 0);
        dist = ll1.greatCircleDistance(ll2);

        zoomLevelDisplay = Main.pref.getBoolean("mappaint.zoomLevelDisplay", false);
        circum = Main.map.mapView.getDist100Pixel();
        styles = MapPaintStyles.getStyles().getStyleSet();
        drawMultipolygon = Main.pref.getBoolean("mappaint.multipolygon", true);
        drawRestriction = Main.pref.getBoolean("mappaint.restriction", true);
        leftHandTraffic = Main.pref.getBoolean("mappaint.lefthandtraffic", false);
        minEN = nc.getEastNorth(0, nc.getHeight() - 1);
        maxEN = nc.getEastNorth(nc.getWidth() - 1, 0);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        this.paintSettings = MapPaintSettings.INSTANCE;
        this.painter = new MapPainter(paintSettings, g, inactive, nc, virtual, dist, circum);

        data.clearErrors();

        ++paintid;

        if (fillAreas > dist && styles != null && styles.hasAreas()) {
            Collection<Way> noAreaWays = new LinkedList<Way>();

            /*** RELATIONS ***/
            for (final Relation osm: data.getRelations()) {
                if (osm.isDrawable()) {
                    paintUnselectedRelation(osm);
                }
            }

            /*** AREAS ***/
            for (final Way osm : selectedLast(data, data.searchWays(bbox))) {
                if (osm.isDrawable() && osm.mappaintDrawnCode != paintid) {
                    if (isPrimitiveArea(osm) && osm.mappaintDrawnAreaCode != paintid) {
                        drawWay(osm, fillAreas);
                    } else {
                        noAreaWays.add(osm);
                    }
                }
            }

            /*** WAYS ***/
            for (final Way osm : noAreaWays) {
                drawWay(osm, 0);
            }
        } else {
            drawMultipolygon = false;

            /*** RELATIONS ***/
            for (final Relation osm: data.getRelations()) {
                if (osm.isDrawable()) {
                    paintUnselectedRelation(osm);
                }
            }

            /*** WAYS (filling disabled)  ***/
            for (final Way way: data.getWays()) {
                if (way.isDrawable() && !data.isSelected(way)) {
                    drawWay(way, 0);
                }
            }
        }

        /*** SELECTED  ***/
        for (final OsmPrimitive osm : data.getSelected()) {
            if (osm.isUsable() && !(osm instanceof Node) && osm.mappaintDrawnCode != paintid) {
                osm.visit(new AbstractVisitor() {
                    public void visit(Way w) {
                        drawWay(w, 0);
                    }

                    public void visit(Node n) {
                        // Selected nodes are painted in following part
                    }

                    public void visit(Relation r) {
                        /* TODO: is it possible to do this like the nodes/ways code? */
                        // Only nodes are painted, ways was already painted before (this might cause that
                        // way in selected relation is hidden by another way)
                        for (RelationMember m : r.getMembers()) {
                            if (m.isNode() && m.getMember().isDrawable()) {
                                drawSelectedMember(m.getMember(), styles != null ? getPrimitiveStyle(m.getMember()) : null, true, true);
                            }
                        }
                    }
                });
            }
        }

        /*** NODES ***/
        for (final Node osm: data.searchNodes(bbox)) {
            if (!osm.isIncomplete() && !osm.isDeleted() && (data.isSelected(osm) || !osm.isFiltered())
                    && osm.mappaintDrawnCode != paintid) {
                drawNode(osm);
            }
        }

        painter.drawVirtualNodes(data.searchWays(bbox));
    }

    public void putError(OsmPrimitive p, String text, boolean isError)
    {
        data.addError(p, isError ? tr("Error: {0}", text) : tr("Warning: {0}", text));
    }

    public void setGraphics(Graphics2D g) {
        this.g = g;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public void setNavigatableComponent(NavigatableComponent nc) {
        this.nc = nc;
    }
}
