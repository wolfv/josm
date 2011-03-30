// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

public interface Selector {

    /**
     * Apply the selector to the primitive and check if it matches.
     *
     * @param env the Environment. env.mc and env.layer are read-only when matching a selector.
     * env.source is not needed. This method will set the matchingReferrers field of env as
     * a side effect! Make sure to clear it before invoking this method.
     * @return true, if the selector applies
     */
    public boolean matches(Environment env);

    public String getSubpart();

    public Range getRange();

    /**
     * <p>Represents a child selector or a parent selector.</p>
     * 
     * <p>In addition to the standard CSS notation for child selectors, JOSM also supports
     * an "inverse" notation:</p>
     * <pre>
     *    selector_a > selector_b { ... }       // the standard notation (child selector)
     *    relation[type=route] > way { ... }    // example (all ways of a route)
     * 
     *    selector_a < selector_b { ... }       // the inverse notation (parent selector)
     *    node[traffic_calming] < way { ... }   // example (way that has a traffic calming node)
     * </pre>
     *
     */
    public static class ChildOrParentSelector implements Selector {
        Selector a, b;
        /** true, if this represents a parent selector (otherwise it is a child selector)
         */
        private final boolean parentSelector;

        /**
         * 
         * @param a the first selector
         * @param b the second selector
         * @param parentSelector if true, this is a parent selector; otherwise a child selector
         */
        public ChildOrParentSelector(Selector a, Selector b, boolean parentSelector) {
            this.a = a;
            this.b = b;
            this.parentSelector = parentSelector;
        }

        @Override
        public boolean matches(Environment e) {
            if (!b.matches(e))
                return false;

            Environment e2 = new Environment(null, e.mc, e.layer, e.source);
            List<OsmPrimitive> matchingRefs = new ArrayList<OsmPrimitive>();
            if (!parentSelector) {
                for (OsmPrimitive ref : e.osm.getReferrers()) {
                    e2.osm = ref;
                    if (a.matches(e2)) {
                        matchingRefs.add(ref);
                    }
                }
                if (!matchingRefs.isEmpty()) {
                    e.setMatchingReferrers(matchingRefs);
                    return true;
                }
            } else {
                if (e.osm instanceof Relation) {
                    for (OsmPrimitive chld : ((Relation) e.osm).getMemberPrimitives()) {
                        e2.osm = chld;
                        if (a.matches(e2))
                            return true;
                    }
                } else if (e.osm instanceof Way) {
                    for (Node n : ((Way) e.osm).getNodes()) {
                        e2.osm = n;
                        if (a.matches(e2))
                            return true;
                    }
                }
            }
            return false;
        }

        @Override
        public String getSubpart() {
            return b.getSubpart();
        }

        @Override
        public Range getRange() {
            return b.getRange();
        }
    }

    public static class GeneralSelector implements Selector {
        public String base;
        public Range range;
        protected List<Condition> conds;
        private String subpart;

        public GeneralSelector(String base, Pair<Integer, Integer> zoom, List<Condition> conds, String subpart) {
            this.base = base;
            if (zoom != null) {
                int a = zoom.a == null ? 0 : zoom.a;
                int b = zoom.b == null ? Integer.MAX_VALUE : zoom.b;
                if (a <= b) {
                    range = fromLevel(a, b);
                }
            }
            if (range == null) {
                range = new Range();
            }
            this.conds = conds;
            this.subpart = subpart;
        }

        @Override
        public String getSubpart() {
            return subpart;
        }
        @Override
        public Range getRange() {
            return range;
        }

        @Override
        public boolean matches(Environment e) {
            if (!baseApplies(e.osm))
                return false;
            for (Condition c : conds) {
                if (!c.applies(e))
                    return false;
            }
            return true;
        }

        private boolean baseApplies(OsmPrimitive osm) {
            if (base.equals("*"))
                return true;
            if (base.equals("area")) {
                if (osm instanceof Way)
                    return true;
                if (osm instanceof Relation && ((Relation) osm).isMultipolygon())
                    return true;
            }
            if (base.equals(OsmPrimitiveType.from(osm).getAPIName()))
                return true;
            return false;
        }

        public static Range fromLevel(int a, int b) {
            if (a > b)
                throw new AssertionError();
            double lower = 0;
            double upper = Double.POSITIVE_INFINITY;
            if (b != Integer.MAX_VALUE) {
                lower = level2scale(b + 1);
            }
            if (a != 0) {
                upper = level2scale(a);
            }
            return new Range(lower, upper);
        }

        final static double R = 6378135;

        public static double level2scale(int lvl) {
            if (lvl < 0)
                throw new IllegalArgumentException();
            // preliminary formula - map such that mapnik imagery tiles of the same
            // or similar level are displayed at the given scale
            return 2.0 * Math.PI * R / Math.pow(2.0, lvl) / 2.56;
        }

        @Override
        public String toString() {
            return base + (range == null ? "" : range) + Utils.join("", conds) + (subpart != null ? ("::" + subpart) : "");
        }
    }
}
