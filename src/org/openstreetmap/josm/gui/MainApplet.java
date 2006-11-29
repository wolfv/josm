package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.ServerSidePreferences;
import org.openstreetmap.josm.tools.GBC;

public class MainApplet extends JApplet {

	private final class MainCaller extends Main {
		private MainCaller() {
			setContentPane(contentPane);
			setJMenuBar(menu);
			setBounds(bounds);
		}
	}

	private final static String[][] paramInfo = {
		{"username", tr("string"), tr("Name of the user.")},
		{"password", tr("string"), tr("OSM Password.")},
		{"geometry", tr("string"), tr("Size the applet to the given geometry (format: WIDTHxHEIGHT)")},
		{"download", tr("string;string;..."), tr("Download each. Can be x1,y1,x2,y2 an url containing lat=y&lon=x&zoom=z or a filename")},
		{"downloadgps", tr("string;string;..."), tr("Download each as raw gps. Can be x1,y1,x2,y2 an url containing lat=y&lon=x&zoom=z or a filename")},
		{"selection", tr("string;string;..."), tr("Add each to the initial selection. Can be a google-like search string or an url which returns osm-xml")},
		{"reset-preferences", tr("any"),tr("If specified, reset the configuration instead of reading it.")}
	};

	private Map<String, Collection<String>> args = new HashMap<String, Collection<String>>(); 

	@Override public String[][] getParameterInfo() {
		return paramInfo;
	}

	@Override public void init() {
		for (String[] s : paramInfo) {
			Collection<String> p = readParameter(s[0], args.get(s[0]));
			if (p != null)
				args.put(s[0], p);
		}
		if (!args.containsKey("geometry") && getParameter("width") != null && getParameter("height") != null) {
			args.put("geometry", Arrays.asList(new String[]{getParameter("width")+"x"+getParameter("height")}));
		}
	}

	@Override public void start() {
		String username = args.containsKey("username") ? args.get("username").iterator().next() : null;
		String password = args.containsKey("password") ? args.get("password").iterator().next() : null;
		if (username == null || password == null) {
			JPanel p = new JPanel(new GridBagLayout());
			p.add(new JLabel(tr("Username")), GBC.std().insets(0,0,20,0));
			JTextField user = new JTextField(username == null ? "" : username);
			p.add(user, GBC.eol().fill(GBC.HORIZONTAL));
			p.add(new JLabel(tr("Password")), GBC.std().insets(0,0,20,0));
			JPasswordField pass = new JPasswordField(password == null ? "" : password);
			p.add(pass, GBC.eol().fill(GBC.HORIZONTAL));
			JOptionPane.showMessageDialog(null, p);
			username = user.getText();
			password = new String(pass.getPassword());
			args.put("password", Arrays.asList(new String[]{password}));
		}

		Main.applet = true;
		Main.pref = new ServerSidePreferences(getCodeBase(), username);

		Main.preConstructorInit(args);
		Main.parent = this;
		new MainCaller().postConstructorProcessCmdLine(args);
		
		MainMenu m = Main.main.menu; // shortcut

		// remove offending stuff from JOSM (that would break the SecurityManager)
		m.remove(m.fileMenu);
		m.open.setEnabled(false);
		m.exit.setEnabled(false);
		m.save.setEnabled(false);
		m.saveAs.setEnabled(false);
		m.gpxExport.setEnabled(false);
	}

	private Collection<String> readParameter(String s, Collection<String> v) {
		String param = getParameter(s);
		if (param != null) {
			if (v == null)
				v = new LinkedList<String>();
			v.addAll(Arrays.asList(param.split(";")));
		}
		return v;
	}
}
