package com.download;

import java.util.EventListener;


/**
 *
 */
public interface IDownloadEventListener extends EventListener {
	void OnDownloadCompleted(DownloadEventArgs args);
	void OnProgressChanged(ProgressChangedEventArgs args);
}
