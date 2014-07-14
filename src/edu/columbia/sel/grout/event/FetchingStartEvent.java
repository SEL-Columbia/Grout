/**
 * 
 * Copyright (c) 2014 Jonathan Wohl, Sustainable Engineering Lab, Columbia University
 * 
 * See the file LICENSE for copying permission.
 * 
 */
package edu.columbia.sel.grout.event;

public class FetchingStartEvent {
	public int total;
	
	public FetchingStartEvent() {
	}
	
	public FetchingStartEvent(int pTotal) {
		total = pTotal;
	}
}
