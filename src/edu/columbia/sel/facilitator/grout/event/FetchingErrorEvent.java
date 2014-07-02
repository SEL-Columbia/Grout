/**
 * 
 * Copyright (c) 2014 Jonathan Wohl, Sustainable Engineering Lab, Columbia University
 * 
 * See the file LICENSE for copying permission.
 * 
 */
package edu.columbia.sel.facilitator.grout.event;

public class FetchingErrorEvent {
	public static final int INVALID_REGION = 1;
	public static final int MAX_REGION_SIZE_EXCEEDED = 2;
	public static final int ALREADY_RUNNING = 3;
	
	public int cause;
	
	public FetchingErrorEvent(int cause) {
		this.cause = cause;
	}
}
