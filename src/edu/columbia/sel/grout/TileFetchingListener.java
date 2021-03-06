/**
 * 
 * Copyright (c) 2014 Jonathan Wohl, Sustainable Engineering Lab, Columbia University
 * 
 * See the file LICENSE for copying permission.
 * 
 */
package edu.columbia.sel.grout;

import edu.columbia.sel.grout.event.FetchingErrorEvent;
import edu.columbia.sel.grout.event.FetchingProgressEvent;
import edu.columbia.sel.grout.event.FetchingStartEvent;

public interface TileFetchingListener {
	public void onTileDownloaded();
	
	public void onFetchingStart(FetchingStartEvent fse);
	
	public void onFetchingStop();
	
	public void onFetchingComplete();
	
	public void onFetchingProgress(FetchingProgressEvent fpe);
	
	public void onFetchingError(FetchingErrorEvent fee);
}
