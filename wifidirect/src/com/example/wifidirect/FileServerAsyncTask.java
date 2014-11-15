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
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
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
	
	private HashMap<String, File> clientNamesAndFiles = new HashMap<String, File>();
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
		Socket client = null;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		
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
			
			utilObject.setRangeForDevices();
			File serverPart = getServerPart(utilObject);
			clientNamesAndFiles.put("D4", serverPart);
			oos.writeObject(utilObject.rangeMap);
			oos.flush();
			
			Thread.sleep(1000);
			
			File clientPart = (File) ois.readObject();
			File clientFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    + "/clientFile.mp4");
			InputStream inputStream = new FileInputStream(clientPart);
			OutputStream outputStream = new FileOutputStream(clientFile);
			DeviceDetailFragment.copyFile(inputStream, outputStream);
			Log.d(WiFiDirectActivity.TAG, "Received file size :" + clientFile.length());
			inputStream.close();
			outputStream.close();
			//Add file in map with client name
			clientNamesAndFiles.put(clientName, clientFile);
			
	//		File serverPart = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
	//		                       + "/newVideo.mp4");
	//		Log.d(WiFiDirectActivity.TAG, "File path :" + serverPart.getAbsolutePath());
			oos.writeObject(clientNamesAndFiles);
			oos.flush();
			
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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Method downloads the server's video part and adds to the map
	 * @param utilObject2 - object which has range parameters
	 * @return
	 */
	private File getServerPart(Util utilObject2) {
		HashMap<String, String> rangeMap = utilObject2.getRangeMap();
		String serverRange = (String) rangeMap.get("D4");
		Log.d(WiFiDirectActivity.TAG, serverRange);
		File serverPart = utilObject2.downloadVideo(serverRange, utilObject2.getURL());
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
