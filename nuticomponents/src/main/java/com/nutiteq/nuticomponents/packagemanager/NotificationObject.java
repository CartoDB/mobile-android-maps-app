package com.nutiteq.nuticomponents.packagemanager;

import java.util.ArrayList;

public class NotificationObject {

	public int id;// for notification id
	public String packageId;
	public int position;// for position in package list
	public int level;
	public ArrayList<StackObject> stack;
	public boolean isRoutingPkg;

	public boolean shouldCancel = false;
	public boolean isDownloadFailed = false;
	public boolean isPausedShow = false;

	public NotificationObject(int id, String packageId, int position,
			int level, ArrayList<StackObject> stack, boolean isRoutingPkg) {
		this.id = id;
		this.packageId = packageId;
		this.position = position;
		this.level = level;
		this.stack = stack;
		this.isRoutingPkg = isRoutingPkg;
	}
}
