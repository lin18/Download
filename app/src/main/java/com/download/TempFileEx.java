package com.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 */
public class TempFileEx {	
	private static final String TEMP_FILE_EXTENTION=".tmp~";
	
	private String fileName;
	private HeaderTempFile header;
	/**
	 * 初始化新实例。
	 * @throws Exception 文件系统操作引发的异常。
	 */
	public TempFileEx(String filepath) throws Exception{
		this.fileName=filepath+TEMP_FILE_EXTENTION;
		this.header=new HeaderTempFile(filepath);
		this.header.Sync();
	}

	/****************************************************************************************************/
	/*                                      public functions.                                           */
	/****************************************************************************************************/
	
	public void Write(byte[] data,int length){
		if(!CreateIfNecessary())return;
		if(length<1)return;
		RandomAccessFile raf;
		try {
			raf=new RandomAccessFile(this.fileName,"rw");
			raf.seek(this.header.CurrentLength);
			raf.write(data,0,length);
			this.header.CurrentLength+=length;
			this.header.Sync();
			if(this.header.Length==this.header.CurrentLength){//下载完成了。
				MoveToTarget(raf);//转储。
				raf.close();
				(new File(this.fileName)).delete();
				this.header.Delete();
				return;
			}
			raf.close();
		} catch (Exception e) {
			//do nothing.
			//String a=e.getMessage();
		}
	}
	/**
	 * 获取临时文件头信息。
	 */
	public HeaderTempFile GetHeader(){
		return this.header;
	}
	/**
	 * 获取下载内容范围的开始值。
	 */
	public long GetRangeHeaderValue(){
		return this.header.CurrentLength;
	}
	/**
	 * 获取临时文件全路径名称。
	 */
	public String GetTempFilePath(){
		return this.fileName;
	}
	/**
	 * 判断临时文件是否存在。一般来说存在表示要续传。
	 * @return 返回true表示文件已存在，否则返回false，表示文件不存在。
	 */
	public boolean Exists(){
		File f=new File(this.fileName);
		return f.exists();
	}
	/**
	 * 删除临时文件。
	 */
	public void Delete(){
		File f=new File(this.fileName);
		f.delete();
	}
	/**
	 * 同步文件系统状态。考虑一种异常情况：当文件下载完成后没来得及转储，下次下载开始前应检测出该状况且调用该方法，执行转储而非下载。
	 */
	public void Sync() throws Exception{
		RandomAccessFile raf=new RandomAccessFile(this.fileName,"rw");
		//UpdateHeader(raf);//有时没删除临时文件，如果下次再读时会保留长度冲突情况，所以，这里再同步一次头信息。
		MoveToTarget(raf);//转储。
		raf.close();
		(new File(this.fileName)).delete();
		this.header.Delete();
	}
	
	/****************************************************************************************************/
	/*                                         helper routines.                                         */
	/****************************************************************************************************/
	
	/**
	 * 根据需要创建临时文件。
	 * @return 返回true表示保证文件已存在，否则返回false，表示文件不存在。
	 */
	private boolean CreateIfNecessary(){
		if(Exists())return true;
		try {
			File f=new File(this.fileName);
			f.createNewFile();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	/**
	 * 文件转储。
	 */
	private void MoveToTarget(RandomAccessFile file) throws Exception{
		File f=new File(this.header.TargetUri);
		if(f.exists())return;
		f.createNewFile();
		FileOutputStream fos=new FileOutputStream(f);
		file.seek(0);
		byte[] buffer=new byte[4096];
		int len=0;
		while(0<(len=file.read(buffer, 0, 4096))){
			fos.write(buffer,0,len);
		}
		fos.flush();
		fos.close();
		//参数file不能关闭，不归这个方法管理。
	}
}
