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
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
	
	private static Boolean isFileSentToServer = false;
	
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
				Log.d(WiFiDirectActivity.TAG, "Opening client socket - " + device_name);
				//socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)));
				Log.d(WiFiDirectActivity.TAG, "Local address " + socket.getLocalAddress() + "  Port :" + socket.getLocalPort());
				Device device = new Device(device_name, 
										   socket.getLocalAddress(), 
										   socket.getLocalPort());
				Log.d(WiFiDirectActivity.TAG,"Client socket - " + socket.isConnected());
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				
			//	if(!isFileSentToServer){
					//Step 1 - Send request to Server
					oos.writeObject(device);
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
					String d2Range = (String) rangeMap.get(device_name);
					Log.d(WiFiDirectActivity.TAG, d2Range);
									
					File toBeSend = utilObj.downloadVideo(d2Range, utilObjectUrl, device_name);
					Log.d(WiFiDirectActivity.TAG, "Downloaded file :" + toBeSend.length() + toBeSend.getName());
					Thread.sleep(1000);
					// call method to get content of file into byte[] array  
					byte[] content = Util.readFile(toBeSend);
					oos.writeObject(content);
					oos.flush();		
					isFileSentToServer = true;
					oos.close();
					ois.close();
					socket.close();
			//	}
		
				/*Open new socket to get the list of parts from server */
				ServerSocket inputSocket = new ServerSocket(5000);
				Socket tempSocket = inputSocket.accept();
				ois = new ObjectInputStream(tempSocket.getInputStream());
				oos = new ObjectOutputStream(tempSocket.getOutputStream());
				@SuppressWarnings("unchecked")
				Map<String, byte[]> clientNamesAndFileContent = (Map<String, byte[]>) ois.readObject();
				Log.d(WiFiDirectActivity.TAG, "Received map size :"+ clientNamesAndFileContent.size());
				
				for(String str : clientNamesAndFileContent.keySet()) {
					Log.d(WiFiDirectActivity.TAG, str + " " + str.charAt(1));
				}
				
				// Get the merged File
				File mergedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/mergedFile.mp4");
				byte[] mergedByteArray = Util.mergeContents(clientNamesAndFileContent);
				Log.d(WiFiDirectActivity.TAG, "Merged byte array size at client :" + mergedByteArray.length);
				OutputStream outputStream = new FileOutputStream(mergedFile);
				outputStream.write(mergedByteArray);
				Log.d(WiFiDirectActivity.TAG, "Resultant file size at client :" + mergedFile.length());
				
			} catch (Exception e) {
				Log.e(WiFiDirectActivity.TAG, " " + e.getMessage());
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

	/**
	 * Method saves all files in map into memory
	 * by assigning it to new files in memory.
	 * Because merge needs files to be in memory
	 * @param clientNamesAndFiles
	 * @throws IOException 
	 */
	private void saveFilesFromMap(HashMap<String, File> clientNamesAndFiles) throws IOException {
		
		InputStream inputStream = null;
		OutputStream outputStream = null;
		
		for(String key : clientNamesAndFiles.keySet()) {
			
			File fileFromMap = clientNamesAndFiles.get(key);
			String fileName = fileFromMap.getName();
			Log.d(WiFiDirectActivity.TAG, "/" + fileName);
			File newFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +"/"+
									"file"+key);
			if(!newFile.exists()){
				newFile.createNewFile();
				newFile.canWrite();
			}
			try {
				inputStream = new FileInputStream(fileFromMap);
				outputStream = new FileOutputStream(newFile);
				DeviceDetailFragment.copyFile(inputStream, outputStream);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(WiFiDirectActivity.TAG, " Map file size " + fileFromMap.length());
			Log.d(WiFiDirectActivity.TAG, " New file size " + newFile.length());
		}		
		
		try {
			inputStream.close();
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
}
