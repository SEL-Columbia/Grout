package edu.columbia.sel.facilitator.grout.util;

import java.io.File;

import org.apache.commons.io.FileUtils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class FolderDeleter implements DeleterListener {
	
	private Handler mHandler; 
	
	public FolderDeleter(File dir, DeleterListener mDl) {
		this.mDl = mDl;
		
		onDeleteStart();
		
		DeleterRunnable dr = new DeleterRunnable(dir);
		Thread thread = new Thread(dr);
		thread.start();
		
		mHandler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				onDeleteComplete();
			}
		};
	}

	DeleterListener mDl;
	
	public DeleterListener getDeleterListener() {
		return mDl;
	}

	public void setDeleterListener(DeleterListener mDl) {
		this.mDl = mDl;
	}

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
	
	public class DeleterRunnable implements Runnable {

		private File dir;
		
		public DeleterRunnable(File dir) {
			this.dir = dir;
		}
		
		/**
		 * Delete a directory and its contents. Returns true on success, false otherwise.
		 * @param dir
		 * @return boolean
		 */
		public boolean deleteDirectory(File dir) {
//			this.onDeleteStart();
			try {
				FileUtils.deleteDirectory(dir);
//				this.onDeleteComplete();
				mHandler.sendEmptyMessage(0);
			} catch(Exception e) {
				return false;
			}
			return true;
		}
		
	    public void run() {
	        deleteDirectory(this.dir);
	    }

	}
}
