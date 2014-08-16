package net.sourceforge.meid;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DATABASE {

	// define the layout of our table in fields
	// "_id" is used by Android for Content Providers and should
	// generally be an auto-incrementing key in every table.
	public static final String KEY_ROWID = "_id";
	public static final String KEY_NAME  = "name";
	public static final String KEY_PHONE = "phone";
	public static final String KEY_DATE  = "date";
	public static final String KEY_SSN   = "ssn";
	public static final String KEY_EMAIL   = "email";
	public static final String KEY_PATH  = "path";
	public static final String KEY_PATH2  = "path2";

	// define some SQLite database fields
	// Take a look at your DB on the emulator with:
	// 	adb shell 
	//  sqlite3 /data/data/<pkg_name>/databases/<DB_NAME>
	private static final String DB_NAME  = "db_example";
	private static final String DB_TABLE = "patients";
	private static final int    DB_VER   = 4;

	// a SQL statement to create a new table
	private static final String DB_CREATE = "CREATE TABLE patients ("+
	"_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT ,date TEXT,ssn TEXT,email TEXT, path TEXT, path2 TEXT);";

	
	
	// define an extension of the SQLiteOpenHelper to handle the
	// creation and upgrade of a table
	private static class DatabaseHelper extends SQLiteOpenHelper {

		// Class constructor
		DatabaseHelper(Context c) {
			// instantiate a SQLiteOpenHelper by passing it
			// the context, the database's name, a CursorFactory 
			// (null by default), and the database version.
			super(c, DB_NAME, null, DB_VER);
		}

		// called by the parent class when a DB doesn't exist
		public void onCreate(SQLiteDatabase db) {
			// Execute our DB_CREATE statement
			db.execSQL(DB_CREATE);
		}
		
		// called by the parent when a DB needs to be upgraded
		public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
			// remove the old version and create a new one.
			// If we were really upgrading we'd try to move data over
			db.execSQL("DROP TABLE IF EXISTS "+DB_TABLE);
			onCreate(db);
		}
	}


	// useful fields in the class
    private final Context context;	
    private DatabaseHelper helper;
    private SQLiteDatabase db;

    // DBAdapter class constructor
	public DATABASE(Context c) {
		this.context = c;
	}
	
	/** Open the DB, or throw a SQLException if we cannot open
	  * or create a new DB.
	  */ 
	public DATABASE open() throws SQLException {
		// instantiate a DatabaseHelper class (see above)
		helper = new DatabaseHelper(context);

		// the SQLiteOpenHelper class (a parent of DatabaseHelper)
		// has a "getWritableDatabase" method that returns an
		// object of type SQLiteDatabase that represents an open
		// connection to the database we've opened (or created).
		db = helper.getWritableDatabase();

		return this;
	}
	
	/** Close the DB
	  */
	public void close() {
		helper.close();
	}

	/** Insert a user and password into the db
	  * 
	  * @param user username (string)
	  * @param pass user's password (string)
	  * @return the row id, or -1 on failure
	 */
	public long insertUser(String Name, String Phone, String Date,String Ssn,String Email,String Path,String Path2) {
		ContentValues vals = new ContentValues();
		vals.put(KEY_NAME, Name);
		vals.put(KEY_PHONE, Phone);
		vals.put(KEY_DATE, Date);
		vals.put(KEY_SSN, Ssn);
		vals.put(KEY_EMAIL, Email);
		vals.put(KEY_PATH, Path);
		vals.put(KEY_PATH2, Path2);

		return db.insert(DB_TABLE, null, vals);
	}

	/** Authenticate a user by querying the table to see
	  * if that user and password exist. We expect only one row
	  * to be returned if that combination exists, and if so, we
	  * have successfully authenticated.
	  * 
	  * @param user username (string)
	  * @param pass user's password (string)
	  * @return true if authenticated, false otherwise
	 */
	public boolean authenticateUser(String name, String phone) {
		// Perform a database query
		Cursor cursor = db.query(
				DB_TABLE, // table to perform the query
				new String[] { KEY_NAME }, //resultset columns/fields
				KEY_NAME+"=? AND "+KEY_PHONE+"=?", //condition or selection
				new String[] { name, phone },  //selection arguments (fills in '?' above)
				null,  //groupBy
				null,  //having
				null,  //having
				null   //orderBy
			);

		// if a Cursor object was returned by the query and
		// that query returns exactly 1 row, then we've authenticated
		if(cursor != null && cursor.getCount() == 1) {
			return true;
		}
		
		// The query returned no results or the incorrect
		// number of rows
		return false;
	}
public String[] getdata()
{
Cursor c = db.query(DB_TABLE, null, null, null, null, null, null,null);

String[] resultcursor=new String[7];
int iname = c.getColumnIndex(KEY_NAME);
int iphone = c.getColumnIndex(KEY_PHONE);
int idate = c.getColumnIndex(KEY_DATE);
int issn = c.getColumnIndex(KEY_SSN);
int iemail = c.getColumnIndex(KEY_EMAIL);
int ipath = c.getColumnIndex(KEY_PATH);
int ipath2 = c.getColumnIndex(KEY_PATH2);

for(c.moveToFirst();!c.isAfterLast();c.moveToNext())
{
	resultcursor[0] = c.getString(iname);
	resultcursor[1] = c.getString(iphone);
	resultcursor[2] = c.getString(idate);
	resultcursor[3] = c.getString(issn);
	resultcursor[4] = c.getString(iemail);
	resultcursor[5] = c.getString(ipath);
	resultcursor[6] = c.getString(ipath2);

}
return resultcursor;
}

}