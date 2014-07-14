package edu.columbia.sel.grout.util;

public interface DeleterListener {
	public void onDeleteStart();
	public void onDeleteComplete();
	public void onDeleteError();
}