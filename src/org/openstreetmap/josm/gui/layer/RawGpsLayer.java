// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.io.MultiPartFormOutputStream;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.DontShowAgainInfo;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;

/**
 * A layer holding data from a gps source.
 * The data is read only.
 * 
 * @author imi
 */
public class RawGpsLayer extends Layer implements PreferenceChangedListener {

	public class ConvertToDataLayerAction extends AbstractAction {
		public ConvertToDataLayerAction() {
			super(tr("Convert to data layer"), ImageProvider.get("converttoosm"));
		}
		public void actionPerformed(ActionEvent e) {
			JPanel msg = new JPanel(new GridBagLayout());
			msg.add(new JLabel(tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>If you want to upload traces, look here:")), GBC.eol());
			msg.add(new UrlLabel(tr("http://www.openstreetmap.org/traces")), GBC.eop());
			if (!DontShowAgainInfo.show("convert_to_data", msg))
				return;
			DataSet ds = new DataSet();
			for (Collection<GpsPoint> c : data) {
				Way w = new Way();
				for (GpsPoint p : c) {
					Node n = new Node(p.latlon);
					ds.nodes.add(n);
					w.nodes.add(n);
				}
				ds.ways.add(w);
			}
			Main.main.addLayer(new OsmDataLayer(ds, tr("Converted from: {0}", RawGpsLayer.this.name), null));
			Main.main.removeLayer(RawGpsLayer.this);
		}
	}
	
	public class UploadTraceAction extends AbstractAction {
		public UploadTraceAction() {
			super(tr("Upload this trace..."), ImageProvider.get("uploadtrace"));
		}
		public void actionPerformed(ActionEvent e) {
			JPanel msg = new JPanel(new GridBagLayout());
			msg.add(new JLabel(tr("<html>This functionality has been added only recently. Please<br>"+
					              "use with care and check if it works as expected.</html>")), GBC.eop());
			ButtonGroup bg = new ButtonGroup();
			JRadioButton c1 = null;
			JRadioButton c2 = null;
			
			if (associatedFile != null) {
				c1 = new JRadioButton(tr("Upload track filtered by JOSM"), false);
				c2 = new JRadioButton(tr("Upload raw file: {0}", associatedFile.getName()), true);
			}
			else
			{
				c1 = new JRadioButton(tr("Upload track filtered by JOSM"), true);
				c2 = new JRadioButton(tr("Upload raw file: "), false);
				c2.setEnabled(false);
			}
			c1.setEnabled(false);
			bg.add(c1);
			bg.add(c2);

			msg.add(c1, GBC.eol());
			msg.add(c2, GBC.eop());

			
			JTextField description = new JTextField();
			JTextField tags = new JTextField();
			msg.add(new JLabel(tr("Description:")), GBC.std());
			msg.add(description, GBC.eol().fill(GBC.HORIZONTAL));
			msg.add(new JLabel(tr("Tags:")), GBC.std());
			msg.add(tags, GBC.eol().fill(GBC.HORIZONTAL));
			JCheckBox c3 = new JCheckBox("public");
			msg.add(c3, GBC.eop());
			msg.add(new JLabel("Please ensure that you don't upload your traces twice."), GBC.eop());
			
			int answer = JOptionPane.showConfirmDialog(Main.parent, msg, tr("GPX-Upload"), JOptionPane.OK_CANCEL_OPTION);
			if (answer == JOptionPane.OK_OPTION)
			{
				try {
					String version = Main.pref.get("osm-server.version", "0.5");
					URL url = new URL(Main.pref.get("osm-server.url") +
							"/" + version + "/gpx/create");

					// create a boundary string
					String boundary = MultiPartFormOutputStream.createBoundary();
					URLConnection urlConn = MultiPartFormOutputStream.createConnection(url);
					urlConn.setRequestProperty("Accept", "*/*");
					urlConn.setRequestProperty("Content-Type", 
						MultiPartFormOutputStream.getContentType(boundary));
					// set some other request headers...
					urlConn.setRequestProperty("Connection", "Keep-Alive");
					urlConn.setRequestProperty("Cache-Control", "no-cache");
					// no need to connect cuz getOutputStream() does it
					MultiPartFormOutputStream out = 
						new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
					out.writeField("description", description.getText());
					out.writeField("tags", tags.getText());
					out.writeField("public", (c3.getSelectedObjects() != null) ? "1" : "0");
					// upload a file
					out.writeFile("gpx_file", "text/xml", associatedFile);
					// can also write bytes directly
					// out.writeFile("myFile", "text/plain", "C:\\test.txt", 
					// "This is some file text.".getBytes("ASCII"));
					out.close();
					// read response from server
					BufferedReader in = new BufferedReader(
						new InputStreamReader(urlConn.getInputStream()));
					String line = "";
					while((line = in.readLine()) != null) {
						 System.out.println(line);
					}
					in.close();
					
					/*
					int retCode = activeConnection.getResponseCode();
					System.out.println("got return: "+retCode);
					String retMsg = activeConnection.getResponseMessage();
					activeConnection.disconnect();
					if (retCode != 200) {
						// Look for a detailed error message from the server
						if (activeConnection.getHeaderField("Error") != null)
							retMsg += "\n" + activeConnection.getHeaderField("Error");

						// Report our error
						ByteArrayOutputStream o = new ByteArrayOutputStream();
						System.out.println(new String(o.toByteArray(), "UTF-8").toString());
						throw new RuntimeException(retCode+" "+retMsg);
					}
					*/
				} catch (UnknownHostException ex) {
					throw new RuntimeException(tr("Unknown host")+": "+ex.getMessage(), ex);
				} catch (Exception ex) {
					//if (cancel)
					//	return; // assume cancel
					if (ex instanceof RuntimeException)
						throw (RuntimeException)ex;
					throw new RuntimeException(ex.getMessage(), ex);
				}	
			}
		}
	}

	public static class GpsPoint {
		public final LatLon latlon;
		public final EastNorth eastNorth;
		public final String time;
		public GpsPoint(LatLon ll, String t) {
			latlon = ll; 
			eastNorth = Main.proj.latlon2eastNorth(ll); 
			time = t;
		}
	}

	/**
	 * A list of ways which containing a list of points.
	 */
	public final Collection<Collection<GpsPoint>> data;
	public final boolean fromServer;

	public RawGpsLayer(boolean fromServer, Collection<Collection<GpsPoint>> data, String name, File associatedFile) {
		super(name);
		this.fromServer = fromServer;
		this.associatedFile = associatedFile;
		this.data = data;
		Main.pref.listener.add(this);
	}

	/**
	 * Return a static icon.
	 */
	@Override public Icon getIcon() {
		return ImageProvider.get("layer", "rawgps_small");
	}

	@Override public void paint(Graphics g, MapView mv) {
		String gpsCol = Main.pref.get("color.gps point");
		String gpsColSpecial = Main.pref.get("color.layer "+name);
		if (!gpsColSpecial.equals(""))
			g.setColor(ColorHelper.html2color(gpsColSpecial));
		else if (!gpsCol.equals(""))
			g.setColor(ColorHelper.html2color(gpsCol));
		else
			g.setColor(Color.GRAY);
		Point old = null;

		boolean force = Main.pref.getBoolean("draw.rawgps.lines.force");
		boolean lines = Main.pref.getBoolean("draw.rawgps.lines");
		String linesKey = "draw.rawgps.lines.layer "+name;
		if (Main.pref.hasKey(linesKey))
			lines = Main.pref.getBoolean(linesKey);
		boolean large = Main.pref.getBoolean("draw.rawgps.large");
		for (Collection<GpsPoint> c : data) {
			if (!force)
				old = null;
			for (GpsPoint p : c) {
				Point screen = mv.getPoint(p.eastNorth);
				if (lines && old != null)
					g.drawLine(old.x, old.y, screen.x, screen.y);
				else if (!large)
					g.drawRect(screen.x, screen.y, 0, 0);
				if (large)
					g.fillRect(screen.x-1, screen.y-1, 3, 3);
				old = screen;
			}
		}
	}

	@Override public String getToolTipText() {
		int points = 0;
		for (Collection<GpsPoint> c : data)
			points += c.size();
		String tool = data.size()+" "+trn("track", "tracks", data.size())
		+" "+points+" "+trn("point", "points", points);
		if (associatedFile != null)
			tool = "<html>"+tool+"<br>"+associatedFile.getPath()+"</html>";
		return tool;
	}

	@Override public void mergeFrom(Layer from) {
		RawGpsLayer layer = (RawGpsLayer)from;
		data.addAll(layer.data);
	}

	@Override public boolean isMergable(Layer other) {
		return other instanceof RawGpsLayer;
	}

	@Override public void visitBoundingBox(BoundingXYVisitor v) {
		for (Collection<GpsPoint> c : data)
			for (GpsPoint p : c)
				v.visit(p.eastNorth);
	}

	@Override public Object getInfoComponent() {
		StringBuilder b = new StringBuilder();
		int points = 0;
		for (Collection<GpsPoint> c : data) {
			b.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+trn("a track with {0} point","a track with {0} points", c.size(), c.size())+"<br>");
			points += c.size();
		}
		b.append("</html>");
		return "<html>"+trn("{0} consists of {1} track", "{0} consists of {1} tracks", data.size(), name, data.size())+" ("+trn("{0} point", "{0} points", points, points)+")<br>"+b.toString();
	}

	@Override public Component[] getMenuEntries() {
		JMenuItem line = new JMenuItem(tr("Customize line drawing"), ImageProvider.get("mapmode/addsegment"));
		line.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JRadioButton[] r = new JRadioButton[3];
				r[0] = new JRadioButton(tr("Use global settings."));
				r[1] = new JRadioButton(tr("Draw lines between points for this layer."));
				r[2] = new JRadioButton(tr("Do not draw lines between points for this layer."));
				ButtonGroup group = new ButtonGroup();
				Box panel = Box.createVerticalBox();
				for (JRadioButton b : r) {
					group.add(b);
					panel.add(b);
				}
				String propName = "draw.rawgps.lines.layer "+name;
				if (Main.pref.hasKey(propName))
					group.setSelected(r[Main.pref.getBoolean(propName) ? 1:2].getModel(), true);
				else
					group.setSelected(r[0].getModel(), true);
				int answer = JOptionPane.showConfirmDialog(Main.parent, panel, tr("Select line drawing options"), JOptionPane.OK_CANCEL_OPTION);
				if (answer == JOptionPane.CANCEL_OPTION)
					return;
				if (group.getSelection() == r[0].getModel())
					Main.pref.put(propName, null);
				else
					Main.pref.put(propName, group.getSelection() == r[1].getModel());
				Main.map.repaint();
			}
		});

		JMenuItem color = new JMenuItem(tr("Customize Color"), ImageProvider.get("colorchooser"));
		color.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String col = Main.pref.get("color.layer "+name, Main.pref.get("color.gps point", ColorHelper.color2html(Color.gray)));
				JColorChooser c = new JColorChooser(ColorHelper.html2color(col));
				Object[] options = new Object[]{tr("OK"), tr("Cancel"), tr("Default")};
				int answer = JOptionPane.showOptionDialog(Main.parent, c, tr("Choose a color"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				switch (answer) {
				case 0:
					Main.pref.put("color.layer "+name, ColorHelper.color2html(c.getColor()));
					break;
				case 1:
					return;
				case 2:
					Main.pref.put("color.layer "+name, null);
					break;
				}
				Main.map.repaint();
			}
		});

		JMenuItem tagimage = new JMenuItem(tr("Import images"), ImageProvider.get("tagimages"));
		tagimage.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(Main.pref.get("tagimages.lastdirectory"));
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fc.setMultiSelectionEnabled(true);
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileFilter(){
					@Override public boolean accept(File f) {
						return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg");
					}
					@Override public String getDescription() {
						return tr("JPEG images (*.jpg)");
					}
				});
				fc.showOpenDialog(Main.parent);
				File[] sel = fc.getSelectedFiles();
				if (sel == null || sel.length == 0)
					return;
				LinkedList<File> files = new LinkedList<File>();
				addRecursiveFiles(files, sel);
				Main.pref.put("tagimages.lastdirectory", fc.getCurrentDirectory().getPath());
				GeoImageLayer.create(files, RawGpsLayer.this);
			}

			private void addRecursiveFiles(LinkedList<File> files, File[] sel) {
				for (File f : sel) {
					if (f.isDirectory())
						addRecursiveFiles(files, f.listFiles());
					else if (f.getName().toLowerCase().endsWith(".jpg"))
						files.add(f);
				}
			}
		});

		if (Main.applet)
			return new Component[]{
				new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
				new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
				new JSeparator(),
				color,
				line,
				new JMenuItem(new ConvertToDataLayerAction()),
				//new JMenuItem(new UploadTraceAction()),
				new JSeparator(),
				new JMenuItem(new RenameLayerAction(associatedFile, this)),
				new JSeparator(),
				new JMenuItem(new LayerListPopup.InfoAction(this))};
		return new Component[]{
				new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
				new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
				new JSeparator(),
				new JMenuItem(new GpxExportAction(this)),
				color,
				line,
				tagimage,
				new JMenuItem(new ConvertToDataLayerAction()),
				//new JMenuItem(new UploadTraceAction()),
				new JSeparator(),
				new JMenuItem(new RenameLayerAction(associatedFile, this)),
				new JSeparator(),
				new JMenuItem(new LayerListPopup.InfoAction(this))};
	}

	public void preferenceChanged(String key, String newValue) {
		if (Main.map != null && (key.equals("draw.rawgps.lines") || key.equals("draw.rawgps.lines.force")))
			Main.map.repaint();
	}

	@Override public void destroy() {
		Main.pref.listener.remove(RawGpsLayer.this);
    }
}
