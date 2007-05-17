package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.xml.sax.SAXException;

/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask implements DownloadTask {

	private static class Task extends PleaseWaitRunnable {
		private BoundingBoxDownloader reader;
		private DataSet dataSet;

		public Task(BoundingBoxDownloader reader) {
			super(tr("Downloading data"));
			this.reader = reader;
		}

		@Override public void realRun() throws IOException, SAXException {
			dataSet = reader.parseOsm();
		}

		@Override protected void finish() {
			if (dataSet == null)
				return; // user cancelled download or error occoured
			if (dataSet.allPrimitives().isEmpty())
				errorMessage = tr("No data imported.");
			Main.main.addLayer(new OsmDataLayer(dataSet, tr("Data Layer"), null));
		}

		@Override protected void cancel() {
			if (reader != null)
				reader.cancel();
		}
	}
	private JCheckBox checkBox = new JCheckBox(tr("OpenStreetMap data"));

	public void download(DownloadAction action, double minlat, double minlon, double maxlat, double maxlon) {
		Task task = new Task(new BoundingBoxDownloader(minlat, minlon, maxlat, maxlon));
		Main.worker.execute(task);
    }

	public JCheckBox getCheckBox() {
	    return checkBox;
    }

	public String getPreferencesSuffix() {
	    return "osm";
    }
}
