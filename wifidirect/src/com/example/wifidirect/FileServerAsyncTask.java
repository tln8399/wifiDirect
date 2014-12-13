package com.example.wifidirect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
	private ArrayList<InetAddress> clients = new ArrayList<InetAddress>(); 
	private static HashMap<String, byte[]> clientNamesAndFileContent = new HashMap<String, byte[]>();
	private static HashMap<String, Boolean> isDataReceivedFromClient = new HashMap<String, Boolean>();
	private static HashMap<String, Device> peersInfo = new HashMap<String, Device>();
	private static String serverName = null;
	/**
	 * @param context
	 * @param statusText
	 */
	public FileServerAsyncTask(Context context, View statusText) {
		this.context = context;
		this.statusText = (TextView) statusText;
	}

	@Override
	protected String doInBackground(Void... params) {
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		
		try {	
			
			// Create util object to set URL and content length
			utilObject = new Util();
			if(utilObject == null) {
				Log.d(WiFiDirectActivity.TAG, "Util object is null.");
			}
			
			// Wait till clients gets started
			Thread.sleep(10000);
			
			PeerList peerList = new PeerList(); // Constructor sets the static peer list
			Log.d(WiFiDirectActivity.TAG, "Number of Peers discovered: " + PeerList.getPeerListSize());
			int numberOfDevices = PeerList.getPeerListSize();
			
			//Add all peers to map with unvisited status
			for(WifiP2pDevice device : PeerList.getPeerList()) {
				isDataReceivedFromClient.put(device.deviceName, false);
				Log.d(WiFiDirectActivity.TAG, device.deviceName + " " + isDataReceivedFromClient.get(device.deviceName));
			}
			//Set the range for all the peers including Server
			utilObject.setRangeForDevices(numberOfDevices);			
			serverSocket = new ServerSocket(8988);
			Log.d(WiFiDirectActivity.TAG, "Server: Socket opened..");
			
			while(!(checkAllClientsVisited(isDataReceivedFromClient))) {
				
				// Connect to client
				Log.d(WiFiDirectActivity.TAG, "Waiting for client to connect..");
				clientSocket = serverSocket.accept();
				Log.d(WiFiDirectActivity.TAG, "Server: connection done");
				Log.d(WiFiDirectActivity.TAG, "Client Address :" + clientSocket.getInetAddress() + " Port :" + clientSocket.getPort());
				clients.add(clientSocket.getInetAddress());				
								
				//Step 2 - Receive url request text
				oos = new ObjectOutputStream(clientSocket.getOutputStream());
				ois = new ObjectInputStream(clientSocket.getInputStream());
				Device client = (Device) ois.readObject();
				Log.d(WiFiDirectActivity.TAG, "Request received from client :" + client.getName() +
											  " Ip :" + client.getIpAddress() +
											  " Port :" + client.getPort());
				peersInfo.put(client.getName(), client);			
				oos.writeObject(utilObject.getURL());
				oos.flush();
				
				oos.writeObject(utilObject.rangeMap);
				oos.flush();			
				//Thread.sleep(1000);				
				byte[] clientFileContent = (byte[]) ois.readObject();
				Log.d(WiFiDirectActivity.TAG, "Client " + client.getName() + " part size :" + clientFileContent.length);
				// Add file content in map with client name
				clientNamesAndFileContent.put(client.getName(), clientFileContent);
				isDataReceivedFromClient.put(client.getName(), true);
				oos.close();
				ois.close();
				clientSocket.close();
			}
			
			long startTimeServer = System.currentTimeMillis();
			
			// Download the server video file
			byte[] serverContent = getServerPart(utilObject);
			// get the file content into byte array
			//byte[] serverPartContent = Util.readFile(serverPart);
			//Add server part content into map
			clientNamesAndFileContent.put(serverName, serverContent);			
			sendResultMapToAllPeers(clientNamesAndFileContent, peersInfo);										
			// Get the merged File
			long fileSizeAtServer = mergeMapContentsAtServer(clientNamesAndFileContent);
						
			long endTimeServer = System.currentTimeMillis();
			Log.d(WiFiDirectActivity.TAG, " Total time at server : " + ((endTimeServer - startTimeServer) / 1000) + " Seconds");			
			
			String result = "File Size: Downloaded-"+ serverContent.length+ " Merged-"+fileSizeAtServer;
			return result; // Converting to String to display on screen
			
		} catch (IOException e) {
			Log.d(WiFiDirectActivity.TAG, e.getMessage());
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				serverSocket.close();
				oos.close();
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Method get sorted map as intput and merges all parts into one file
	 * @param sortedMap
	 * @return 
	 * @throws IOException 
	 */
	private long mergeMapContentsAtServer(HashMap<String, byte[]> contentMap) throws IOException {
		
		File mergedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) 
								   + "/mergedFile.mp4");
		
		// Get the sorted map values
		Map<String, byte[]> sortedMap = Util.getSortedMap(contentMap);						
		
		byte[] mergedByteArray = Util.mergeContents(sortedMap);
		Log.d(WiFiDirectActivity.TAG, "Merged byte array size at Server :" + mergedByteArray.length);
		@SuppressWarnings("resource")
		FileOutputStream outputStream = new FileOutputStream(mergedFile);
		outputStream.write(mergedByteArray);
		Log.d(WiFiDirectActivity.TAG, "Resultant file size at server :" + mergedFile.length());
		return mergedByteArray.length;
	}

	/**
	 * Method that connects with all the peers one at a time and
	 * sends map that contains the video part downloaded by all other
	 * peers.	
	 * @param contentMap map that contains peer and its content 
	 * @param peersInfo2 map contains the peer info i.e. name, Ip and port 
	 */
	private void sendResultMapToAllPeers(HashMap<String, byte[]> contentMap,
										 HashMap<String, Device> peersInfo) {
		
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		for(String peer : peersInfo.keySet()) {
			// Put the map data in new map
			HashMap<String, byte[]> newMap = new HashMap<String, byte[]>();
			newMap.putAll(contentMap);
			// remove the entry of peer to whom map is to be sent
			newMap.remove(peer);
			Log.d(WiFiDirectActivity.TAG, "Soreted map size:"+ contentMap.size() +"  new Map size:" + newMap.size());
			Socket newClientSocket = new Socket();
			try {
				newClientSocket.connect(new InetSocketAddress(peersInfo.get(peer).getIpAddress(), 5000));
				oos = new ObjectOutputStream(newClientSocket.getOutputStream());
				ois = new ObjectInputStream(newClientSocket.getInputStream());
				// Send map of client names and content to client
				oos.writeObject(newMap);
				oos.flush();
				newClientSocket.close();
				oos.close();
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			newMap.clear();
		}
	}

	/**
	 * Method to check all peers have been visited or not.
	 * @param isDataReceivedFromClient
	 * @return 
	 */
	private Boolean checkAllClientsVisited(
			HashMap<String, Boolean> isDataReceivedFromClient) {
		
		for(String clientName : isDataReceivedFromClient.keySet()) {
			if(isDataReceivedFromClient.get(clientName) == false) {
				return false;
			}
		}
		return true;
	}

	

	/**
	 * Method downloads the server's video part and adds to the map
	 * @param utilObject2 - object which has range parameters
	 * @return File object - contains the video part downloaded by Server
	 */
	private byte[] getServerPart(Util utilObject) {
		HashMap<String, String> rangeMap = utilObject.getRangeMap();
		String[] deviceNames = {"D1", "D2", "D3", "D4", "D5"};
		
		for(int i = 0; i < (isDataReceivedFromClient.size()+1); i++) {
			if(!isDataReceivedFromClient.containsKey(deviceNames[i])) {
				serverName = deviceNames[i]; // static variable
			}
		}
		String serverRange = (String) rangeMap.get(serverName);
		Log.d(WiFiDirectActivity.TAG, "Server part range :" + serverRange);		
		byte[] serverContent = utilObject.downloadVideo(serverRange, utilObject.getURL(), serverName);
		Log.d(WiFiDirectActivity.TAG, "Downloaded file part size at Server :" + serverContent.length);
		
		return serverContent;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(String result) {
		if (result != null) {
			
			statusText.setText(result);
		}

	}

	

	

}
