package com.example.wifidirect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class FileClientAsyncTask extends AsyncTask<Void, Void, String>{

	 private String EXTRAS_GROUP_OWNER_ADDRESS;
     private String EXTRAS_GROUP_OWNER_PORT;
     private TextView statusText;
     public static String group_owner_ip;
     public static String device_name;
     /**
      * @param context
      * @param statusText
      */
     public FileClientAsyncTask(String ip, String port, View statusText) {
         this.EXTRAS_GROUP_OWNER_PORT = ip;
         this.EXTRAS_GROUP_OWNER_PORT = port;
         this.statusText = (TextView) statusText;
     }
     
     public static void setGroupOwnerIP(String ip) {
    	 group_owner_ip = ip;
     }
     
     public static void setDevicename(String name) {
 		device_name = name;
 	}
     
	@Override
	protected String doInBackground(Void... params) {

		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		
		String host = group_owner_ip;
		Socket socket = new Socket();
		int port = Integer.parseInt(EXTRAS_GROUP_OWNER_PORT);
		long sizeOfFile = 0;
		String result = "";
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
			
			long startTimeClient1 = System.currentTimeMillis();
			
			// Download video part for given range
			byte[] content = utilObj.downloadVideo(d2Range, utilObjectUrl, device_name);
			
			long endTimeClient1 = System.currentTimeMillis();
			long time1 = (endTimeClient1 - startTimeClient1) / 1000;
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
			
			long startTimeClient2 = System.currentTimeMillis();
						
			// Get the merged File - call to local method
			sizeOfFile = mergeMapContentsAtClient(sortedMap);				
			
			long endTimeClient2 = System.currentTimeMillis();
			long time2 = (endTimeClient2 - startTimeClient2) / 1000;
			Log.d(WiFiDirectActivity.TAG, " Total time at client : " + (time1+time2) +" Seconds");
			
			result = "File Size: Downloaded-"+ content.length+ " Merged-"+sizeOfFile;
			
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
		
		
		
		return result;
	}
	
	/**
	 * Method that merges the file contents from input map 
	 * @param clientNamesAndFileContent
	 * @throws IOException 
	 */
	private long mergeMapContentsAtClient(Map<String, byte[]> clientNamesAndFileContent) throws IOException {
		
		File mergedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/mergedFile.mp4");
		byte[] mergedByteArray = Util.mergeContents(clientNamesAndFileContent);
		Log.d(WiFiDirectActivity.TAG, "Merged byte array size at client :" + mergedByteArray.length);
		OutputStream outputStream = new FileOutputStream(mergedFile);
		outputStream.write(mergedByteArray);
		Log.d(WiFiDirectActivity.TAG, "Resultant file size at client :" + mergedFile.length());
		
		return mergedByteArray.length;
	}	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(String result) {
		if (result != null) {
			statusText.setText(result);
			Intent intent = new Intent();
			//intent.se
			// intent.setAction(android.content.Intent.ACTION_VIEW);
			// intent.setDataAndType(Uri.parse("file://" + result), "image/*");
			// context.startActivity(intent);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	@Override
	protected void onPreExecute() {
		//statusText.setText("Opening a server socket");
	}

}
