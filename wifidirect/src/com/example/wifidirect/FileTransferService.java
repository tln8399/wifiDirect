// Copyright 2011 Google Inc. All Rights Reserved.

package com.example.wifidirect;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService implements Serializable {

	private static final int SOCKET_TIMEOUT = 50000;
	public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
	public static final String EXTRAS_FILE_PATH = "file_url";
	public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
	public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
	public static String device_name;
	
	private TextView client_status;
	
	public FileTransferService(String name) {
		super(name);
	}

	public FileTransferService() {
		super("FileTransferService");
	}
	
	public static void setDevicename(String name) {
		device_name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {

		Context context = getApplicationContext();
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;

		if (intent.getAction().equals(ACTION_SEND_FILE)) {
			String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
			String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

			try {
				
				Util utilObj = new Util();
				Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
				//socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)));

				Log.d(WiFiDirectActivity.TAG,"Client socket - " + socket.isConnected());
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				
				//Step 1 - Send request to Server
				oos.writeObject("Request for url");
				oos.flush();
				Log.d(WiFiDirectActivity.TAG, "Request sent");
				URL utilObjectUrl = (URL) ois.readObject();
				if(utilObjectUrl != null) {
				Log.d(WiFiDirectActivity.TAG, "URL received :" + utilObjectUrl.toString());
				Log.d(WiFiDirectActivity.TAG, "Content Length " + utilObj.getUrlContentLength(utilObjectUrl));
				Log.d(WiFiDirectActivity.TAG, "Device Name : " + device_name);
				//Log.d(WiFiDirectActivity.TAG, "URL content length :" + utilObject.getUrlContentLength(utilObject.getURL()));
				}
				else {
					Log.d(WiFiDirectActivity.TAG, "URL object received is null.");
				}
				
				HashMap rangeMap = (HashMap) ois.readObject();
				String d2Range = (String) rangeMap.get("D2");
				Log.d(WiFiDirectActivity.TAG, d2Range);
				;
				
				File clientVideo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
					                   + "/part2.mp4");
				File toBeSend = utilObj.downloadVideo(d2Range, utilObjectUrl);
				Log.d(WiFiDirectActivity.TAG, "Downloaded file :" + toBeSend.length());
				Thread.sleep(1000);
				// Send file using copyfile method,  
				InputStream inputStream = new FileInputStream(toBeSend);
				OutputStream outputStream = socket.getOutputStream();
				DeviceDetailFragment.copyFile(inputStream, outputStream);
		//		oos.writeObject(toBeSend);
		//		oos.flush();
		//		File serverPart = (File) ois.readObject();
		//		Log.d(WiFiDirectActivity.TAG, "Received :"+ serverPart.length());
			} catch (Exception e) {
				Log.e(WiFiDirectActivity.TAG, e.getMessage());
			} finally {
				if (socket != null) {
					if (socket.isConnected()) {
						try {
							socket.close();
							oos.close();
							ois.close();
						} catch (IOException e) {
							// Give up
							e.printStackTrace();
						}
					}
				}
			}

		}
	}

}
