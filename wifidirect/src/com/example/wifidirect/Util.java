package com.example.wifidirect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.client.ClientProtocolException;

//import com.googlecode.mp4parser.authoring.Movie;













import android.net.wifi.WifiConfiguration;
import android.net.wifi.p2p.WifiP2pDevice;
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
		//url = new URL("http://hindimobilevideos.com/load/Mp4%20Videos/2014%20Videos/Daawat-e-Ishq/Rangreli%20(Daawat-e-Ishq)%20_%20Bollywood%20Videos%20-%20Hindimobilevideos.com.mp4");
		url = new URL("https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4");
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
	
	public void setRangeForDevices(int numberOfDevices) {
		URL url = getURL();
		long contentLength = getUrlContentLength(url);
		
		//rangeMap.put("D4", 0 + "-" + (contentLength/2));
		//rangeMap.put("D2", ((contentLength/2)+1) +"-"+contentLength);
		
		long sizeOfEachPart = contentLength / (numberOfDevices + 1); // +1 for group owner
		for(int i = 0; i < (numberOfDevices+1); i++) {
			if(i == numberOfDevices){ 
				// In case of odd size, last device will download remaining content
				rangeMap.put("D"+(i+1) , (sizeOfEachPart * i) + "-" + (contentLength-1));
			}
			else {
				rangeMap.put("D"+(i+1) , (sizeOfEachPart * i) + "-" + ((sizeOfEachPart * (i+1))-1));
			}
		}
		
		for(String s : rangeMap.keySet()) {
			Log.d(WiFiDirectActivity.TAG, s + " : " + rangeMap.get(s));
   	 	}
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
	
	public HashMap<String, String> getRangeMap(){
		return rangeMap;
	}

	public File downloadVideo(String range, URL url, String device_name) {
		
		File outFile = null;
		try {
			//URL url = new URL(
			//		"http://hindimobilevideos.com/load/Mp4%20Videos/2014%20Videos/Daawat-e-Ishq/Rangreli%20(Daawat-e-Ishq)%20_%20Bollywood%20Videos%20-%20Hindimobilevideos.com.mp4");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			int lengthOfFile = 0;
			/************** Logic to save a video file *********************************************/

			int start = lengthOfFile / 2;
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");// HEAD
			connection.setRequestProperty("Range", "bytes=" + range);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			Log.d(WiFiDirectActivity.TAG, " HTTP_PARTIAL Code: " + HttpURLConnection.HTTP_PARTIAL + " -  Response Code: "
					+ connection.getResponseCode());
			Log.d(WiFiDirectActivity.TAG, "Connected to URL.");

			// set the path where we want to save the file
			File SDCardRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	//		Log.d("MyMsgs", "file path :" + SDCardRoot.getAbsolutePath());
			// create a new file, to save the downloaded file
			outFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
							   + "/"+ device_name +"part");
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
	//		Log.d(WiFiDirectActivity.TAG, " " + connection.getHeaderFields());
	//		Log.d(WiFiDirectActivity.TAG,	"Get Content-Length :" + connection.getHeaderField("Content-Length"));
			int expLength = Integer.parseInt(connection.getHeaderField("Content-Length"));
			while ((len1 = inStream.read(buffer, 0, 1024)) > 0
					&& countInKB < expLength) {
				fileStream.write(buffer, 0, len1);
				countInKB += len1;
			}
			fileStream.close();
			Log.d(WiFiDirectActivity.TAG, "Output file size :" + outFile.length());
			//return outFile;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			Log.d(WiFiDirectActivity.TAG, "Exception 2");
		} catch (IOException e) {
			e.printStackTrace();
			Log.d(WiFiDirectActivity.TAG, "Exception 3");
		}
		return outFile;
	}
	
	/**
	 * Method to get the file contents into byte[] array
	 */
	public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }
	
	/**
	 * Method to merge the contents of all devices
	 */
	public static byte[] mergeContents(Map<String, byte[]> contentMap) {
		
		int length = 0;
		for(String str : contentMap.keySet()) {
			Log.d(WiFiDirectActivity.TAG, "" + str + " part length = " + (contentMap.get(str)).length);
			length = length + (contentMap.get(str)).length;
		}
		Log.d(WiFiDirectActivity.TAG, "Total length should be :" + length);
		byte[] mergedByteArray = new byte[length];
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		for(String str : contentMap.keySet()) {
			try {
				outputStream.write(contentMap.get(str));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    Log.d(WiFiDirectActivity.TAG, "Merged byte array length :" + outputStream.toByteArray().length);
		return outputStream.toByteArray();
	}
	
}
