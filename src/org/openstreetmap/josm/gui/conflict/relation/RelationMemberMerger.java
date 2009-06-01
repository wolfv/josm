// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.relation;

import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.conflict.ListMerger;

/**
 * A UI component for resolving conflicts in the member lists of two {@see Relation}
 */
public class RelationMemberMerger extends ListMerger<RelationMember> {
    private static final Logger logger = Logger.getLogger(RelationMemberMerger.class.getName());

    @Override
    protected JScrollPane buildMyElementsTable() {
        myEntriesTable  = new JTable(
                model.getMyTableModel(),
                new RelationMemberListColumnModel(),
                model.getMySelectionModel()
        );
        myEntriesTable.setName("table.mynodes");
        return embeddInScrollPane(myEntriesTable);
    }

    @Override
    protected JScrollPane buildMergedElementsTable() {
        logger.info(model.getMergedTableModel().toString());
        mergedEntriesTable  = new JTable(
                model.getMergedTableModel(),
                new RelationMemberListColumnModel(),
                model.getMergedSelectionModel()
        );
        mergedEntriesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        mergedEntriesTable.setName("table.mergednodes");
        return embeddInScrollPane(mergedEntriesTable);
    }

    @Override
    protected JScrollPane buildTheirElementsTable() {
        theirEntriesTable  = new JTable(
                model.getTheirTableModel(),
                new RelationMemberListColumnModel(),
                model.getTheirSelectionModel()
        );
        theirEntriesTable.setName("table.theirnodes");
        return embeddInScrollPane(theirEntriesTable);
    }

    public void populate(Relation my, Relation their) {
        RelationMemberListMergeModel model = (RelationMemberListMergeModel)getModel();
        model.populate(my,their);
    }

    public RelationMemberMerger() {
        super(new RelationMemberListMergeModel());
    }
}
