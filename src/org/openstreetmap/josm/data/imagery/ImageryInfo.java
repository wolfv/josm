// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class that stores info about an image background layer.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class ImageryInfo implements Comparable<ImageryInfo> {

    public enum ImageryType {
        WMS("wms"),
        TMS("tms"),
        HTML("html"),
        BING("bing");

        private String urlString;
        ImageryType(String urlString) {
            this.urlString = urlString;
        }
        public String getUrlString() {
            return urlString;
        }
    }

    private final static String[] BLACKLIST_REGEXES = {
        // These entries are for Google tile servers (names and IPV4 numbers)
        ".*\\.google\\.com/.*",
        ".*209\\.85\\.2\\d\\d.*",
        ".*209\\.85\\.1[3-9]\\d.*",
        ".*209\\.85\\.12[89].*"
    };

    String name;
    String url = null;
    String cookies = null;
    public final String eulaAcceptanceRequired;
    ImageryType imageryType = ImageryType.WMS;
    double pixelPerDegree = 0.0;
    int maxZoom = 0;
    private boolean blacklisted = false;

    public ImageryInfo(String name) {
        this.name=name;
        this.eulaAcceptanceRequired = null;
    }

    public ImageryInfo(String name, String url) {
        this.name=name;
        setUrl(url);
        this.eulaAcceptanceRequired = null;
    }

    public ImageryInfo(String name, String url, String eulaAcceptanceRequired) {
        this.name=name;
        setUrl(url);
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
    }

    public ImageryInfo(String name, String url, String eulaAcceptanceRequired, String cookies) {
        this.name=name;
        setUrl(url);
        this.cookies=cookies;
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
    }

    public ImageryInfo(String name, String url, String cookies, double pixelPerDegree) {
        this.name=name;
        setUrl(url);
        this.cookies=cookies;
        this.pixelPerDegree=pixelPerDegree;
        this.eulaAcceptanceRequired = null;
    }

    public ArrayList<String> getInfoArray() {
        String e2 = null;
        String e3 = null;
        String e4 = null;
        if(url != null && !url.isEmpty()) {
            e2 = getFullUrl();
        }
        if(cookies != null && !cookies.isEmpty()) {
            e3 = cookies;
        }
        if(imageryType == ImageryType.WMS || imageryType == ImageryType.HTML) {
            if(pixelPerDegree != 0.0) {
                e4 = String.valueOf(pixelPerDegree);
            }
        } else {
            if(maxZoom != 0) {
                e4 = String.valueOf(maxZoom);
            }
        }
        if(e4 != null && e3 == null) {
            e3 = "";
        }
        if(e3 != null && e2 == null) {
            e2 = "";
        }

        ArrayList<String> res = new ArrayList<String>();
        res.add(name);
        if(e2 != null) {
            res.add(e2);
        }
        if(e3 != null) {
            res.add(e3);
        }
        if(e4 != null) {
            res.add(e4);
        }
        return res;
    }

    public ImageryInfo(Collection<String> list) {
        ArrayList<String> array = new ArrayList<String>(list);
        this.name=array.get(0);
        if(array.size() >= 2) {
            setUrl(array.get(1));
        }
        if(array.size() >= 3) {
            this.cookies=array.get(2);
        }
        if(array.size() >= 4) {
            if (imageryType == ImageryType.WMS || imageryType == ImageryType.HTML) {
                this.pixelPerDegree=Double.valueOf(array.get(3));
            } else {
                this.maxZoom=Integer.valueOf(array.get(3));
            }
        }
        this.eulaAcceptanceRequired = null;
    }

    public ImageryInfo(ImageryInfo i) {
        this.name=i.name;
        this.url=i.url;
        this.cookies=i.cookies;
        this.imageryType=i.imageryType;
        this.pixelPerDegree=i.pixelPerDegree;
        this.eulaAcceptanceRequired = null;
    }

    @Override
    public int compareTo(ImageryInfo in)
    {
        int i = name.compareTo(in.name);
        if(i == 0) {
            i = url.compareTo(in.url);
        }
        if(i == 0) {
            i = Double.compare(pixelPerDegree, in.pixelPerDegree);
        }
        return i;
    }

    public boolean equalsBaseValues(ImageryInfo in)
    {
        return url.equals(in.url);
    }

    public void setPixelPerDegree(double ppd) {
        this.pixelPerDegree = ppd;
    }

    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }

    public void setUrl(String url) {

        // determine if URL is on blacklist and flag accordingly.
        blacklisted = false;
        for (String blacklistRegex : BLACKLIST_REGEXES) {
            if (url.matches(blacklistRegex)) {
                blacklisted = true;
                System.err.println("layer '" + name + "' uses blacklisted URL");
                break;
            }
        }

        for (ImageryType type : ImageryType.values()) {
            if (url.startsWith(type.getUrlString() + ":")) {
                this.url = url.substring(type.getUrlString().length() + 1);
                this.imageryType = type;
                return;
            }
        }

        // Default imagery type is WMS
        this.url = url;
        this.imageryType = ImageryType.WMS;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return this.url;
    }

    public String getCookies() {
        return this.cookies;
    }

    public double getPixelPerDegree() {
        return this.pixelPerDegree;
    }

    public int getMaxZoom() {
        return this.maxZoom;
    }

    public String getFullUrl() {
        return imageryType.getUrlString() + ":" + url;
    }

    public String getToolbarName()
    {
        String res = name;
        if(pixelPerDegree != 0.0) {
            res += "#PPD="+pixelPerDegree;
        }
        return res;
    }

    public String getMenuName()
    {
        String res = name;
        if(pixelPerDegree != 0.0) {
            res += " ("+pixelPerDegree+")";
        } else if(maxZoom != 0) {
            res += " (z"+maxZoom+")";
        }
        return res;
    }

    public ImageryType getImageryType() {
        return imageryType;
    }

    public static boolean isUrlWithPatterns(String url) {
        return url != null && url.contains("{") && url.contains("}");
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }
}
