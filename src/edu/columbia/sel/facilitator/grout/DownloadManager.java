/**
 * 
 * Copyright (c) 2014 Jonathan Wohl, Sustainable Engineering Lab, Columbia University
 * 
 * See the file LICENSE for copying permission.
 * 
 */
package edu.columbia.sel.facilitator.grout;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.osmdroid.tileprovider.util.StreamUtils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class DownloadManager {
	// ===========================================================
	// Constants
	// ===========================================================
	private final String TAG = this.getClass().getCanonicalName();

	// ===========================================================
	// Fields
	// ===========================================================

	private Grout mGrout;

	private final ExecutorService mThreadPool;

	private final Queue<OSMTileInfo> mQueue = new LinkedBlockingQueue<OSMTileInfo>();

	private final String mBaseURL;
	private final String mDestinationURL;

	private Handler mHandler;

	// ===========================================================
	// Constructors
	// ===========================================================

	public DownloadManager(Grout tp, final String pBaseURL, final String pDestinationURL,
			final int mThreads) {
		
		this.mGrout = tp;
		this.mBaseURL = pBaseURL;
		this.mDestinationURL = pDestinationURL;
		this.mThreadPool = Executors.newFixedThreadPool(mThreads);
//		this.mThreadPool.

		mHandler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				mGrout.onTileDownloaded();
				Bundle bundle = msg.getData();
				int count = bundle.getInt("count");
				String ti = bundle.getString("tileInfo");
				Log.i(TAG, "-------------> " + ti);
			}
		};
	}
	
	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public synchronized void add(final OSMTileInfo pTileInfo) {
		// Log.i(TAG, "+++++++ Adding Tiles");
		this.mQueue.add(pTileInfo);
		spawnNewThread();
	}

	private synchronized OSMTileInfo getNext() {
		final OSMTileInfo tile = this.mQueue.poll();

		final int remaining = this.mQueue.size();
		if (remaining % 10 == 0 && remaining > 0) {
//			Log.i(TAG, "(" + remaining + ")");
		} else {
//			Log.i(TAG, ".");
		}

		this.notify();
		return tile;
	}
	
	public void cancel() {
		this.mThreadPool.shutdownNow();
	}
	
	/**
	 * Kill the unused threads
	 */
	public void cleanUp() {
		this.mThreadPool.shutdownNow();
	}

	public synchronized void waitEmpty() throws InterruptedException {
		while (this.mQueue.size() > 0) {
			this.wait();
		}
	}

	public void waitFinished() throws InterruptedException {
		waitEmpty();
		this.mThreadPool.shutdown();
		this.mThreadPool.awaitTermination(6, TimeUnit.HOURS);
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	private void spawnNewThread() {
		this.mThreadPool.execute(new DownloadRunner());
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private class DownloadRunner implements Runnable {

		private OSMTileInfo mTileInfo;
		private File mDestinationFile;

		public DownloadRunner() {
		}

		private void init(final OSMTileInfo pTileInfo) {
			this.mTileInfo = pTileInfo;
			/* Create destination file. */
			final String filename = String.format(DownloadManager.this.mDestinationURL, this.mTileInfo.zoom,
					this.mTileInfo.x, this.mTileInfo.y);
			this.mDestinationFile = new File(filename);

			final File parent = this.mDestinationFile.getParentFile();
			parent.mkdirs();
		}

		/**
		 * TODO: Maybe clean the actual downloading up?
		 */
		public void run() {
			InputStream in = null;
			OutputStream out = null;

			init(DownloadManager.this.getNext());

			if (mDestinationFile.exists()) {
				return; // TODO issue 70 - make this an option
			}

			final String finalURL = String.format(DownloadManager.this.mBaseURL, this.mTileInfo.zoom, this.mTileInfo.x,
					this.mTileInfo.y);

			try {
				in = new BufferedInputStream(new URL(finalURL).openStream(), StreamUtils.IO_BUFFER_SIZE);

				final FileOutputStream fileOut = new FileOutputStream(this.mDestinationFile);
				out = new BufferedOutputStream(fileOut, StreamUtils.IO_BUFFER_SIZE);

				StreamUtils.copy(in, out);

				out.flush();

				Message msg = new Message();
				Bundle bundle = new Bundle();
				bundle.putInt("count", mQueue.size());
				bundle.putString("tileInfo", mTileInfo.toString());
				msg.setData(bundle);
				mHandler.sendMessage(msg);
			} catch (final Exception e) {
				// System.err.println("Error downloading: '" + this.mTileInfo +
				// "' from URL: " + finalURL + " : " + e);
				Log.e(TAG, "Error downloading: '" + this.mTileInfo + "' from URL: " + finalURL + " : " + e);
				DownloadManager.this.add(this.mTileInfo); // try again later
			} finally {
				StreamUtils.closeStream(in);
				StreamUtils.closeStream(out);
			}
		}
	}
}
