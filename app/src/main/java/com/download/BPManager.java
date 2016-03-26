package com.download;

/**
 * 表示断点管理器。
 */
public class BPManager {
	//private String innerDir;
	//private String innerUri;
	private TempFileEx innerTempFile;
	
	public BPManager(String tempDir,String remoteURI,String localPath) throws Exception{
		//this.innerDir=dir;
		//this.innerUri=uri;
		//String[] items= remoteURI.split("/");
		String[] items= localPath.split("/");
		this.innerTempFile=new TempFileEx(tempDir+"/"+items[items.length-1]);
		HeaderTempFile header=this.innerTempFile.GetHeader();
		header.SourceUri=remoteURI;
		header.TargetUri=localPath;
	}
	/**
	 * 获取临时文件。
	 */
	public TempFileEx GetTempFile(){
		return this.innerTempFile;
	}
	/**
	 * 判断是否将要执行续传动作。
	 */
	public boolean IsResuming(){
		return this.innerTempFile.Exists();
	}
	/**
	 * 获取要下载的起始位置。
	 */
	public long GetStartIndex(){
		if(!IsResuming())return 0;
		return this.innerTempFile.GetRangeHeaderValue();
	}
}
