package net.sourceforge.meid;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;



//import android.app.ProgressDialog;
import android.util.Log;


public class send
{
	//private ProgressDialog dialog = null;
	private int serverResponseCode = 0;
	
	
	public  void  sendimage(String path, String uploadserverurl)
	{
String fileName = path;

HttpURLConnection conn = null;
DataOutputStream dos = null;  
String lineEnd = "\r\n";
String twoHyphens = "--";
String boundary = "*****";
int bytesRead, bytesAvailable, bufferSize;
byte[] buffer;
int maxBufferSize = 1 * 1024 * 1024;

File sourcefile = new File(path);

	try{
		// open a URL connection to the Servlet
FileInputStream fileInputStream = new FileInputStream(sourcefile);
URL url = new URL(uploadserverurl);

//Open a HTTP  connection to  the URL
conn = (HttpURLConnection) url.openConnection(); 
conn.setDoInput(true); // Allow Inputs
conn.setDoOutput(true); // Allow Outputs
conn.setUseCaches(false); // Don't use a Cached Copy
conn.setRequestMethod("POST");
conn.setRequestProperty("Connection", "Keep-Alive");
conn.setRequestProperty("ENCTYPE", "multipart/form-data");
conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
conn.setRequestProperty("uploaded_file", fileName); 

dos = new DataOutputStream(conn.getOutputStream());

dos.writeBytes(twoHyphens + boundary + lineEnd); 
dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
		                     + fileName + "\"" + lineEnd);

dos.writeBytes(lineEnd);

// create a buffer of  maximum size
bytesAvailable = fileInputStream.available(); 

bufferSize = Math.min(bytesAvailable, maxBufferSize);
buffer = new byte[bufferSize];

// read file and write it into form...
bytesRead = fileInputStream.read(buffer, 0, bufferSize);  
  
while (bytesRead > 0) {
	   
  dos.write(buffer, 0, bufferSize);
  bytesAvailable = fileInputStream.available();
  bufferSize = Math.min(bytesAvailable, maxBufferSize);
  bytesRead = fileInputStream.read(buffer, 0, bufferSize);   
  
 }

// send multipart form data necesssary after file data...
dos.writeBytes(lineEnd);
dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

// Responses from the server (code and message)
serverResponseCode = conn.getResponseCode();
String serverResponseMessage = conn.getResponseMessage();
 
Log.i("uploadFile", "HTTP Response is : " 
		   + serverResponseMessage + ": " + serverResponseCode);

if(serverResponseCode == 200){
	   
    //runOnUiThread(new Runnable() {
         //public void run() {
         	//String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
   		      //    +" http://meid.dx.am/";
         	//messageText.setText(msg);
             //Toast.makeText(MainActivity.this, "File Upload Complete.", Toast.LENGTH_SHORT).show();
         //}
     //});                
}    

//close the streams //
fileInputStream.close();
dos.flush();
dos.close();
 
} catch (MalformedURLException ex) {

//dialog.dismiss();  
//ex.printStackTrace();

//runOnUiThread(new Runnable() {
  // public void run() {
 	//  messageText.setText("MalformedURLException Exception : check script url.");
      // Toast.makeText(MainActivity.this, "MalformedURLException", Toast.LENGTH_SHORT).show();
   //}
//});

Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
} catch (Exception e) {

//dialog.dismiss();  
//e.printStackTrace();

//runOnUiThread(new Runnable() {
  // public void run() {
 	//  messageText.setText("Got Exception : see logcat ");
      // Toast.makeText(MainActivity.this, "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();
  // }
//});
Log.e("Upload file to server Exception", "Exception : "  + e.getMessage(), e);  
//}
//dialog.dismiss();       
//return serverResponseCode; 

//} // End else block 
//}

}
	
}
	
	public void senddata(String url, ArrayList<NameValuePair> nameValuePairs) {
		InputStream is = null;
		String line = "";
		String result = "";
		int code;
		try
    	{
		HttpClient httpclient = new DefaultHttpClient();
	        HttpPost httppost = new HttpPost(url);
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	        HttpResponse response = httpclient.execute(httppost); 
	        HttpEntity entity = response.getEntity();
	        is = entity.getContent();
	        Log.i("pass 1", "connection success ");
	}
        catch(Exception e)
	{
        	Log.e("Fail 1", e.toString());
	    	//Toast.makeText(getApplicationContext(), "Invalid IP Address",
			//Toast.LENGTH_LONG).show();
	}     
        
        try
        {
            BufferedReader reader = new BufferedReader
			(new InputStreamReader(is,"iso-8859-1"),8);
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null)
	    {
                sb.append(line + "\n");
            }
            is.close();
            result = sb.toString();
	    Log.i("pass 2", "connection success ");
	}
        catch(Exception e)
	{
            Log.e("Fail 2", e.toString());
	}     
       
	try
	{
            JSONObject json_data = new JSONObject(result);
            code=(json_data.getInt("code"));
			
            if(code==1)
            {
		//Toast.makeText(getBaseContext(), "Inserted Successfully",
			//Toast.LENGTH_SHORT).show();
            Log.i("pass 3","Inserted successfully");
            }
            else
            {
		 //Toast.makeText(getBaseContext(), "Sorry, Try Again",
			//Toast.LENGTH_LONG).show();
            	Log.e("fail 3","Sorry, Try Again");
            }
	}
	catch(Exception e)
	{
            Log.e("Fail 4", e.toString());
	}
    
    
	
	}
}