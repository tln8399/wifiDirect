// Copyright 2011 Google Inc. All Rights Reserved.

package com.example.wifidirect;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
	public static final String EXTRAS_FILE_PATH = "file_url";
	public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
	public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
	public static String device_name;
	
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
				
				long startTimeClient = System.currentTimeMillis();
				
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				
				//Step 1 - Send(device object) request to Server
				oos.writeObject(device);
				oos.flush();
				Log.d(WiFiDirectActivity.TAG, "Request sent");
				URL utilObjectUrl = (URL) ois.readObject();
				if(utilObjectUrl != null) {
					Log.d(WiFiDirectActivity.TAG, "URL received :" + utilObjectUrl.toString());
					Log.d(WiFiDirectActivity.TAG, "Content Length " + utilObj.getUrlContentLength(utilObjectUrl));
					Log.d(WiFiDirectActivity.TAG, "Device Name : " + device_name);
				}
				else {
					Log.d(WiFiDirectActivity.TAG, "URL object received is null.");
				}
				
				// Step 2 - get range map from server
				@SuppressWarnings("unchecked")
				HashMap<String, String> rangeMap = (HashMap<String, String>) ois.readObject();
				String d2Range = (String) rangeMap.get(device_name);
				Log.d(WiFiDirectActivity.TAG, d2Range);
				// Download video part for given range
				byte[] content = utilObj.downloadVideo(d2Range, utilObjectUrl, device_name);
				//Log.d(WiFiDirectActivity.TAG, "Downloaded file :" + toBeSend.length() + toBeSend.getName());
				//Thread.sleep(1000);
				// call readFile method to get content of file into byte[] array  
				//byte[] content = Util.readFile(toBeSend);
				oos.writeObject(content);
				oos.flush();		
				
				// Close all network connection and streams
				oos.close();
				ois.close();
				socket.close();
			
				/*Open new socket to get the list of parts from server */
				@SuppressWarnings("resource")
				ServerSocket inputSocket = new ServerSocket(5000);
				Socket tempSocket = inputSocket.accept();
				ois = new ObjectInputStream(tempSocket.getInputStream());
				oos = new ObjectOutputStream(tempSocket.getOutputStream());
				// Read the map from the server
				@SuppressWarnings("unchecked")
				HashMap<String, byte[]> clientNamesAndFileContent = (HashMap<String, byte[]>) ois.readObject();
				Log.d(WiFiDirectActivity.TAG, "Received unsorted map size :"+ clientNamesAndFileContent.size());
				// Add current client's content into received map
				clientNamesAndFileContent.put(device_name, content);
				// Get the sorted map values
				Map<String, byte[]> sortedMap = Util.getSortedMap(clientNamesAndFileContent);
				
				for(String str : sortedMap.keySet()) {
					Log.d(WiFiDirectActivity.TAG, str + " " + str.charAt(1));
				}
				
				// Get the merged File - call to local method
				mergeMapContentsAtClient(sortedMap);				
				
				long endTimeClient = System.currentTimeMillis();
				Log.d(WiFiDirectActivity.TAG, " Total time at client : " + ((endTimeClient - startTimeClient) / 1000) + " Seconds");
				
			} catch (Exception e) {
				Log.e(WiFiDirectActivity.TAG, " " + e.getMessage());
			} finally {
				try {
					socket.close();
					oos.close();
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * Method that merges the file contents from input map 
	 * @param clientNamesAndFileContent
	 * @throws IOException 
	 */
	private void mergeMapContentsAtClient(Map<String, byte[]> clientNamesAndFileContent) throws IOException {
		
		File mergedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/mergedFile.mp4");
		byte[] mergedByteArray = Util.mergeContents(clientNamesAndFileContent);
		Log.d(WiFiDirectActivity.TAG, "Merged byte array size at client :" + mergedByteArray.length);
		OutputStream outputStream = new FileOutputStream(mergedFile);
		outputStream.write(mergedByteArray);
		Log.d(WiFiDirectActivity.TAG, "Resultant file size at client :" + mergedFile.length());
	}	
	
	
}
