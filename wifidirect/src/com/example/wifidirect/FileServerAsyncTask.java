package com.example.wifidirect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * A simple server socket that accepts connection and writes some data on the
 * stream.
 */
public class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

	private Context context;
	private TextView statusText;
	public Util utilObject;
	
	private HashMap<String, byte[]> clientNamesAndFileContent = new HashMap<String, byte[]>();
	
	/**
	 * @param context
	 * @param statusText
	 */
	public FileServerAsyncTask(Context context, View statusText) {
		this.context = context;
		this.statusText = (TextView) statusText;
	}

	@SuppressWarnings("resource")
	@Override
	protected String doInBackground(Void... params) {
		ServerSocket serverSocket = null;
		Socket client = null;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		FileOutputStream outputStream = null;
		try {
			
			utilObject = new Util();// Create util object to set URL and content length
			if(utilObject == null) {
				Log.d(WiFiDirectActivity.TAG, "Util object is null.");
			}
			serverSocket = new ServerSocket(8988);
			Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
			client = serverSocket.accept();
			Log.d(WiFiDirectActivity.TAG, "Server: connection done");
			
			//Step 2 - Receive url request text
			oos = new ObjectOutputStream(client.getOutputStream());
			ois = new ObjectInputStream(client.getInputStream());
			String clientName = (String) ois.readObject();
			Log.d(WiFiDirectActivity.TAG, "Request received from client :" + clientName);
						
			oos.writeObject(utilObject.getURL());
			oos.flush();
			
		//	PeerList peerList = new PeerList();
		//	Log.d(WiFiDirectActivity.TAG, "Number of Peers discovered: " + PeerList.getPeerListSize());
			
			//Set the range for all the peers including Server
			utilObject.setRangeForDevices();
			// Download the server video file
			File serverPart = getServerPart(utilObject);
			// get the file content into byte array
			byte[] serverPartContent = Util.readFile(serverPart);
			//Add server part content into map
			clientNamesAndFileContent.put("D4", serverPartContent);
			
			oos.writeObject(utilObject.rangeMap);
			oos.flush();			
			Thread.sleep(1000);
			
			byte[] clientFileContent = (byte[]) ois.readObject();
			File clientFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) 
					 				   + "/" + clientName + "part");
			if(!clientFile.exists()) {
				clientFile.createNewFile();
				clientFile.canWrite();
			}		
			// Write content received from client into file
			outputStream = new FileOutputStream(clientFile);
			outputStream.write(clientFileContent);			
			Log.d(WiFiDirectActivity.TAG, "Received file size :"+ clientFile.getName()+ "-" 
				   + clientFile.length()+ "  copied file size: " + clientFile.length());
	
			// Add file content in map with client name
			clientNamesAndFileContent.put(clientName, clientFileContent);
			// Get the sorted map values
			Map<String, byte[]> sortedMap = getSortedMap(clientNamesAndFileContent);
			// Send map of client names and content to client
			oos.writeObject(sortedMap);
			oos.flush();
			
			// Get the merged File
			File mergedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/mergedFile.mp4");
			Log.d(WiFiDirectActivity.TAG, "File path :" + mergedFile.getAbsolutePath());
			byte[] mergedByteArray = Util.mergeContents(clientNamesAndFileContent);
			Log.d(WiFiDirectActivity.TAG, "Merged byte array size at Server :" + mergedByteArray.length);
			outputStream = new FileOutputStream(mergedFile);
			outputStream.write(mergedByteArray);
			Log.d(WiFiDirectActivity.TAG, "Resultant file size at server :" + mergedFile.length());
			
			return null;
		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
			return null;
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				serverSocket.close();
				oos.close();
				ois.close();
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	private Map<String, byte[]> getSortedMap(HashMap<String, byte[]> filesMap) {
		// TODO Auto-generated method stub
		File[] files = new File[10]; // Max device number 10
		int i = 0;
		//Sort map to merge file
		Map<String, byte[]> treeMap = new TreeMap<String, byte[]>(filesMap);
		
		/*
		for (String str : treeMap.keySet()) {
			char[] array = str.toCharArray();
			int value = Character.getNumericValue(array[1]);
		    Log.d(WiFiDirectActivity.TAG, str + " " + value) ;
		    files[value] = treeMap.get(str);
		    i++;
		}
		*/
		
		for(String str : treeMap.keySet()) {
			Log.d(WiFiDirectActivity.TAG, str + " " + str.charAt(1));
		}
		
		return treeMap;
	}

	/**
	 * Method downloads the server's video part and adds to the map
	 * @param utilObject2 - object which has range parameters
	 * @return File object - contains the video part downloaded by Server
	 */
	private File getServerPart(Util utilObject) {
		HashMap<String, String> rangeMap = utilObject.getRangeMap();
		String serverRange = (String) rangeMap.get("D4");
		Log.d(WiFiDirectActivity.TAG, serverRange);
		File serverPart = utilObject.downloadVideo(serverRange, utilObject.getURL(), "D4");
		Log.d(WiFiDirectActivity.TAG, "Downloaded file size at Server :" + serverPart.length());
		return serverPart;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(String result) {
		if (result != null) {
			statusText.setText("File copied - " + result);
			Intent intent = new Intent();
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
		statusText.setText("Opening a server socket");
	}

	

}
