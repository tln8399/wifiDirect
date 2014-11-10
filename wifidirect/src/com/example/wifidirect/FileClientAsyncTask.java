package com.example.wifidirect;

import java.net.Socket;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

public class FileClientAsyncTask extends AsyncTask<Void, Void, String>{

	 private Context context;
     private TextView statusText;
     
     /**
      * @param context
      * @param statusText
      */
     public FileClientAsyncTask(Context context, View statusText) {
         this.context = context;
         this.statusText = (TextView) statusText;
     }
     
	@Override
	protected String doInBackground(Void... params) {
		// TODO Auto-generated method stub
		
	//	Socket clientsocket = new Socket(5000);
		return null;
	}

}
