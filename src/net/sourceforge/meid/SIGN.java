package net.sourceforge.meid;


import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import net.sourceforge.meid.DATABASE;
import net.sourceforge.meid.send;
import net.sourceforge.meid.R;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
//import android.os.Handler;
import android.provider.MediaStore;
//import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SIGN extends Activity implements OnClickListener{
	DATABASE db;
	EditText name, phone,date,ssn,email;
	byte[] byteImage1 = null;
    public static final int SELECT_PICTURE = 1;

///// Hossam start
//public final Handler mHandler = new Handler();
	
	//public final Runnable mUpdateResults = new Runnable() {
    //public void run() {
      //  Toast(this, "pla pla pla ...", 1000).show();
    //};
	////Hossam end
	
    
    public String selectedImagePath;
    public String selectedImagePath1;

    
    int flag;

    
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);
        
        // instantiate a DBAdapter
        db = new DATABASE(this);
        
        // open a connection to the DB, and create
        // a table if one does not yet exist.
        db.open();
        
        // connect to UI elements
        Button auth = (Button)findViewById(R.id.Reye);
        Button save = (Button)findViewById(R.id.save);
        Button save2 = (Button)findViewById(R.id.Leye);

        name 	 = (EditText)findViewById(R.id.username);
        phone 	 = (EditText)findViewById(R.id.password);
        date	 = (EditText)findViewById(R.id.NameID);
        ssn		 = (EditText)findViewById(R.id.SSNID);
        email	 = (EditText)findViewById(R.id.Email);

        // allow our buttons to do something
        auth.setOnClickListener(this);
        save.setOnClickListener(this);
        save2.setOnClickListener(this);
        

	}


	/** Explicitly close our database connection when our
	 * application is done with it and we're about to quit.
	 */
	public void onDestroy() {
		super.onDestroy();
		db.close();
	}


	/** Perform the requested button action: Save will add a
	 * username and password pair to the SQLite table, and
	 * Authenticate will query the database to see if the 
	 * current user/pass combination are present.
	 */
	@Override
	public void onClick(View v) {

		// find the username and password that were entered
		String Name 	= name.getText().toString();
		String PHONE 	= phone.getText().toString();
		String DATE		= date.getText().toString(); 
		String SSN		= ssn.getText().toString();
		String EMAIL		= email.getText().toString();

		String PATH1 = selectedImagePath;
		String PATH2 = selectedImagePath1;
		// if the "Save" button was pushed, we'll save the user/pass into the DB.
		// otherwise we'll try to 'authenticate' by verifying the user/pass exist in the db
		switch(v.getId()) {
			case R.id.save:

				// insertUser() method will insert a user and return a row ID
				long id = db.insertUser(Name, PHONE, DATE, SSN,EMAIL,PATH1,PATH2);
				
				// if the row ID is -1 there was some error, otherwise it was successful
				if (id != -1)
					displayMessage(Name + " inserted!");
				else
					displayMessage(Name + " wasn't inserted?"); 
				
				
				
						new Thread(new Runnable() {
			                 public void run() {
			                	 String[] datatosend = new String[7];
									datatosend = db.getdata();
									String path1 = datatosend[5];
								
			                	 send s = new send();
									s.sendimage(path1, "http://meid.dx.am/UploadToServer.php");
			                 }
			               }).start();
						
						new Thread(new Runnable() {
			                 public void run() {
			                	 String[] datatosend = new String[7];
									datatosend = db.getdata();
									String path2 = datatosend[6];
				                	 send s1 = new send();
									s1.sendimage(path2, "http://meid.dx.am/UploadToServer.php");

			                 }
			               }).start();
						
			                
						new Thread(new Runnable() {
			                 public void run() { 
						String[] datatosend = new String[7];
							datatosend = db.getdata();
							String name = datatosend[0];
	                	 String phone = datatosend[1];
	                	 String date = datatosend[2];
	                	 String ssn = datatosend[3];
	                	 String email = datatosend[4];
	                	 String path1 = datatosend[5];
	                	 String path2 = datatosend[6];
	             		String[] im1=path1.split("/");
	             		String[] im2=path2.split("/");
	            		String photo1=im1[im1.length-1];
	            		String photo2=im2[im2.length-1];
	            		
	            		ArrayList<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
	                	 nameValuePair.add(new BasicNameValuePair("name", name));
	                	 nameValuePair.add(new BasicNameValuePair("phone", phone));
	                	 nameValuePair.add(new BasicNameValuePair("date", date));
	                	 nameValuePair.add(new BasicNameValuePair("ssn", ssn));
	                	 nameValuePair.add(new BasicNameValuePair("email", email));
	                	 nameValuePair.add(new BasicNameValuePair("path1", photo1));
	                	 nameValuePair.add(new BasicNameValuePair("path2", photo2));
	                	 send s = new send();
	                	 s.senddata("http://meid.dx.am/insert.php", nameValuePair);
			                 }
			               }).start();

				break;
			case R.id.Reye: 
				Intent intent = new Intent();
	            intent.setType("image/*");
	            intent.setAction(Intent.ACTION_GET_CONTENT);
	            startActivityForResult(Intent.createChooser(intent, "Select Picture"),SELECT_PICTURE);
	            flag=0;
	            break;

			case R.id.Leye:default :
				Intent intent2 = new Intent();
	            intent2.setType("image/*");
	            intent2.setAction(Intent.ACTION_GET_CONTENT);
	            startActivityForResult(Intent.createChooser(intent2, "Select Picture"),SELECT_PICTURE);
	            flag=1;
	            break;
				/*
				// attempt to authenticate a user. It will return true
				// if authenticated or false otherwise.
				if(db.authenticateUser(Name, PHONE)) {
					displayMessage(Name + " authenticated!");
				} else {
					displayMessage("Authentication failed for "+Name + "!");
				}
				*/
		}
	}

	//added for image view
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
	        if (resultCode == RESULT_OK) {
	            if (requestCode == SELECT_PICTURE) {
	                Uri selectedImageUri = data.getData();
	                if (flag==0){
	                selectedImagePath = getPath(selectedImageUri);
	                }
	                if (flag==1){
		                selectedImagePath1 = getPath(selectedImageUri);
		                }
	               // System.out.println("Image Path : " + selectedImagePath);
	               // image1.setVisibility(View.VISIBLE);
	               // image1.setImageURI(selectedImageUri);           
	            }
	        }
	    }
		//added for image view
		@SuppressWarnings("deprecation")
	    public String getPath(Uri uri) {
	        String[] projection = { MediaStore.Images.Media.DATA };
	        Cursor cursor = managedQuery(uri, projection, null, null, null);
	        int column_index = cursor
	                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        cursor.moveToFirst();
	        return cursor.getString(column_index);
	    }

	/** Display a long Toast as feedback for this Activity.
	 * 
	 * @param msg is the string to display
	 */
	private void displayMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
}