// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.conflict.MergeDecisionType;
import org.openstreetmap.josm.gui.conflict.tags.TagMergeItem;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a the resolution of a tag conflict in an {@see OsmPrimitive}
 *
 */
public class TagConflictResolveCommand extends ConflictResolveCommand {
    private static final Logger logger = Logger.getLogger(TagConflictResolveCommand.class.getName());


    /** the conflict to resolve */
    private Conflict<OsmPrimitive> conflict;

    /** the list of merge decisions, represented as {@see TagMergeItem}s */
    private final List<TagMergeItem> mergeItems;


    /**
     * replies the number of decided conflicts
     * 
     * @return the number of decided conflicts
     */
    public int getNumDecidedConflicts() {
        int n = 0;
        for (TagMergeItem item: mergeItems) {
            if (!item.getMergeDecision().equals(MergeDecisionType.UNDECIDED)) {
                n++;
            }
        }
        return n;
    }

    /**
     * constructor
     * 
     * @param my  my primitive
     * @param their  their primitive
     * @param mergeItems the list of merge decisions, represented as {@see TagMergeItem}s
     */
    public TagConflictResolveCommand(OsmPrimitive my, OsmPrimitive their, List<TagMergeItem> mergeItems) {
        this.conflict = new Conflict<OsmPrimitive>(my,their);
        this.mergeItems = mergeItems;
    }


    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Resolve {0} tag conflicts in {1} {2}",getNumDecidedConflicts(), OsmPrimitiveType.from(conflict.getMy()).getLocalizedDisplayNameSingular(), conflict.getMy().id),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                )
        );
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of modified primitives, i.e. of
        // OSM primitive 'my'
        //
        super.executeCommand();

        // apply the merge decisions to OSM primitive 'my'
        //
        for (TagMergeItem item: mergeItems) {
            if (! item.getMergeDecision().equals(MergeDecisionType.UNDECIDED)) {
                item.applyToMyPrimitive(conflict.getMy());
            }
        }
        rememberConflict(conflict);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
    }
}
