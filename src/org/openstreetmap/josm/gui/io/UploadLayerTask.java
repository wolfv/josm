// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.actions.upload.CyclicUploadDependencyException;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiPrimitiveGoneException;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * UploadLayerTask uploads the data managed by an {@see OsmDataLayer} asynchronously.
 *
 * <pre>
 *     ExecutorService executorService = ...
 *     UploadLayerTask task = new UploadLayerTask(layer, monitor);
 *     Future<?> taskFuture = executorServce.submit(task)
 *     try {
 *        // wait for the task to complete
 *        taskFuture.get();
 *     } catch(Exception e) {
 *        e.printStackTracek();
 *     }
 * </pre>
 */
class UploadLayerTask extends AbstractIOTask implements Runnable {
    private OsmServerWriter writer;
    private OsmDataLayer layer;
    private ProgressMonitor monitor;
    private Changeset changeset;
    private boolean closeChangesetAfterUpload;
    private Collection<OsmPrimitive> toUpload;
    private HashSet<OsmPrimitive> processedPrimitives;

    /**
     *
     * @param layer the layer. Must not be null.
     * @param monitor  a progress monitor. If monitor is null, uses {@see NullProgressMonitor#INSTANCE}
     * @param changeset the changeset to be used
     * @param closeChangesetAfterUpload true, if the changeset should be closed after the upload
     * @throws IllegalArgumentException thrown, if layer is null
     */
    public UploadLayerTask(OsmDataLayer layer, ProgressMonitor monitor, Changeset changeset, boolean closeChangesetAfterUpload) {
        if (layer == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", layer));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        this.layer = layer;
        this.monitor = monitor;
        this.changeset = changeset;
        this.closeChangesetAfterUpload = closeChangesetAfterUpload;
        processedPrimitives = new HashSet<OsmPrimitive>();
    }

    protected OsmPrimitive getPrimitive(OsmPrimitiveType type, long id) {
        for (OsmPrimitive p: toUpload) {
            if (OsmPrimitiveType.from(p).equals(type) && p.getId() == id)
                return p;
        }
        return null;
    }

    /**
     * Retries to recover the upload operation from an exception which was thrown because
     * an uploaded primitive was already deleted on the server.
     *
     * @param e the exception throw by the API
     * @param monitor a progress monitor
     * @throws OsmTransferException  thrown if we can't recover from the exception
     */
    protected void recoverFromGoneOnServer(OsmApiPrimitiveGoneException e, ProgressMonitor monitor) throws OsmTransferException{
        if (!e.isKnownPrimitive()) throw e;
        OsmPrimitive p = getPrimitive(e.getPrimitiveType(), e.getPrimitiveId());
        if (p == null) throw e;
        if (p.isDeleted()) {
            // we tried to delete an already deleted primitive.
            //
            System.out.println(tr("Warning: primitive ''{0}'' is already deleted on the server. Skipping this primitive and retrying to upload.", p.getDisplayName(DefaultNameFormatter.getInstance())));
            processedPrimitives.addAll(writer.getProcessedPrimitives());
            processedPrimitives.add(p);
            toUpload.removeAll(processedPrimitives);
            return;
        }
        // exception was thrown because we tried to *update* an already deleted
        // primitive. We can't resolve this automatically. Re-throw exception,
        // a conflict is going to be created later.
        throw e;
    }

    @Override
    public void run() {
        monitor.subTask(tr("Preparing primitives to upload ..."));
        APIDataSet ds = new APIDataSet(layer.data);
        try {
            ds.adjustRelationUploadOrder();
        } catch(CyclicUploadDependencyException e) {
            setLastException(e);
            return;
        }
        toUpload = ds.getPrimitives();
        if (toUpload.isEmpty())
            return;
        writer = new OsmServerWriter();
        try {
            while(true) {
                try {
                    ProgressMonitor m = monitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
                    if (isCancelled()) return;
                    writer.uploadOsm(layer.data.getVersion(), toUpload, changeset, m);
                    processedPrimitives.addAll(writer.getProcessedPrimitives());
                    break;
                } catch(OsmApiPrimitiveGoneException e) {
                    recoverFromGoneOnServer(e, monitor);
                }
            }
            if (closeChangesetAfterUpload) {
                if (changeset != null && changeset.getId() > 0) {
                    OsmApi.getOsmApi().closeChangeset(changeset, monitor.createSubTaskMonitor(0, false));
                }
            }
        } catch (Exception sxe) {
            if (isCancelled()) {
                System.out.println("Ignoring exception caught because upload is cancelled. Exception is: " + sxe.toString());
                return;
            }
            setLastException(sxe);
        }

        if (isCancelled())
            return;
        layer.cleanupAfterUpload(processedPrimitives);
        layer.fireDataChange();
        layer.onPostUploadToServer();

        // don't process exceptions remembered with setLastException().
        // Caller is supposed to deal with them.
    }

    @Override
    public void cancel() {
        setCancelled(true);
        if (writer != null) {
            writer.cancel();
        }
    }
}
