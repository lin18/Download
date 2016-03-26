package com.download;


/**
 *
 */
public interface IDownloadAsyncTaskCallback {
	void PostDownloadAsync(DownloadEventArgs e);
	void PostProgressChangedAsync(ProgressChangedEventArgs e);
	void PostRequest(DownloadAsyncTaskArgs e) throws Exception;
}
