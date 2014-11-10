package com.example.wifidirect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;

//import com.googlecode.mp4parser.authoring.Movie;




import android.os.Environment;
import android.util.Log;

public class Util implements Serializable {

    public static final int SERVER_SOCKET = 5000;

	public static URL url;
	public static long urlContentLength;
	private File outFile;
	private int start;
	private int end;
	
	public HashMap<String, String> rangeMap = new HashMap<String, String> ();

	public Util(String path, int start, int end) throws MalformedURLException {
		url = new URL("http://hindimobilevideos.com/load/Mp4%20Videos/2014%20Videos/Daawat-e-Ishq/Rangreli%20(Daawat-e-Ishq)%20_%20Bollywood%20Videos%20-%20Hindimobilevideos.com.mp4");
		this.outFile = new File(path);
		this.start = start;
		this.end = end;
	}

	public Util() throws MalformedURLException {
		url = new URL("http://hindimobilevideos.com/load/Mp4%20Videos/2014%20Videos/Daawat-e-Ishq/Rangreli%20(Daawat-e-Ishq)%20_%20Bollywood%20Videos%20-%20Hindimobilevideos.com.mp4");
		//urlContentLength = getUrlContentLength(url);
	}

	public long getUrlContentLength(URL url) {
		
		long lengthOfFile = 0;
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				lengthOfFile = connection.getContentLength();
				Log.d("MyMsgs", "Length of file :" + lengthOfFile);
				connection.disconnect();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lengthOfFile;
	}
	
	public void setRangeForDevices() {
		URL url = getURL();
		long contentLength = getUrlContentLength(url);
		
		rangeMap.put("D4", 0 + "-" + (contentLength/2));
		rangeMap.put("D2", (contentLength/2)+1 + "-" + contentLength);
	}

	public File getFile() {
		return outFile;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public long getSizeOfFile() {
		return outFile.length();
	}
	
	public URL getURL() {
		return url;
	}

	public File downloadVideo(String range, URL url) {
		
		File outFile = null;
		try {
			//URL url = new URL(
			//		"http://hindimobilevideos.com/load/Mp4%20Videos/2014%20Videos/Daawat-e-Ishq/Rangreli%20(Daawat-e-Ishq)%20_%20Bollywood%20Videos%20-%20Hindimobilevideos.com.mp4");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			int lengthOfFile = 0;
			/*
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				lengthOfFile = connection.getContentLength();
				Log.d("MyMsgs", "Length of file :" + lengthOfFile);
				connection.disconnect();

			}
			*/
			/************** Logic to save a video file *********************************************/

			int start = lengthOfFile / 2;
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");// HEAD
			connection.setRequestProperty("Range", "bytes=" + range);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			Log.d("MyMsgs", " HTTP_PARTIAL Code: "
					+ HttpURLConnection.HTTP_PARTIAL + " -  Response Code: "
					+ connection.getResponseCode());
			Log.d("MyMsgs", "Connected to URL.");

			// set the path where we want to save the file
			File SDCardRoot = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			Log.d("MyMsgs", "file path :" + SDCardRoot.getAbsolutePath());
			// create a new file, to save the downloaded file
			

			outFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
							+ "/part2.mp4");
			// If file does not exists, then create it
			if (!outFile.exists()) {
				outFile.createNewFile();
				outFile.canWrite();
			}

			final int DOWNLOAD_BUFFER_SIZE = 1024;
			FileOutputStream fileStream = new FileOutputStream(outFile.getAbsolutePath());
			BufferedOutputStream outStream = new BufferedOutputStream(fileStream, DOWNLOAD_BUFFER_SIZE);
			InputStream inStream = new BufferedInputStream(url.openStream());// ,
																				// 8192);
			byte[] buffer = new byte[1024];
			int len1 = 0;
			int countInKB = 0;
			Log.d("MyMsgs", " " + connection.getHeaderFields());
			int fileinKB = lengthOfFile / 1024;
			int devidedSize = fileinKB / 10;
			int count = 0;
			Log.d("MyMsgs",	"Get Content-Length :" + connection.getHeaderField("Content-Length"));
			int expLength = Integer.parseInt(connection.getHeaderField("Content-Length"));
			while ((len1 = inStream.read(buffer, 0, 1024)) > 0
					&& countInKB < expLength) {
				fileStream.write(buffer, 0, len1);

				countInKB += len1;
				count++;
			}
			fileStream.close();
			Log.d("MyMsgs", "Output file size :" + outFile.length());
			//return outFile;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			Log.d("MyMsgs", "Exception 2");
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("MyMsgs", "Exception 3");

		} finally {

		}
		return outFile;
	}
	
	
}
