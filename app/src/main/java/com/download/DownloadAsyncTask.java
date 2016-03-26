package com.download;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import android.os.AsyncTask;
/**
 *
 */
public class DownloadAsyncTask extends AsyncTask<DownloadAsyncTaskArgs, ProgressChangedEventArgs, DownloadEventArgs> {

	private IDownloadAsyncTaskCallback innerCallback=null;
	public DownloadAsyncTask(IDownloadAsyncTaskCallback callback){
		this.innerCallback=callback;
	}
	private boolean isCancelled;
	public void setCancelled(boolean val){this.isCancelled=val;}
	
	@SuppressWarnings("unused")
	@Override
	protected DownloadEventArgs doInBackground(DownloadAsyncTaskArgs... params) {
		DownloadAsyncTaskArgs arg=params[0];
		Object userState=arg.UserState;
		Exception ex=null;
		String result=arg.LocalUri;
		boolean cancelled=false;
		long startIndexIfResuming=0;
		TempFileEx tempFile=null;
		HeaderTempFile tempFileHeader=null;
		try{
			tempFile=arg.BPM.GetTempFile();
			tempFileHeader=tempFile.GetHeader();
		}catch(Exception e1){
			if(e1==null)ex= new Exception("Unknown");
			ex=e1;
		}
		HttpURLConnection conn=null;
		if(!isCancelled){
			try{
				//do works.
				URI uri=new URI(arg.RemoteUri);
				conn= (HttpURLConnection)uri.toURL().openConnection();
				conn.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 8.0;Windows NT 6.1;WOW64;Trident/4.0;SLCC2;.NET CLR 2.0.50727;.NET CLR 3.5.30729;.NET CLR 3.0.30729;.NET4.0C;.NET4.0E)");
				conn.setRequestProperty("Accept", "*/*");//"text/html,image/gif,image/jpeg,*;q=.2,*/*;q=.2");
				conn.setRequestProperty("Accept-Language", "en-US");
				conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
				
				if(arg.BPM.IsResuming()){//发生断点续传。
					startIndexIfResuming=arg.BPM.GetStartIndex();
					if(startIndexIfResuming>=tempFileHeader.Length){//已经下载完成了，但由于异常导致没转储。
						tempFile.Sync();
						return new DownloadEventArgs(result, ex, userState, cancelled);
					}else{
						conn.setRequestProperty("RANGE","bytes="+String.valueOf(startIndexIfResuming)+"-"); 
					}
				}else{
					tempFileHeader.Length= Downloader.GetURIContentLengthOpen(conn);
				}
				InputStream stream=conn.getInputStream();
				int len=0;
				byte[] buf=new byte[4096];
				DownloadDebugger debugger=new DownloadDebugger(tempFile);
				while((len=stream.read(buf, 0, 4096))>0){
					if(this.isCancelled){
						cancelled=true;
						break;
					}
					debugger.FixBugEveryFetching(len, 4096);
					tempFile.Write(buf, len);
					//post progress
					ProgressChangedEventArgs progress=new ProgressChangedEventArgs(((float)tempFileHeader.CurrentLength/(float)tempFileHeader.Length), userState);
					this.publishProgress(progress);
				}
				if(debugger.FixBugAfterCompleting()){//由于长度不一致，所以最近一次progress不是100%，这里修正。
					//post progress
					ProgressChangedEventArgs progress=new ProgressChangedEventArgs((float) 1.0, userState);
					this.publishProgress(progress);
				}
				conn.disconnect();
			}
			catch(Exception e){
				if(conn!=null)conn.disconnect();
				if(e==null)ex= new Exception("Unknown");
				ex=e;
			}
		}else{
			//task cancelled.
			cancelled=true;
		}
		DownloadEventArgs args=new DownloadEventArgs(result, ex, userState, cancelled);
		return args;
	}
	@Override
	protected void onProgressUpdate(ProgressChangedEventArgs... progress) {
        if(this.innerCallback!=null){
        	this.innerCallback.PostProgressChangedAsync(progress[0]);
        }
    }
	@Override
	protected void onPostExecute(DownloadEventArgs e){
		if(this.innerCallback!=null){
        	this.innerCallback.PostDownloadAsync(e);
        }
	}


}
