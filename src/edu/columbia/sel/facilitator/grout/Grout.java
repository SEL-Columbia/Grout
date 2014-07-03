/**
 * 
 * Copyright (c) 2014 Jonathan Wohl, Sustainable Engineering Lab, Columbia University
 * 
 * See the file LICENSE for copying permission.
 * 
 */
package edu.columbia.sel.facilitator.grout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.columbia.sel.facilitator.grout.event.FetchingErrorEvent;
import edu.columbia.sel.facilitator.grout.event.FetchingProgressEvent;
import edu.columbia.sel.facilitator.grout.event.FetchingStartEvent;
import edu.columbia.sel.facilitator.grout.util.DbCreator;
import edu.columbia.sel.facilitator.grout.util.DeleterListener;
import edu.columbia.sel.facilitator.grout.util.FolderDeleter;
import edu.columbia.sel.facilitator.grout.util.FolderZipper;
import edu.columbia.sel.facilitator.grout.util.TileUtils;

import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GEMFFile;

import android.os.Environment;
import android.util.Log;

public class Grout implements TileFetchingListener, DeleterListener {
	// ===========================================================
	// Constants
	// ===========================================================
	private final String TAG = this.getClass().getCanonicalName();
	
	// TODO: what is the proper way to select the default number of threads?
	private static final int DEFAULT_THREADCOUNT = 100;
	
	// By default we use the mapquest tile server (http://developer.mapquest.com/web/products/open/map)
	// TODO: this tile server serves JPGs regardless of the .png extension... it works fine unless we try
	// to archive the resulting files (known bug in osmdroid: https://github.com/osmdroid/osmdroid/issues/18)
	private static final String DEFAULT_SERVER_URL = "http://otile1.mqcdn.com/tiles/1.0.0/map/%d/%d/%d.png";
	
	// Root directory to save files
	private static final String DEFAULT_ROOT_DIR = Environment.getExternalStorageDirectory().toString() + "/osmdroid/tiles";
	
	// Default maximum tiles
	private static final int DEFAULT_MAX_TILES = 50000;

	// ===========================================================
	// Fields
	// ===========================================================
	
	private String mServerURL = DEFAULT_SERVER_URL;
	private String mRootDownloadDir = DEFAULT_ROOT_DIR;
	private String mDestinationFile = null;
	private String mTempFolder = "OfflineTiles";
	private String mFileAppendix = ".tile";
	private BoundingBoxE6 mBoundingBox;
	private Double mNorth = null;
	private Double mSouth = null;
	private Double mEast = null;
	private Double mWest = null;
	private Integer mMaxZoom = 16;
	private int mMinZoom = 8;
	private int mThreadCount = DEFAULT_THREADCOUNT;
	private int mMaxTiles = DEFAULT_MAX_TILES;
	private int mTotalExpected;
	private int mRemaining;
	
	private boolean mIsRunning = false;
	
	private TileFetchingListener mListener;
	private DeleterListener mDeleterListener;
	
	private DownloadManager dm;
	private FolderDeleter fd;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Grout() {
		Log.i(TAG, "++++++++++++ Creating Tile Packager");
//		dm = new DownloadManager(this, pBaseURL, pTempBaseURL, pThreadCount);
	}

	public Grout(Double north, Double south, Double east, Double west) {
		this.mNorth = north;
		this.mSouth = south;
		this.mEast = east;
		this.mWest = west;
	}

	public Grout(BoundingBoxE6 bb) {
		this.mBoundingBox = bb;
		this.mNorth = (bb.getLatNorthE6() / 1E6);
		this.mSouth = (bb.getLatSouthE6() / 1E6);
		this.mEast = (bb.getLonEastE6() / 1E6);
		this.mWest = (bb.getLonWestE6() / 1E6);
	}

		

	
	
	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public TileFetchingListener getTileFetchingListener() {
		return mListener;
	}

	public void setTileFetchingListener(TileFetchingListener mListener) {
		this.mListener = mListener;
	}
	
	public DeleterListener getDeleterListener() {
		return mDeleterListener;
	}

	public void setDeleterListener(DeleterListener deleterListener) {
		this.mDeleterListener = deleterListener;
	}

	public void setBoundingBox(BoundingBoxE6 bb) {
		this.mBoundingBox = bb;
		this.mNorth = (bb.getLatNorthE6() / 1E6);
		this.mSouth = (bb.getLatSouthE6() / 1E6);
		this.mEast = (bb.getLonEastE6() / 1E6);
		this.mWest = (bb.getLonWestE6() / 1E6);
	}

	public String getServerURL() {
		return this.mServerURL;
	}

	public void setServerURL(String serverURL) {
		this.mServerURL = serverURL;
	}

	public String getDestinationFile() {
		return mDestinationFile;
	}

	public void setDestinationFile(String destinationFile) {
		this.mDestinationFile = destinationFile;
	}

	public String getTempFolder() {
		return mTempFolder;
	}

	public void setTempFolder(String tempFolder) {
		this.mTempFolder = tempFolder;
	}

	public String getRootDownloadDir() {
		return mRootDownloadDir;
	}

	public void setRootDownloadDir(String mRootDownloadDir) {
		this.mRootDownloadDir = mRootDownloadDir;
	}

	public String getFileAppendix() {
		return mFileAppendix;
	}

	public void setFileAppendix(String fileAppendix) {
		this.mFileAppendix = fileAppendix;
	}

	public Double getNorth() {
		return mNorth;
	}

	public void setNorth(Double north) {
		this.mNorth = north;
	}

	public Double getSouth() {
		return mSouth;
	}

	public void setSouth(Double south) {
		this.mSouth = south;
	}

	public Double getEast() {
		return mEast;
	}

	public void setEast(Double east) {
		this.mEast = east;
	}

	public Double getWest() {
		return mWest;
	}

	public void setWest(Double west) {
		this.mWest = west;
	}

	public Integer getMaxzoom() {
		return mMaxZoom;
	}

	public void setMaxzoom(Integer maxzoom) {
		this.mMaxZoom = maxzoom;
	}

	public int getMinzoom() {
		return mMinZoom;
	}

	public void setMinzoom(int minzoom) {
		this.mMinZoom = minzoom;
	}

	public int getThreadCount() {
		return mThreadCount;
	}

	public void setThreadCount(int threadCount) {
		this.mThreadCount = threadCount;
	}
	
	public String getFullTempPath() {
		return this.mRootDownloadDir + "/" + this.mTempFolder;
	}
	
	public String getFullDestinationFilePath() {
		return this.mRootDownloadDir + "/" + this.mDestinationFile;
	}
	
	
	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Kick off the process of downloading tiles.
	 */
	public void run() {
		// perform validity checks before attempting to download tiles
		if (!isValidForDownload()) {
			Log.e(TAG, "Select is invalid for download.");
			return;
		}

		// build full paths using the specified root
		String fullTempPath = getFullTempPath();
		String fullDestinationFilePath = getFullDestinationFilePath();

//		// remove previously cached files
//		// TODO: Make this an option?
//		Log.i(TAG, "----------------------- DELETING TILES in " + fullTempPath);
//		clearOfflineTiles();

		// download tiles for selected region
		Log.i(TAG, "----------------------- DOWNLOADING TILES in " + this.mTempFolder);
		downloadTiles(mServerURL, fullTempPath, mThreadCount, mFileAppendix, mMinZoom, mMaxZoom, mNorth, mSouth,
				mEast, mWest);
		
		// if a destination file is specified, create it from the downloaded tiles
		if (mDestinationFile != null) {
			if (mDestinationFile.endsWith(".zip")) {
				Log.i(TAG, "----------------------- ZIPPING TILES in " + this.mTempFolder);
				createZipFile(fullTempPath, fullDestinationFilePath);
			} else if (mDestinationFile.endsWith(".gemf")) {
				createGemfFile(fullTempPath, fullDestinationFilePath);
			} else {
				createDb(fullTempPath, fullDestinationFilePath);
			}
	
			if (mServerURL != null) {
				clearOfflineTiles();
			}
		}
	}
	
	/**
	 * Cancel the current download.
	 */
	public void cancel() {
		if (mIsRunning) {
			mIsRunning = false;
			dm.cancel();
		}
	}

	/**
	 * Check if the specified area is suitable for download, emitting error events. 
	 * TODO: determine some maximum values for number of tiles.
	 * 
	 * @return
	 */
	public Boolean isValidForDownload() {

		// check bounds
		if (mNorth == null || mSouth == null || mEast == null || mWest == null) {
			this.onFetchingError(new FetchingErrorEvent(FetchingErrorEvent.INVALID_REGION));
			return false;
		}
		
		// check if a download is already running
		if (mIsRunning) {
			Log.e(TAG, "Tile Packager is already running.");
			this.onFetchingError(new FetchingErrorEvent(FetchingErrorEvent.ALREADY_RUNNING));
			return false;
		}
	
		// check if the region is within the maximum limit
		if (!isWithinMaxRegionSize()) {
			this.onFetchingError(new FetchingErrorEvent(FetchingErrorEvent.MAX_REGION_SIZE_EXCEEDED));
			return false;
		}
		
		return true;
	}

	/**
	 * Check if the specified area contains fewer tiles than the specified max.
	 * TODO: determine some reasonable maximum values for number of tiles.
	 * 
	 * @return
	 */
	public Boolean isWithinMaxRegionSize() {
		// check that expected num tiles < max
		mTotalExpected = this.getExpectedFileCount(mMinZoom, mMaxZoom, mNorth, mSouth, mEast, mWest);
		if (mTotalExpected > mMaxTiles) {
			return false;
		}
		
		return true;
	}
	
	private void checkFileExistence() {
		String fullTempPath = getFullTempPath();
	
		// count files in the temp folder
		Log.i(TAG, "-------------> Counting existing files ...");
		int actualFileCount = TileUtils.getTotalRecursiveFileCount(new File(fullTempPath));
		if (mTotalExpected == actualFileCount) {
			Log.i(TAG, "-------------> SUCCESS! Total (" + actualFileCount + ") == Expected (" + mTotalExpected + ").");
		} else {
			Log.i(TAG, "-------------> FAIL! Total (" + actualFileCount + ") != Expected (" + mTotalExpected + ").");
		}
	}

	private void createGemfFile(final String pTempFolder, final String pDestinationFile) {
		try {
			Log.i(TAG, "Creating GEMF archive from " + mTempFolder + " to " + mDestinationFile + " ...");
			final List<File> sourceFolders = new ArrayList<File>();
			sourceFolders.add(new File(pTempFolder));
			final GEMFFile file = new GEMFFile(pDestinationFile, sourceFolders);
			Log.i(TAG, " done.");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void createZipFile(final String pTempFolder, final String pDestinationFile) {
		try {
			Log.i(TAG, "Zipping files to " + pDestinationFile + " ...");
			FolderZipper.zipFolderToFile(new File(pDestinationFile), new File(pTempFolder));
			Log.i(TAG, " done.");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void createDb(final String pTempFolder, final String pDestinationFile) {
		try {
			Log.i(TAG, "Putting files into db : " + pDestinationFile + " ...");
			DbCreator.putFolderToDb(new File(pDestinationFile), new File(pTempFolder));
			Log.i(TAG, " done.");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Deletes all cached tiles.
	 */
	public void clearOfflineTiles() {
		String fullTempPath = getFullTempPath();
		Log.i(TAG, "-------------> CLEARING OFFLINE TILES in " + fullTempPath);
		fd = new FolderDeleter(new File(fullTempPath), this.mDeleterListener);
//		TileUtils.deleteDirectory(new File(fullTempPath));
	}
	
	/**
	 * Returns the number of cached tiles currently downloaded.
	 * @return int
	 */
	public int countCachedTiles() {
		String fullTempPath = getFullTempPath();
		Log.i(TAG, "-------------> Counting tiles in " + fullTempPath);
		return TileUtils.getTotalRecursiveFileCount(new File(fullTempPath));
	}
	
	/**
	 * Public method for creating a Zip file from the cached tiles.
	 */
	public void createZipFile() {
		Log.i(TAG, "-------------> Creating Zip");
		this.createZipFile(getFullTempPath(), getFullDestinationFilePath());
	}
	
	/**
	 * Public method for creating a GEMF file from the cached tiles.
	 */
	public void createGemfFile() {
		Log.i(TAG, "-------------> Creating GEMF");
		this.createGemfFile(getFullTempPath(), getFullDestinationFilePath());
	}

	/**
	 * Kicks off the actual download of the tiles for the selected region.
	 * 
	 * @param pBaseURL
	 * @param pTempFolder
	 * @param pThreadCount
	 * @param pFileAppendix
	 * @param pMinZoom
	 * @param pMaxZoom
	 * @param pNorth
	 * @param pSouth
	 * @param pEast
	 * @param pWest
	 */
	private void downloadTiles(final String pBaseURL, final String pTempFolder, final int pThreadCount,
			final String pFileAppendix, final int pMinZoom, final int pMaxZoom, final double pNorth,
			final double pSouth, final double pEast, final double pWest) {
		
		// Trigger start event
		this.onFetchingStart(new FetchingStartEvent(this.mTotalExpected));
		
		final String pTempBaseURL = pTempFolder + File.separator + "%d" + File.separator + "%d" + File.separator + "%d"
				+ pBaseURL.substring(pBaseURL.lastIndexOf('.'))
				+ pFileAppendix.replace(File.separator + File.separator, File.separator);

		// TODO: Possible memory leak. The download manager probably shouldn't get reinstantiated each time. 
		dm = new DownloadManager(this, pBaseURL, pTempBaseURL, pThreadCount);

		// Add all of the tiles to the queue for download
		for (int z = pMinZoom; z <= pMaxZoom; z++) {
			final OSMTileInfo upperLeft = TileUtils.getMapTileFromCoordinates(pNorth, pWest, z);
			final OSMTileInfo lowerRight = TileUtils.getMapTileFromCoordinates(pSouth, pEast, z);

			Log.i(TAG, "Adding ZoomLevel: " + z + " ");
			for (int x = upperLeft.x; x <= lowerRight.x; x++) {
				for (int y = upperLeft.y; y <= lowerRight.y; y++) {
					dm.add(new OSMTileInfo(x, y, z));
				}
			}
		}
	}

	/**
	 * Given a min/max zoom and a bounding box, how many tiles will we be downloading?
	 * 
	 * @param pMinZoom
	 * @param pMaxZoom
	 * @param pNorth
	 * @param pSouth
	 * @param pEast
	 * @param pWest
	 * @return int Expected number of images.
	 */
	private int getExpectedFileCount(final int pMinZoom, final int pMaxZoom, final double pNorth, final double pSouth,
			final double pEast, final double pWest) {
		/* Calculate file-count. */
		int fileCnt = 0;
		for (int z = pMinZoom; z <= pMaxZoom; z++) {
			final OSMTileInfo upperLeft = TileUtils.getMapTileFromCoordinates(pNorth, pWest, z);
			final OSMTileInfo lowerRight = TileUtils.getMapTileFromCoordinates(pSouth, pEast, z);

			final int dx = lowerRight.x - upperLeft.x + 1;
			final int dy = lowerRight.y - upperLeft.y + 1;
			fileCnt += dx * dy;
		}

		return fileCnt;
	}
	
	// ===========================================================
	// Listener Implementations
	// ===========================================================
	
	public void onTileDownloaded() {
		if (this.mRemaining > 0) {
			this.mRemaining -= 1;
		}
		if (this.mRemaining == 0) {
			this.onFetchingComplete();
		}
		FetchingProgressEvent fpe = new FetchingProgressEvent();
		this.onFetchingProgress(fpe);
		if (mListener != null) {
			mListener.onTileDownloaded();			
		}
	}

	public void onFetchingStart(FetchingStartEvent fse) {
		mIsRunning = true;
		this.mRemaining = this.mTotalExpected;
		Log.i(TAG, "-----------> onFetchingStart: total: " + this.mTotalExpected + ", remaining: " + this.mRemaining);
		fse.total = this.mTotalExpected;
		if (mListener != null) {
			mListener.onFetchingStart(fse);
		}
	}

	public void onFetchingStop() {
		mIsRunning = false;
		if (mListener != null) {
			mListener.onFetchingStop();
		}
	}

	public void onFetchingComplete() {
		// Let's check the number of files that have downloaded:
		this.checkFileExistence();
		if (mListener != null) {
			mListener.onFetchingComplete();
		}
		this.onFetchingStop();
	}

	public void onFetchingProgress(FetchingProgressEvent fpe) {
		fpe.completed = this.mTotalExpected - this.mRemaining;
		fpe.total = this.mTotalExpected;
		fpe.percent = (float)fpe.completed / (float)fpe.total;
		if (mListener != null) {
			mListener.onFetchingProgress(fpe);
		}
	}

	public void onFetchingError(FetchingErrorEvent fee) {
		if (mListener != null) {
			mListener.onFetchingError(fee);
		}
	}

	public void onDeleteStart() {
		// TODO Auto-generated method stub
		
	}

	public void onDeleteComplete() {
		// TODO Auto-generated method stub
		
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
