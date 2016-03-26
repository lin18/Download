package com.download;

/**
 * 每次下载只应初始化一个该类型的对象。
 */
public class DownloadDebugger {
	private TempFileEx innerTempFile;
	private HeaderTempFile innerHeader;
	private boolean isConflict;
	public DownloadDebugger(TempFileEx tempFile){
		this.innerTempFile=tempFile;
		this.innerHeader=this.innerTempFile.GetHeader();
		isConflict=false;
	}
	/**
	 * 每次从服务器获取数据均需要调用该方法做处理和修正。
	 * <p>为什么需要该方法？考虑以下情况：</p>
	 * <p>1.响应头给定的长度与真实内容的长度不一致。</p>
	 * @param realRec :本次实际接收到的数据的长度。
	 * @param bufLen ：缓冲区的长度。
	 */
	public void FixBugEveryFetching(int realRec,int bufLen){
		//if(realRec>=bufLen){this.isConflict=false; return;}//注意取消冲突。
		//只考虑buffer不满时的情况。
		long nextLen=this.innerHeader.CurrentLength+realRec;
		if(this.innerHeader.Length==nextLen){this.isConflict=false; return;}//注意取消冲突。
		if(this.innerHeader.Length>nextLen){
			//缓冲区没满，且头长度大于真实长度，有2种情况：
			//1.头长度和真实长度不一致，且已下载完。
			//2.头长度和真实长度不一致，且未下载完。
			this.isConflict=true;
			return;
		}else if(this.innerHeader.Length<nextLen){
			//缓冲区没满，且头长度小于真实长度，以真实长度为准，且不能判断是
			//否下载完成，只有等到跳出下载循环时才能确定下载完成。
			this.innerHeader.Length=(long)(nextLen*1.2);//+realRec;//这样就会标示为没下载完。
			this.isConflict=true;
			return;
		}
		this.isConflict=false;
	}
	/**
	 * 下载完成后调用。
	 * @return 如果发生长度冲突（会自动修正）返回true，否则返回false。
	 * @throws Exception :同步文件时可能会发生IO异常。
	 */
	public boolean FixBugAfterCompleting() throws Exception{
		boolean result=false;
		if(isConflict){
			//虽已发生冲突，但总算是下载完成了。
			this.innerHeader.Length=this.innerHeader.CurrentLength;
			this.innerTempFile.Sync();
			result=true;
		}
		isConflict=false;
		return result;
	}
}
