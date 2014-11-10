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
			String strRequest = (String) ois.readObject();
			Log.d(WiFiDirectActivity.TAG, "Request received from client :" + strRequest);
						
			oos.writeObject(utilObject.getURL());
			oos.flush();
			
			utilObject.setRangeForDevices();
			oos.writeObject(utilObject.rangeMap);
			oos.flush();
			
			Thread.sleep(1000);
			
		//	File clientPart = (File) ois.readObject();
			File part2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    + "/newVideo.mp4");
			InputStream inputStream = client.getInputStream();
			OutputStream outputStream = new FileOutputStream(part2);
			DeviceDetailFragment.copyFile(inputStream, outputStream);
			Log.d(WiFiDirectActivity.TAG, "Received file size :" + part2.length());
			
	//		File serverPart = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
	//		                       + "/newVideo.mp4");
	//		Log.d(WiFiDirectActivity.TAG, "File path :" + serverPart.getAbsolutePath());
	//		oos.writeObject(serverPart);
	//		oos.flush();
			
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
