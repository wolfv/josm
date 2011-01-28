// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.gui.mappaint.xml.XmlStyleSource;
import org.openstreetmap.josm.gui.mappaint.xml.XmlStyleSourceHandler;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.MapPaintPreference.MapPaintPrefMigration;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class MapPaintStyles {

    private static ElemStyles styles = new ElemStyles();
    private static Collection<String> iconDirs;

    public static ElemStyles getStyles()
    {
        return styles;
    }
    
    public static class IconReference {

        public String iconName;
        public XmlStyleSource source;

        public IconReference(String iconName, XmlStyleSource source) {
            this.iconName = iconName;
            this.source = source;
        }
    }

    public static ImageIcon getIcon(IconReference ref)
    {
        String styleName = ref.source.getPrefName();
        List<String> dirs = new LinkedList<String>();
        for(String fileset : iconDirs)
        {
            String[] a;
            if(fileset.indexOf("=") >= 0) {
                a = fileset.split("=", 2);
            } else {
                a = new String[] {"", fileset};
            }

            /* non-prefixed path is generic path, always take it */
            if(a[0].length() == 0 || styleName.equals(a[0])) {
                dirs.add(a[1]);
            }
        }
        ImageIcon i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, ref.iconName, ref.source.zipIcons);
        if(i == null)
        {
            System.out.println("Mappaint style \""+styleName+"\" icon \"" + ref.iconName + "\" not found.");
            i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, "misc/no_icon.png");
        }
        return i;
    }

    @SuppressWarnings("null")
    public static void readFromPreferences() {
        iconDirs = Main.pref.getCollection("mappaint.icon.sources", Collections.<String>emptySet());
        if(Main.pref.getBoolean("mappaint.icon.enable-defaults", true))
        {
            LinkedList<String> f = new LinkedList<String>(iconDirs);
            /* don't prefix icon path, as it should be generic */
            f.add("resource://images/styles/standard/");
            f.add("resource://images/styles/");
            iconDirs = f;
        }

        Collection<? extends SourceEntry> sourceEntries = (new MapPaintPrefMigration()).get();

        for (SourceEntry entry : sourceEntries) {
            XmlStyleSource style = new XmlStyleSource(entry);
            try {
                XmlObjectParser parser = new XmlObjectParser(new XmlStyleSourceHandler(style));
                MirroredInputStream in = new MirroredInputStream(entry.url);
                InputStream zip = in.getZipEntry("xml","style");
                InputStreamReader ins;
                if(zip != null)
                {
                    style.zipIcons = in.getFile();
                    ins = new InputStreamReader(zip);
                } else {
                    ins = new InputStreamReader(in);
                }
                parser.startWithValidation(ins, "http://josm.openstreetmap.de/mappaint-style-1.0",
                "resource://data/mappaint-style.xsd");
                while(parser.hasNext()) {
                }
            } catch(IOException e) {
                System.err.println(tr("Warning: failed to load Mappaint styles from ''{0}''. Exception was: {1}", entry.url, e.toString()));
                e.printStackTrace();
                style.hasError = true;
            } catch(SAXParseException e) {
                System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: [{1}:{2}] {3}", entry.url, e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
                e.printStackTrace();
                style.hasError = true;
            } catch(SAXException e) {
                System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: {1}", entry.url, e.getMessage()));
                e.printStackTrace();
                style.hasError = true;
            }
            styles.add(style);
        }
    }
}
