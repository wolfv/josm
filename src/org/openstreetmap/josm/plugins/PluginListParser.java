// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

/**
 * A parser for the plugin list provided by a JOSM Plugin Download Site.
 * 
 * See <a href="http://josm.openstreetmap.de/plugin">http://josm.openstreetmap.de/plugin</a>
 * for a sample of the document. The format is a custom format, kind of mix of CSV and RFC822 style
 * name/value-pairs.
 *
 */
public class PluginListParser {

    /**
     * Creates the plugin information object
     * 
     * @param name the plugin name
     * @param url the plugin download url
     * @param manifest the plugin manifest
     * @return a plugin information object
     * @throws PluginListParseException
     */
    protected PluginInformation createInfo(String name, String url, String manifest) throws PluginListParseException{
        try {
            return new PluginInformation(
                    new ByteArrayInputStream(manifest.getBytes("utf-8")),
                    name.substring(0, name.length() - 4),
                    url
            );
        } catch(UnsupportedEncodingException e) {
            throw new PluginListParseException(tr("Failed to create plugin information from manifest for plugin ''{0}''", name), e);
        } catch (PluginException e) {
            throw new PluginListParseException(tr("Failed to create plugin information from manifest for plugin ''{0}''", name), e);
        }
    }

    /**
     * Parses a plugin information document and replies a list of plugin information objects.
     * 
     * See <a href="http://josm.openstreetmap.de/plugin">http://josm.openstreetmap.de/plugin</a>
     * for a sample of the document. The format is a custom format, kind of mix of CSV and RFC822 style
     * name/value-pairs.
     * 
     * @param in the input stream from which to parse
     * @return the list of plugin information objects
     * @throws PluginListParseException thrown if something goes wrong while parsing
     */
    public List<PluginInformation> parse(InputStream in) throws PluginListParseException{
        List<PluginInformation> ret = new LinkedList<PluginInformation>();
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            String name = null;
            String url = null;
            StringBuilder manifest = new StringBuilder();
            /*
            code structure:
                for () {
                    A;
                    B;
                    C;
                }
                B;
            */
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                if (line.startsWith("\t")) {
                    line = line.substring(1);
                    if (line.length() > 70) {
                        manifest.append(line.substring(0, 70)).append("\n");
                        line = " " + line.substring(70);
                    }
                    manifest.append(line).append("\n");
                    continue;
                }
                if (name != null) {
                    PluginInformation info = createInfo(name, url, manifest.toString());
                    if (info != null) {
                        for (PluginProxy plugin : PluginHandler.pluginList) {
                            if (plugin.getPluginInformation().name.equals(info.getName())) {
                                info.localversion = plugin.getPluginInformation().localversion;
                            }
                        }
                        ret.add(info);
                    }
                }
                String x[] = line.split(";");
                name = x[0];
                url = x[1];
                manifest = new StringBuilder();

            }
            if (name != null) {
                PluginInformation info = createInfo(name, url, manifest.toString());
                if (info != null) {
                    for (PluginProxy plugin : PluginHandler.pluginList) {
                        if (plugin.getPluginInformation().name.equals(info.getName())) {
                            info.localversion = plugin.getPluginInformation().localversion;
                        }
                    }
                    ret.add(info);
                }
            }
            return ret;
        } catch (IOException e) {
            throw new PluginListParseException(e);
        }
    }
}
