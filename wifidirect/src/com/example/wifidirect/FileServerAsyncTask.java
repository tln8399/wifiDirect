package com.example.wifidirect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
	private ArrayList<InetAddress> clients = new ArrayList<InetAddress>(); 
	private static HashMap<String, byte[]> clientNamesAndFileContent = new HashMap<String, byte[]>();
	private static HashMap<String, Boolean> isDataReceivedFromClient = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> isDataSentToClient = new HashMap<String, Boolean>();
	private static HashMap<String, Device> peersInfo = new HashMap<String, Device>();
	private static Boolean areAllClientsVisited = false;
	private static Boolean areAllClientsReceivedData = false;
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
		Socket clientSocket = null;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		FileOutputStream outputStream = null;
		
		try {			
			
			utilObject = new Util();// Create util object to set URL and content length
			if(utilObject == null) {
				Log.d(WiFiDirectActivity.TAG, "Util object is null.");
			}
			
			PeerList peerList = new PeerList();
			Log.d(WiFiDirectActivity.TAG, "Number of Peers discovered: " + PeerList.getPeerListSize());
			int numberOfDevices = PeerList.getPeerListSize();
			//Set the range for all the peers including Server
			utilObject.setRangeForDevices(numberOfDevices);
			//Add all peers to map with unvisited status
			for(WifiP2pDevice device : PeerList.getPeerList()) {
				isDataReceivedFromClient.put(device.deviceName, false);
				Log.d(WiFiDirectActivity.TAG, device.deviceName + " " + isDataReceivedFromClient.get(device.deviceName));
			}
			areAllClientsVisited = checkAllClientsVisited(isDataReceivedFromClient);			
			serverSocket = new ServerSocket(8988);
			Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
			
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
				Thread.sleep(1000);
				
				byte[] clientFileContent = (byte[]) ois.readObject();
				File clientFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) 
						 				   + "/" + client.getName() + "part");
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
				clientNamesAndFileContent.put(client.getName(), clientFileContent);
				isDataReceivedFromClient.put(client.getName(), true);
				oos.close();
				ois.close();
				clientSocket.close();
			//	serverSocket.close();
			}
			
			// Download the server video file
			File serverPart = getServerPart(utilObject);
			// get the file content into byte array
			byte[] serverPartContent = Util.readFile(serverPart);
			//Add server part content into map
			clientNamesAndFileContent.put("D2", serverPartContent);			
			// Get the sorted map values
			Map<String, byte[]> sortedMap = getSortedMap(clientNamesAndFileContent);
			
			sendResultMapToAllPeers(sortedMap, peersInfo);				
			
			// Get the merged File
			File mergedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/mergedFile.mp4");
			Log.d(WiFiDirectActivity.TAG, "File path :" + mergedFile.getAbsolutePath());
			byte[] mergedByteArray = Util.mergeContents(sortedMap);
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
			//	oos.close();
			//	ois.close();
			//	outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	

	/**
	 * Method that connects with all the peers one at a time and
	 * sends map that contains the video part downloaded by all other
	 * peers.	
	 * @param sortedMap map that contains peer and its content 
	 * @param peersInfo2 map contains the peer info i.e. name, Ip and port 
	 */
	private void sendResultMapToAllPeers(Map<String, byte[]> sortedMap,
										 HashMap<String, Device> peersInfo) {
		
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		for(String peer : peersInfo.keySet()) {
						
			Socket newClientSocket = new Socket();
			try {
				newClientSocket.connect(new InetSocketAddress(peersInfo.get(peer).getIpAddress(), 5000));
				oos = new ObjectOutputStream(newClientSocket.getOutputStream());
				ois = new ObjectInputStream(newClientSocket.getInputStream());
				// Send map of client names and content to client
				oos.writeObject(sortedMap);
				oos.flush();
				newClientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
	 * Method that returns the sorted map
	 * @param filesMap input HashMap
	 * @return Map object
	 */
	private Map<String, byte[]> getSortedMap(HashMap<String, byte[]> filesMap) {
		
		File[] files = new File[10]; // Max device number 10
		int i = 0;
		//Sort map to merge file
		Map<String, byte[]> treeMap = new TreeMap<String, byte[]>(filesMap);
		
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
