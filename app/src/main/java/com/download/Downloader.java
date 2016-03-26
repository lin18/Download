package com.download;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import android.app.Activity;
import android.os.Environment;

/**
 *
 */
public class Downloader implements IDownloadAsyncTaskCallback {
	/****************************************************************************************************/
	/*                                          constants.                                              */
	/****************************************************************************************************/
	
	protected static final String TEMP_DIR_NAME="download";
	//protected static final String SYSTEM_TEMP_DIR_PATH="/data/local/"+TEMP_DIR_NAME;
	protected static final String PRIVATE_PATH_PREFIX="/Android/data";//"/data/data";
	protected static final String AGENT_NAME="Android.Downloader";
	//protected static final String TEMP_FILE_EXTENTION=".tmp~";
	
	/****************************************************************************************************/
	/*                                      static functions.                                           */
	/****************************************************************************************************/
	
	/*
	 * 清空默认缓存。
	 */
	public static final void clearSystemCache(){
		clearCache(getSystemCachePath());
	}
	/*
	 * 如果默认缓冲区不存在则创建之。
	 */
	public static final String createSystemCacheIfNecessary(){
		return CreateCacheIfNecessary(getSystemCachePath());
	}
	/*
	 * 清空指定缓冲区。
	 */
	public static final void clearCache(String dir){
		File rootDir=new File(dir);
		if(rootDir.exists()&&rootDir.isDirectory()){
			File[] files=rootDir.listFiles();
			for(File f:files){
				f.delete();
			}
		}
	}
	/*
	 * 获取默认缓存路径。
	 */
	public static final String getSystemCachePath(){
		return Environment.getDownloadCacheDirectory().getPath()+"/"+TEMP_DIR_NAME;
	}
	/*
	 * 如果指定缓冲区不存在则创建之。
	 */
	public static final String CreateCacheIfNecessary(String dir){
		File rootDir=new File(dir);
		if(!rootDir.exists()){
			rootDir.mkdir();
		}
		return rootDir.getAbsolutePath();
	}
	/**
	 * 获取uri内容长度。
	 */
	public static long GetURIContentLength(String uri) throws Exception{
		long nFileLength = -1; 
		String sHeader;
		URL url = new URL(uri); 
		HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
		httpConnection.setRequestProperty("User-Agent",AGENT_NAME); 
		//httpConnection.setRequestProperty("RANGE","bytes=0-10");
		try{
			int responseCode=httpConnection.getResponseCode();
			if(responseCode>=400) 
			{ 
				return -2; //-2 represent access is error 
			}
			for(int i=1;;i++){ 
				sHeader=httpConnection.getHeaderFieldKey(i);
				if(sHeader!=null){ 
					if(sHeader.equals("Content-Length")){ 
						nFileLength = Long.parseLong(httpConnection.getHeaderField(sHeader));
						break;
					} 
				}else{
					break;
				}
			}
			httpConnection.disconnect();
		}
		catch(Exception ex){
			if(httpConnection!=null)httpConnection.disconnect();
			throw ex;
		}
		if(nFileLength==-1)
			nFileLength=4096*2;//default fixed.
		return nFileLength;
	}

	/**
	 * 获取uri内容长度。
	 */
	@SuppressWarnings("unused")
	public static long GetURIContentLengthOpen(HttpURLConnection httpConnection) throws Exception{
		long nFileLength = -1; 
		String sHeader;
		try{
			int responseCode=httpConnection.getResponseCode();
			if(responseCode>=400) 
			{
				return -2; //-2 represent access is error 
			}
			for(int i=1;;i++){ 
				sHeader=httpConnection.getHeaderFieldKey(i);
				if(sHeader!=null){ 
					if(sHeader.equals("Content-Length")){ 
						nFileLength = Long.parseLong(httpConnection.getHeaderField(sHeader));
						break;
					} 
				}else{
					break;
				}
			}
		}
		catch(Exception ex){
			if(ex==null)throw new Exception("Unknown");
			throw ex;
		}
		if(nFileLength==-1)
			nFileLength=4096*2;//default fixed.
		return nFileLength;
	}
	/****************************************************************************************************/
	/*                                 fields and constructors.                                         */
	/****************************************************************************************************/
	
	private Vector<IDownloadEventListener> downloadEventListeners=new Vector<IDownloadEventListener>();
	private String innerTempDir;
	private Activity innerActivity;
	private Hashtable<Object,DownloadAsyncTask> innerDownloadAsyncTasks;
	public Downloader(Activity activity,String tempDir,boolean useSystemTempCache) throws IOException{
		this.innerTempDir=tempDir;
		this.innerActivity=activity;
		this.innerDownloadAsyncTasks=new Hashtable<Object,DownloadAsyncTask>();
		
		if(tempDir==null||tempDir=="")
		{
			if(!useSystemTempCache){//不使用系统缓存区，使用应用程序的缓存区。
				this.innerTempDir=BuildPrivateCachePath();
			}else{//使用系统缓存区。
				this.innerTempDir=getSystemCachePath(); //SYSTEM_TEMP_DIR_PATH;
			}
		}
	}
	
	/****************************************************************************************************/
	/*                                      public functions.                                           */
	/****************************************************************************************************/
	
	public void DownloadAsync(String sourceUri,String targetUri,Object userState) throws Exception{
		if(userState==null)throw new Exception("userState cannot be null.");
		synchronized (this.innerDownloadAsyncTasks) {
			if(this.innerDownloadAsyncTasks!=null&&this.innerDownloadAsyncTasks.size()>0&&this.innerDownloadAsyncTasks.contains(userState))
				return;
			DownloadAsyncTask downloadAsyncTask=new DownloadAsyncTask(this);
			DownloadAsyncTaskArgs args=new DownloadAsyncTaskArgs();
			args.RemoteUri=sourceUri;
			args.LocalUri=targetUri;
			args.UserState=userState;
			args.BPM=new BPManager(this.innerTempDir, sourceUri,targetUri);
			this.innerDownloadAsyncTasks.put(userState, downloadAsyncTask);
			downloadAsyncTask.execute(args);
		}
	}
	public void CancelDownload(Object userState){
		synchronized (this.innerDownloadAsyncTasks) {
			DownloadAsyncTask downloadAsyncTask=this.innerDownloadAsyncTasks.get(userState);
			if(downloadAsyncTask!=null){
				downloadAsyncTask.cancel(false);
				downloadAsyncTask.setCancelled(true);
				this.innerDownloadAsyncTasks.remove(userState);
			}
		}
	}
	public String getCurrentCachePath(){return this.innerTempDir;}
	public void CreatePrivateCacheIfNecessary() throws IOException{
		Downloader.CreateCacheIfNecessary(BuildPrivateCachePath());
	}
	
	/****************************************************************************************************/
	/*                                      Download Event.                                             */
	/****************************************************************************************************/
	
	public void AddDownloadEventListener(IDownloadEventListener listener)
	{
		if(listener==null)return;
		this.downloadEventListeners.addElement(listener);
	}
	public void RemoveDownloadEventListener(IDownloadEventListener listener)
	{
		if(listener==null)return;
		this.downloadEventListeners.removeElement(listener);
	}
	private void PostDownloadCompleted(DownloadEventArgs e)
	{
		Enumeration<IDownloadEventListener> eles=this.downloadEventListeners.elements();
		while(eles.hasMoreElements()){
			eles.nextElement().OnDownloadCompleted(e);
		}
		this.innerDownloadAsyncTasks.remove(e.GetUserState());
	}
	private void PostProgressChanged(ProgressChangedEventArgs e)
	{
		Enumeration<IDownloadEventListener> eles=this.downloadEventListeners.elements();
		while(eles.hasMoreElements()){
			eles.nextElement().OnProgressChanged(e);
		}
	}
	
	/****************************************************************************************************/
	/*                                      callback functions.                                         */
	/****************************************************************************************************/
	
	@Override
	public void PostDownloadAsync(DownloadEventArgs e) {
		PostDownloadCompleted(e);
	}
	@Override
	public void PostProgressChangedAsync(ProgressChangedEventArgs e) {
		PostProgressChanged(e);
	}
	@Override
	public void PostRequest(DownloadAsyncTaskArgs e) throws Exception{
		//URI uri=new URI(e.RemoteUri);
		//HttpURLConnection conn= (HttpURLConnection)uri.toURL().openConnection();
		//conn.setRequestProperty("User-Agent","Android.Downloader");
		//conn.setRequestProperty("RANGE","bytes=2000070"); 
	}
	
	/****************************************************************************************************/
	/*                                         helper routines.                                         */
	/****************************************************************************************************/
	
	private String BuildPrivateCachePath() throws IOException{
		//String result= PRIVATE_PATH_PREFIX+"/"+this.innerActivity.getPackageName()+"/files";///"+TEMP_DIR_NAME;
		String result= this.innerActivity.getFilesDir().getPath();//+"/"+TEMP_DIR_NAME;//+"/"+this.innerActivity.getPackageName();//+"/files";///"+TEMP_DIR_NAME;
		//File f=new File(result);
		//if(!f.exists())
			//f.createNewFile();
		return result;
	}
}