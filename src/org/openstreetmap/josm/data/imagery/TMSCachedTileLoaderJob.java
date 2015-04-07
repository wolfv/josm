// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntry;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener;
import org.openstreetmap.josm.data.cache.JCSCachedTileLoaderJob;
import org.openstreetmap.josm.data.preferences.IntegerProperty;

/**
 * @author Wiktor Niesiobędzki
 *
 * Class bridging TMS requests to JCS cache requests
 *
 */
public class TMSCachedTileLoaderJob extends JCSCachedTileLoaderJob<String, BufferedImageCacheEntry> implements TileJob, ICachedLoaderListener  {
    private static final Logger log = FeatureAdapter.getLogger(TMSCachedTileLoaderJob.class.getCanonicalName());
    private Tile tile;
    private TileLoaderListener listener;
    private volatile URL url;

    /**
     * overrides the THREAD_LIMIT in superclass, as we want to have separate limit and pool for TMS
     */
    public static IntegerProperty THREAD_LIMIT = new IntegerProperty("imagery.tms.tmsloader.maxjobs", 25);
    /**
     * separate from JCS thread pool for TMS loader, so we can have different thread pools for default JCS
     * and for TMS imagery
     */
    private static ThreadPoolExecutor DOWNLOAD_JOB_DISPATCHER = new ThreadPoolExecutor(
            THREAD_LIMIT.get().intValue(), // keep the thread number constant
            THREAD_LIMIT.get().intValue(), // do not this number of threads
            30, // keepalive for thread
            TimeUnit.SECONDS,
            // make queue of LIFO type - so recently requested tiles will be loaded first (assuming that these are which user is waiting to see)
            new LinkedBlockingDeque<Runnable>(5) {
                /* keep the queue size fairly small, we do not want to
                 download a lot of tiles, that user is not seeing anyway */
                @Override
                public boolean offer(Runnable t) {
                    return super.offerFirst(t);
                }

                @Override
                public Runnable remove() {
                    return super.removeFirst();
                }
            }
            );

    /**
     * Constructor for creating a job, to get a specific tile from cache
     * @param listener
     * @param tile to be fetched from cache
     * @param cache object
     * @param connectTimeout when connecting to remote resource
     * @param readTimeout when connecting to remote resource
     * @param headers to be sent together with request
     */
    public TMSCachedTileLoaderJob(TileLoaderListener listener, Tile tile, ICacheAccess<String, BufferedImageCacheEntry> cache, int connectTimeout, int readTimeout,
            Map<String, String> headers) {
        super(cache, connectTimeout, readTimeout, headers);
        this.tile = tile;
        this.listener = listener;
    }

    @Override
    public Tile getTile() {
        return getCachedTile();
    }

    @Override
    public String getCacheKey() {
        if (tile != null)
            return tile.getKey();
        return null;
    }

    /*
     *  this doesn't needs to be synchronized, as it's not that costly to keep only one execution
     *  in parallel, but URL creation and Tile.getUrl() are costly and are not needed when fetching
     *  data from cache
     *
     *  We need to have static url value for TileLoaderJob, as for some TileSources we might get different
     *  URL's each call we made (servers switching), and URL's are used below as a key for duplicate detection
     *
     */
    @Override
    public URL getUrl() {
        if (url == null) {
            try {
                synchronized (this) {
                    if (url == null)
                        url = new URL(tile.getUrl());
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "JCS TMS Cache - error creating URL for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
                log.log(Level.INFO, "Exception: ", e);
            }
        }
        return url;
    }

    @Override
    public boolean isObjectLoadable() {
        if (cacheData != null) {
            byte[] content = cacheData.getContent();
            try {
                return content != null  || cacheData.getImage() != null || cacheAsEmpty();
            } catch (IOException e) {
                log.log(Level.WARNING, "JCS TMS - error loading from cache for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
            }
        }
        return false;
    }

    @Override
    protected boolean cacheAsEmpty() {
        if (attributes != null && attributes.isNoTileAtZoom()) {
            // do not remove file - keep the information, that there is no tile, for further requests
            // the code above will check, if this information is still valid
            log.log(Level.FINE, "JCS TMS - Tile valid, but no file, as no tiles at this level {0}", tile);
            tile.setError("No tile at this zoom level");
            tile.putValue("tile-info", "no-tile");
            return true;
        }
        return false; // as there is no other cache to cache the Tile, also cache other empty requests
    }

    @Override
    protected Executor getDownloadExecutor() {
        return DOWNLOAD_JOB_DISPATCHER;
    }

    public void submit() {
        tile.initLoading();
        super.submit(this);
    }

    @Override
    public void loadingFinished(CacheEntry object, boolean success) {
        try {
            loadTile(object, success);
            if (listener != null) {
                listener.tileLoadingFinished(tile, success);
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "JCS TMS - error loading object for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
            tile.setError(e.getMessage());
            tile.setLoaded(false);
            if (listener != null) {
                listener.tileLoadingFinished(tile, false);
            }
        }
    }

    /**
     * Method for getting the tile from cache only, without trying to reach remote resource
     * @return tile or null, if nothing (useful) was found in cache
     */
    public Tile getCachedTile() {
        BufferedImageCacheEntry data = get();
        if (isObjectLoadable()) {
            try {
                loadTile(data);
                return tile;
            } catch (IOException e) {
                log.log(Level.WARNING, "JCS TMS - error loading object for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
                return null;
            }

        } else {
            return null;
        }
    }

    // loads tile when calling back from cache
    private void loadTile(CacheEntry object, boolean success) throws IOException {
        tile.finishLoading();
        if (object != null) {
            byte[] content = object.getContent();
            if (content != null && content.length > 0) {
                tile.loadImage(new ByteArrayInputStream(content));
            }
        }
        if (!success) {
            tile.setError("Problem loading tile");
        }
    }

    // loads tile when geting stright from cache
    private void loadTile(BufferedImageCacheEntry object) throws IOException {
        tile.finishLoading();
        if (cacheAsEmpty() || object != null) { // if cache as empty object, do not try to load image
            if (object.getImage() != null) {
                tile.setImage(object.getImage());
            }
        }
    }

    @Override
    protected boolean handleNotFound() {
        tile.setError("No tile at this zoom level");
        tile.putValue("tile-info", "no-tile");
        return true;
    }

    /**
     * For TMS use BaseURL as settings discovery, so for different paths, we will have different settings (useful for developer servers)
     *
     * @return base URL of TMS or server url as defined in super class
     */
    @Override
    protected String getServerKey() {
        TileSource ts = tile.getSource();
        if (ts instanceof AbstractTMSTileSource) {
            return ((AbstractTMSTileSource) ts).getBaseUrl();
        }
        return super.getServerKey();
    }

    @Override
    protected BufferedImageCacheEntry createCacheEntry(byte[] content) {
        return new BufferedImageCacheEntry(content);
    }
}
