package edu.columbia.sel.facilitator.grout.util;

import java.io.File;

import org.apache.commons.io.FileUtils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * FolderDeleter is a class used for... deleting... a folder. Oh, Java.
 * @author Jonathan Wohl
 *
 */
public class FolderDeleter implements DeleterListener {

	private Handler mHandler;
	private DeleterListener mDl;
	private File mDir;

	/**
	 * Construct the FolderDeleter and setup the handler for communication between threads.
	 * @param dir
	 * @param mDl
	 */
	public FolderDeleter(File dir, DeleterListener mDl) {
		this.mDl = mDl;
		this.mDir = dir;

		mHandler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				// The only message that gets sent is when the directory deleted
				// is complete
				switch (msg.arg1) {
				case 0:
					onDeleteComplete();
					break;
				case 1:
					onDeleteError();
					break;
				default:
//					thread.
					break;
				}
			}
		};
	}
	
	/**
	 * Create the deleter runnable and spawn the new deleter thread.
	 */
	public void start() {
		onDeleteStart();
		DeleterRunnable dr = new DeleterRunnable(mDir);
		Thread thread = new Thread(dr);
		thread.start();
	}

	
	
	
	/*****************
	 * GETTERS & SETTERS
	 */
	
	public DeleterListener getDeleterListener() {
		return mDl;
	}

	public void setDeleterListener(DeleterListener mDl) {
		this.mDl = mDl;
	}

	
	
	
	/*****************
	 * LISTENERS
	 */
	
	public void onDeleteStart() {
		if (mDl != null) {
			mDl.onDeleteStart();
		}
	}

	public void onDeleteComplete() {
		if (mDl != null) {
			mDl.onDeleteComplete();
		}
	}

	public void onDeleteError() {
		if (mDl != null) {
			mDl.onDeleteError();
		}
	}
	
	
	
	/******************
	 * INNER CLASSES
	 */

	/**
	 * DeleterRunnable is a directory deleter designed to run on a separate thread so as not to block the main thread.
	 * @author Jonathan Wohl
	 *
	 */
	public class DeleterRunnable implements Runnable {

		private File dir;

		public DeleterRunnable(File dir) {
			this.dir = dir;
		}

		/**
		 * Delete a directory and its contents. Returns true on success, false
		 * otherwise.
		 * 
		 * @param dir
		 * @return boolean
		 */
		public boolean deleteDirectory(File dir) {
			try {
				FileUtils.deleteDirectory(dir);
				mHandler.sendEmptyMessage(0);
			} catch (Exception e) {
				mHandler.sendEmptyMessage(1);
				return false;
			}
			return true;
		}

		public void run() {
			deleteDirectory(this.dir);
		}

	}
}
