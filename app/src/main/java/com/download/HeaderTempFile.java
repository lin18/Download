package com.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

import android.util.Xml.Encoding;

/**
 *
 */
public class HeaderTempFile {
	private static final int DEFAULT_HEAD_LENGTH=425;
	/**
	 * 文件头信息长度。
	 */
	public int HeaderLength=DEFAULT_HEAD_LENGTH;// size: 5 bytes
	/**
	 * 完整文件的长度。
	 */
	public long Length;// size: 10 bytes
	/**
	 * 已保存的文件内容长度。
	 */
	public long CurrentLength;// size: 10 bytes
	public String SourceUri;// size: 200 bytes
	public String TargetUri;// size: 200 bytes
	
	private static final String TEMP_FILE_EXTENTION=".lh";
	
	private String fileName;
	/**
	 * 初始化新实例。
	 */
	public HeaderTempFile(String filepath){
		this.fileName=filepath+TEMP_FILE_EXTENTION;
		ReadHeaderIfNecessary();
	}

	/****************************************************************************************************/
	/*                                      public functions.                                           */
	/****************************************************************************************************/
	/**
	 * 获取下载内容范围的开始值。
	 */
	public long GetRangeHeaderValue(){
		//ReadHeaderIfNecessary();
		return this.CurrentLength;
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
		/*RandomAccessFile raf=new RandomAccessFile(this.fileName,"rw");
		UpdateHeader(raf);//有时没删除临时文件，如果下次再读时会保留长度冲突情况，所以，这里再同步一次头信息。
		MoveToTarget(raf);//转储。
		raf.close();
		(new File(this.fileName)).delete();*/
		
		if(!CreateIfNecessary())return;
		RandomAccessFile raf=null;
		try {
			raf=new RandomAccessFile(this.fileName,"rw");
			UpdateHeader(raf);
			raf.close();
		} catch (Exception e) {
			//do nothing.
			//String a=e.getMessage();
		}
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
	 * 确保临时文件头信息准确。
	 */
	private void ReadHeaderIfNecessary(){
		FileInputStream fis=null;
		if(this.Length>0){//头部信息已经读取过，只需更新再写入。
			//do nothings.
		}else{
			try {
				fis=new FileInputStream(this.fileName);
				byte[] buffer = new byte[DEFAULT_HEAD_LENGTH];
				int count = fis.read(buffer, 0, DEFAULT_HEAD_LENGTH);
				if(count>0){
					//DeserializeHeader(EncodingUtils.getString(buffer, Encoding.UTF_8.name()));
					DeserializeHeaderByte(buffer);
				}
			} catch (Exception e) {}
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {}
			}
		}
	}
	/**
	 * 确保临时文件头信息准确。
	 */
	private void UpdateHeader(RandomAccessFile file){
		try {
			file.seek(0);
			//file.writeUTF(SerializeHeader());
			byte[] data=SerializeHeaderByte();
			file.write(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 序列化临时文件头信息。
	 * @return 字符，表示临时文件头。
	 */
	private String SerializeHeader(){
		String result="";
		result=String.format("%1$-5d", this.HeaderLength)+
				String.format("%1$-10d", this.Length)+
				String.format("%1$-10d", this.CurrentLength)+
				String.format("%1$-200s", this.SourceUri)+
				String.format("%1$-200s", this.TargetUri);
		return result;
	}
	private byte[] SerializeHeaderByte() throws UnsupportedEncodingException{
		String res=SerializeHeader();
		return res.getBytes(Encoding.US_ASCII.name());
	}
	private void DeserializeHeaderByte(byte[] headerBytes){
		DeserializeHeader(EncodingUtils.getString(headerBytes, Encoding.US_ASCII.name()));
	}
	/**
	 * 反序列化临时文件头信息。
	 * @param headerBytes :必须包含全部头部信息,建议使用前425字节。
	 * @return 从不返回。
	 */
	private void DeserializeHeader(String headerBytes){
		String hl=headerBytes.substring(0, 5);//5 bytes.
		this.HeaderLength=DEFAULT_HEAD_LENGTH;//Integer.parseInt(hl);
		hl=headerBytes.substring(5,15);
		this.Length=Long.parseLong(hl.trim());
		hl=headerBytes.substring(15,25);
		this.CurrentLength=Long.parseLong(hl.trim());
		this.SourceUri=headerBytes.substring(25,225).trim();
		this.TargetUri=headerBytes.substring(225,425).trim();
		//this.innerContentStartIndex=425;
	}
}
