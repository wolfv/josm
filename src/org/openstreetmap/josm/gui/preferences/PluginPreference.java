//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginDownloader;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.XmlObjectParser.Uniform;

public class PluginPreference implements PreferenceSetting {

    /**
     * Only the plugin name, its jar location and the description.
     * In other words, this is the minimal requirement the plugin preference page
     * needs to show the plugin as available
     *
     * @author imi
     */
    public static class PluginDescription implements Comparable<Object> {
        // Note: All the following need to be public instance variables of
        // type String.  (Plugin description XMLs from the server are parsed
        // with tools.XmlObjectParser, which uses reflection to access them.)
        public String name;
        public String description;
        public String resource;
        public String version;
        public PluginDescription(String name, String description, String resource, String version) {
            this.name = name;
            this.description = description;
            this.resource = resource;
            this.version = version;
        }
        public PluginDescription() {
        }
        public int compareTo(Object n) {
            if(n instanceof PluginDescription)
                return name.compareToIgnoreCase(((PluginDescription)n).name);
            return -1;
        }
    }

    private Map<PluginDescription, Boolean> pluginMap;
    private JPanel plugin;
    private class MyBox extends Box {
        int lastwidth;
        int offset = 40;
        public MyBox()
        {
            super(BoxLayout.Y_AXIS);
        }
        public int myGetWidth()
        {
            int w = plugin.getWidth()-offset;
            if(w <= 0) w = 450;
            lastwidth = w;
            return w;
        }
        public void paint(Graphics g)
        {
            if(lastwidth != plugin.getWidth()-offset)
                refreshPluginPanel(gui);
            super.paint(g);
        }
    }
    private MyBox pluginPanel = new MyBox();
    private PreferenceDialog gui;

    public void addGui(final PreferenceDialog gui) {
        this.gui = gui;
        plugin = gui.createPreferenceTab("plugin", tr("Plugins"), tr("Configure available plugins."), false);
        JScrollPane pluginPane = new JScrollPane(pluginPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pluginPane.setBorder(null);
        plugin.add(pluginPane, GBC.eol().fill(GBC.BOTH));
        plugin.add(GBC.glue(0,10), GBC.eol());
        JButton morePlugins = new JButton(tr("Download List"));
        morePlugins.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                int count = PluginDownloader.downloadDescription();
                if (count > 0)
                    JOptionPane.showMessageDialog(Main.parent,
                        trn("Downloaded plugin information from {0} site",
                            "Downloaded plugin information from {0} sites", count, count));
                else
                    JOptionPane.showMessageDialog(Main.parent, tr("No plugin information found."));
                refreshPluginPanel(gui);
            }
        });
        plugin.add(morePlugins, GBC.std().insets(0,0,10,0));

        JButton update = new JButton(tr("Update"));
        update.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                update();
                refreshPluginPanel(gui);
            }
        });
        plugin.add(update, GBC.std().insets(0,0,10,0));

        JButton configureSites = new JButton(tr("Configure Sites ..."));
        configureSites.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                configureSites();
            }
        });
        plugin.add(configureSites, GBC.std());

        refreshPluginPanel(gui);
    }

    private void configureSites() {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Add either site-josm.xml or Wiki Pages.")), GBC.eol());
        final DefaultListModel model = new DefaultListModel();
        for (String s : PluginDownloader.getSites())
            model.addElement(s);
        final JList list = new JList(model);
        p.add(new JScrollPane(list), GBC.std().fill());
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.add(new JButton(new AbstractAction(tr("Add")){
            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(gui, tr("Add either site-josm.xml or Wiki Pages."));
                if (s != null)
                    model.addElement(s);
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        buttons.add(new JButton(new AbstractAction(tr("Edit")){
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedValue() == null) {
                    JOptionPane.showMessageDialog(gui, tr("Please select an entry."));
                    return;
                }
                String s = JOptionPane.showInputDialog(gui, tr("Add either site-josm.xml or Wiki Pages."), list.getSelectedValue());
                model.setElementAt(s, list.getSelectedIndex());
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        buttons.add(new JButton(new AbstractAction(tr("Delete")){
            public void actionPerformed(ActionEvent event) {
                if (list.getSelectedValue() == null) {
                    JOptionPane.showMessageDialog(gui, tr("Please select an entry."));
                    return;
                }
                model.removeElement(list.getSelectedValue());
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(buttons, GBC.eol());
        int answer = JOptionPane.showConfirmDialog(gui, p, tr("Configure Plugin Sites"), JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION)
            return;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < model.getSize(); ++i) {
            b.append(model.getElementAt(i));
            if (i < model.getSize()-1)
                b.append(" ");
        }
        Main.pref.put("pluginmanager.sites", b.toString());
    }

    private void update() {
        // refresh description
        int num = PluginDownloader.downloadDescription();
        Boolean done = false;
        refreshPluginPanel(gui);

        Set<PluginDescription> toUpdate = new HashSet<PluginDescription>();
        StringBuilder toUpdateStr = new StringBuilder();
        for (PluginProxy proxy : Main.plugins) {
            PluginDescription description = findDescription(proxy.info.name);
            if (description != null && (description.version == null || description.version.equals(""))
            ? (proxy.info.version != null && proxy.info.version.equals("")) : !description.version.equals(proxy.info.version)) {
                toUpdate.add(description);
                toUpdateStr.append(description.name+"\n");
            }
        }
        if (toUpdate.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent, tr("All installed plugins are up to date."));
            done = true;
        }
        else
        {
            int answer = JOptionPane.showConfirmDialog(Main.parent, tr("Update the following plugins:\n\n{0}",
            toUpdateStr.toString()), tr("Update"), JOptionPane.OK_CANCEL_OPTION);
            if (answer == JOptionPane.OK_OPTION)
            {
                PluginDownloader.update(toUpdate);
                done = true;
            }
        }
        if(done && num >= 1)
            Main.pref.put("pluginmanager.lastupdate", Long.toString(System.currentTimeMillis()));
    }

    private PluginDescription findDescription(String name) {
        for (PluginDescription d : pluginMap.keySet())
            if (d.name.equals(name))
                return d;
        return null;
    }

    private void refreshPluginPanel(final PreferenceDialog gui) {
        Collection<PluginDescription> availablePlugins = getAvailablePlugins();
        pluginMap = new HashMap<PluginDescription, Boolean>();
        pluginPanel.removeAll();
        int width = pluginPanel.myGetWidth();

        Collection<String> enabledPlugins = Main.pref.getCollection("plugins", null);

        for (final PluginDescription plugin : availablePlugins) {
            boolean enabled = enabledPlugins != null && enabledPlugins.contains(plugin.name);
            String remoteversion = plugin.version;
            if(remoteversion == null || remoteversion.equals(""))
                remoteversion = tr("unknown");

            String localversion;
            PluginInformation p = PluginInformation.findPlugin(plugin.name);
            if(p != null)
            {
                if(p.version != null && !p.version.equals(""))
                    localversion = p.version;
                else
                    localversion = tr("unknown");
                localversion = " (" + localversion + ")";
            }
            else
                localversion = "";

            final JCheckBox pluginCheck = new JCheckBox(tr("{0}: Version {1}{2}", plugin.name, remoteversion, localversion), enabled);
            pluginPanel.add(pluginCheck);

            pluginCheck.setToolTipText(plugin.resource != null ? ""+plugin.resource : tr("Plugin bundled with JOSM"));
            JLabel label = new JLabel("<html><i>"+(plugin.description==null?tr("no description available"):plugin.description)+"</i></html>");
            label.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));
            label.setMaximumSize(new Dimension(width,1000));
            pluginPanel.add(label);
            pluginPanel.add(Box.createVerticalStrut(5));

            pluginMap.put(plugin, enabled);
            pluginCheck.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    // if user enabled a plugin, it is not loaded but found somewhere on disk: offer to delete jar
                    if (pluginCheck.isSelected()) {
                        PluginInformation plinfo = PluginInformation.findPlugin(plugin.name);
                        if ((PluginInformation.getLoaded(plugin.name) == null) && (plinfo != null)) {
                            try {
                                int answer = JOptionPane.showConfirmDialog(Main.parent,
                                    tr("Plugin archive already available. Do you want to download current version by deleting existing archive?\n\n{0}",
                                    plinfo.file.getCanonicalPath()), tr("Plugin already exists"), JOptionPane.OK_CANCEL_OPTION);
                                if (answer == JOptionPane.OK_OPTION) {
                                    if (!plinfo.file.delete()) {
                                        JOptionPane.showMessageDialog(Main.parent, tr("Error deleting plugin file: {0}", plinfo.file.getCanonicalPath()));
                                    }
                                }
                            } catch (IOException e1) {
                                e1.printStackTrace();
                                JOptionPane.showMessageDialog(Main.parent, tr("Error deleting plugin file: {0}", e1.getMessage()));
                            }
                        }
                    }
                    pluginMap.put(plugin, pluginCheck.isSelected());
                }
            });
        }
        plugin.updateUI();
    }

    private Collection<PluginDescription> getAvailablePlugins() {
        SortedMap<String, PluginDescription> availablePlugins = new TreeMap<String, PluginDescription>(new Comparator<String>(){
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        for (String location : PluginInformation.getPluginLocations()) {
            File[] pluginFiles = new File(location).listFiles();
            if (pluginFiles != null) {
                Arrays.sort(pluginFiles);
                for (File f : pluginFiles) {
                    if (!f.isFile())
                        continue;
                    if (f.getName().endsWith(".jar")) {
                        try {
                            PluginInformation info = new PluginInformation(f);
                            if (!availablePlugins.containsKey(info.name))
                                availablePlugins.put(info.name, new PluginDescription(
                                    info.name,
                                    info.description,
                                    PluginInformation.fileToURL(f).toString(),
                                    info.version));
                        } catch (PluginException x) {
                        }
                    } else if (f.getName().matches("^[0-9]+-site.*\\.xml$")) {
                        try {
                            Uniform<PluginDescription> parser = new Uniform<PluginDescription>(new FileReader(f), "plugin", PluginDescription.class);
                            for (PluginDescription pd : parser)
                                if (!availablePlugins.containsKey(pd.name))
                                    availablePlugins.put(pd.name, pd);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(Main.parent, tr("Error reading plugin information file: {0}", f.getName()));
                        }
                    }
                }
            }
        }
        for (PluginProxy proxy : Main.plugins)
            if (!availablePlugins.containsKey(proxy.info.name))
                availablePlugins.put(proxy.info.name, new PluginDescription(
                        proxy.info.name,
                        proxy.info.description,
                        proxy.info.file == null ? null :
                            PluginInformation.fileToURL(proxy.info.file).toString(),
                        proxy.info.version));
        return availablePlugins.values();
    }

    public void ok() {
        Collection<PluginDescription> toDownload = new LinkedList<PluginDescription>();
        String msg = "";
        for (Entry<PluginDescription, Boolean> entry : pluginMap.entrySet()) {
            if (entry.getValue() && PluginInformation.findPlugin(entry.getKey().name) == null) {
                toDownload.add(entry.getKey());
                msg += entry.getKey().name+"\n";
            }
        }
        if (!toDownload.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(Main.parent,
                    tr("Download the following plugins?\n\n{0}", msg),
                    tr("Download missing plugins"),
                    JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.OK_OPTION)
                for (PluginDescription pd : toDownload)
                    pluginMap.put(pd, false);
            else
                for (PluginDescription pd : toDownload)
                    if (!PluginDownloader.downloadPlugin(pd))
                        pluginMap.put(pd, false);

        }

        String oldPlugins = Main.pref.get("plugins");
            LinkedList<String> plugins = new LinkedList<String>();
        Object pd[] = pluginMap.keySet().toArray();
        Arrays.sort(pd);
        for (Object d : pd) {
            if (pluginMap.get(d))
                plugins.add(((PluginDescription)d).name);
        }

        Main.pref.putCollection("plugins", plugins);
        String newPlugins = Main.pref.get("plugins");
        if(oldPlugins == null && plugins == null)
            return;
        if(plugins == null || oldPlugins == null || !plugins.equals(oldPlugins))
            gui.requiresRestart = true;
    }
}
