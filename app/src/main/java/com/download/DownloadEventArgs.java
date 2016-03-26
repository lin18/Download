package com.download;

/**
 *
 */
public class DownloadEventArgs {
	private Object innerUserState;
	private Exception innerEx;
	private String innerResult;
	private boolean innerIsCancelled;
	
	public DownloadEventArgs(String result,Exception ex,Object userState,boolean cancelled){
		innerUserState=userState;
		innerEx=ex;
		innerResult=result;
		this.innerIsCancelled=cancelled;
	}
	
	public Object GetUserState(){return innerUserState;}
	public Object GetIsCancelled(){return innerIsCancelled;}
	public Exception GetException(){return innerEx;}
	public String GetResult()throws Exception{
		ThrowIfNecessary();
		return innerResult;
	}
	
	protected void ThrowIfNecessary() throws Exception{
		if(innerEx!=null)
			throw new Exception("There is an exception occoured.");
	}
}
