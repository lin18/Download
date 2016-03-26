package com.download;

/**
 *
 */
public class ProgressChangedEventArgs {
	private float Progress;
	private Object innerUserState;
	public ProgressChangedEventArgs(float percent,Object userState){
		this.Progress=percent;
		this.innerUserState=userState;
	}
	public float getProgress(){return this.Progress;}
	public Object getUserState(){return this.innerUserState;}
}
