package com.mpower.clientcollection.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mpower.clientcollection.models.MessageInfos;
import com.mpower.clientcollection.utilities.DateUtils;
import com.mpower.clientcollection.utilities.UserCollection;


public class MpowerDatabaseHelper extends SQLiteOpenHelper{

public static final String DATABASE_NAME = "clientcollection.db";
public static final int DATABASE_VIRSION = 7;
private String mKey = "qjYcPCHkFpbTjntDCpXCGabSY5DFH";

// XML based interview basic info like schedleId or beneficiaryId.
public static final String INTERVIEW_TABLE_NAME = "Interview_Table";
public static final String INTERVIEW_TABLE_ID = "interviewId";
public static final String INTERVIEW_SCHEDULE_ID = "scheduleId";

// Notification message information
public static final String M_ID = "Id";
public static final String M_TABLE_NAME = "Message_Table";
public static final String M_NOTIFICATION_ID = "notificationId";
public static final String M_HH_ID = "hhId";
public static final String M_NAME = "beneficiaryname";
public static final String M_DATE = "ReceiveTime";
public static final String M_MESSAGE = "Message";
public static final String M_READ = "ReadStatus";
public static final int M_READ_STATUS = 0;
public static final String M_USER_ID = "userId";

private Context context; 
 
	public MpowerDatabaseHelper(Context context) {	
		super(context, DATABASE_NAME, null, DATABASE_VIRSION);
		this.context = context;
		
	}

/*	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			// Enable foreign key constraints
			// works since sqlite version 3.6.19 (Froyo)
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}
*/		
	@Override
	public void onCreate(SQLiteDatabase db) {
		String interviewTable = "CREATE TABLE IF NOT EXISTS " + INTERVIEW_TABLE_NAME + " (" + INTERVIEW_TABLE_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT,  " + INTERVIEW_SCHEDULE_ID + "  TEXT" + " );";
		db.execSQL(interviewTable);
		
		String messageCreateTableSql = "CREATE TABLE IF NOT EXISTS " + M_TABLE_NAME + " (" + M_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + M_USER_ID
				+ " TEXT NOT NULL," + M_NOTIFICATION_ID + " TEXT NOT NULL," + M_HH_ID + " TEXT NOT NULL," +  M_NAME + " TEXT," + M_DATE + " TEXT," + M_READ + " INTEGER,"
				+ M_MESSAGE + " TEXT);";

		db.execSQL(messageCreateTableSql);
		Log.i("MpowerDatabase", "create database");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
		db.execSQL("DROP TABLE IF EXISTS " + INTERVIEW_TABLE_NAME + " ;");
		db.execSQL("DROP TABLE IF EXISTS " + M_TABLE_NAME + " ;");
		
		//Create tables again 
		onCreate(db);
	}
	
	private SQLiteDatabase openWritableDatabase(){
		return this.getWritableDatabase();
	} 
	
	private SQLiteDatabase openReadableDatabase(){
		return this.getReadableDatabase();
	}
	
	public void insertInterviewBasicInfo(ContentValues cv){
		SQLiteDatabase db = this.openWritableDatabase();
		ContentValues values = new ContentValues(cv);
		db.insertOrThrow(INTERVIEW_TABLE_NAME, null, values);	
		db.close();
	}
	
	public void insertScheduleId(String scheduleId){
		ContentValues cv = new ContentValues();
		cv.put(INTERVIEW_SCHEDULE_ID, scheduleId);
		this.insertInterviewBasicInfo(cv);
	}
	
	public HashMap<String, String> getInterviewBasicInfo(){
		HashMap<String, String> map = new HashMap<String, String>();
		
		SQLiteDatabase db = this.openReadableDatabase();
		Cursor cur = db.rawQuery("SELECT * FROM " + INTERVIEW_TABLE_NAME, null);
		if(cur != null){
			if(cur.moveToFirst()){
				map.put(INTERVIEW_SCHEDULE_ID, cur.getString(cur.getColumnIndex(INTERVIEW_SCHEDULE_ID)));
			}			
		}
		cur.close();
		db.close();
		
		this.deleteDatabaseInfo();
		return map;
	}
	
	private void deleteDatabaseInfo(){
		SQLiteDatabase db = this.openWritableDatabase();
		db.execSQL("DELETE FROM " + INTERVIEW_TABLE_NAME);
		db.close();
	}
	
	//Notification message information insertion, deletion and modification
	public void insertMessagesInfo(ArrayList<MessageInfos> message) throws Exception {

		SQLiteDatabase db = this.getWritableDatabase();
		String userId = UserCollection.getInstance().getUserData().getUsername();
		ContentValues cv = new ContentValues();		
			for(MessageInfos s : message){
				cv.clear();
				cv.put(M_NOTIFICATION_ID, s.getNotificationId());
				cv.put(M_HH_ID, s.getHhid());
				cv.put(M_NAME, s.getBeneficiaryName());
				cv.put(M_DATE, DateUtils.getCurrentDateInString().toString());
				cv.put(M_MESSAGE, s.getNotificationMsg());
				cv.put(M_READ, s.getReadStatus());
				cv.put(M_USER_ID, userId);
				long i = db.insertWithOnConflict(M_TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
				Log.d("DataBasehelper", "Notification insetion status = " + i);
			}
		db.close();
	}

	public List<MessageInfos> getMessageInfo() {
		List<MessageInfos> messages = new ArrayList<MessageInfos>();

		SQLiteDatabase db = this.getReadableDatabase();
		String userId = UserCollection.getInstance().getUserData().getUsername();
		Cursor cur = db.rawQuery("SELECT * FROM " + M_TABLE_NAME + " WHERE " + M_USER_ID + " = '" + userId + "'" + " ORDER BY " + M_DATE + " DESC;",
				null);
		if (cur.moveToFirst()) {
			int _id = cur.getColumnIndex(M_ID);
			int _name = cur.getColumnIndex(M_NAME);
			int _notiId = cur.getColumnIndex(M_NOTIFICATION_ID);
			int _hhid = cur.getColumnIndex(M_HH_ID);
			int _date = cur.getColumnIndex(M_DATE);
			int _message = cur.getColumnIndex(M_MESSAGE);
			int _mark = cur.getColumnIndex(M_READ);
			
			do {
				MessageInfos mi = new MessageInfos();
				mi.setDbId(cur.getLong(_id));
				mi.setNotificationId(cur.getString(_notiId));
				mi.setBeneficiaryName(cur.getString(_name));
				mi.setNotificationMsg(cur.getString(_message));
				mi.setHhid(cur.getString(_hhid));
				mi.setDate(cur.getString(_date));
				mi.setReadStatus(cur.getInt(_mark));				
				messages.add(mi);
			} while (cur.moveToNext());
		}
		cur.close();
		db.close();
		return messages;
	}
	
	public String getHHId(long msgId) {
		String hhid = null;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cur = db.rawQuery("SELECT " + M_HH_ID + " FROM " + M_TABLE_NAME + " WHERE " + M_ID + " = '" + msgId + "';", null);
	
		if (cur.moveToFirst()) {
			int _hhid = cur.getColumnIndex(M_HH_ID);
			hhid = cur.getString((_hhid));
		}
		cur.close();
		db.close();

		return hhid;
	}
	
	public String getNotificationId(long msgId) {
		String hhid = null;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cur = db.rawQuery("SELECT " + M_NOTIFICATION_ID + " FROM " + M_TABLE_NAME + " WHERE " + M_ID + " = '" + msgId + "';", null);
		if (cur.moveToFirst()) {
			int _id = cur.getColumnIndex(M_NOTIFICATION_ID);
			hhid = cur.getString((_id));
		}
		cur.close();
		db.close();

		return hhid;
	}

	public String getMaxNotificationServerId(){
		String maxServerId = "0";
		String sql = "SELECT MAX("+ M_NOTIFICATION_ID + ") FROM " + M_TABLE_NAME + " WHERE " + M_USER_ID + " = '" + UserCollection.getInstance().getUserData().getUsername() + "';";
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cur = db.rawQuery(sql, null);
	
		if(cur.moveToFirst()){
			maxServerId = String.valueOf(cur.getLong(0));
			Log.d("DatabaseHelper", "maxServerId = " +maxServerId);
		}
		cur.close();
		db.close();
		return maxServerId;
	}
	
	public void updateReadStatus(long msgId, int status) {
		SQLiteDatabase db = this.getWritableDatabase();		
		db.execSQL("UPDATE " + M_TABLE_NAME + " SET " + M_READ + " = " + status + " WHERE " + M_ID + " = " + msgId + " ;");
		db.close();
	}

	public void deleteTotalMessages(String msghhId) {
		SQLiteDatabase db = this.getWritableDatabase();
		String userId = UserCollection.getInstance().getUserData().getUsername();
		String query = "";
		query = "DELETE FROM " + M_TABLE_NAME + " WHERE " + M_HH_ID + " = '" + msghhId + "'" + " AND " + M_USER_ID + " = '" + userId + "';";
		db.execSQL(query);
		db.close();
	}

	public void deleteMessage(long msgId) {
		SQLiteDatabase db = this.getWritableDatabase();
		String userId = UserCollection.getInstance().getUserData().getUsername().toString();
		String query = "";
		query = "DELETE FROM " + M_TABLE_NAME + " WHERE " + M_ID + " = '" + msgId + "' AND " + M_USER_ID + " = '" + userId + "';";
		db.execSQL(query);
		db.close();
	}
	public List<MessageInfos> getSelectedMessageInfo(String hhId) {
		List<MessageInfos> messages = new ArrayList<MessageInfos>();				
		SQLiteDatabase db = this.getReadableDatabase();
	
		/*String query = "SELECT * FROM " + M_TABLE_NAME + " WHERE " + M_SENDER_ID + " = '" + msgSender + "' OR '" + userId + "'" + " AND " + M_USER_ID + " = '" + userId
				             + "'" + " ORDER BY " + M_DATE + " ASC;";
		 */		
		String query = "SELECT * FROM " + M_TABLE_NAME + " WHERE " + M_HH_ID + " = '" + hhId + "'" + " ORDER BY " + M_DATE + " ASC;";

		Cursor cur = db.rawQuery(query, null);
		if (cur.moveToFirst()) {
			int _id = cur.getColumnIndex(M_ID);
			int _name = cur.getColumnIndex(M_NAME);
			int _notiId = cur.getColumnIndex(M_NOTIFICATION_ID);
			int _hhid = cur.getColumnIndex(M_HH_ID);
			int _date = cur.getColumnIndex(M_DATE);
			int _message = cur.getColumnIndex(M_MESSAGE);
			int _mark = cur.getColumnIndex(M_READ);
			
			do {
				MessageInfos mi = new MessageInfos();
				mi.setDbId(cur.getLong(_id));
				mi.setNotificationId(cur.getString(_notiId));
				mi.setBeneficiaryName(cur.getString(_name));
				mi.setNotificationMsg(cur.getString(_message));
				mi.setHhid(cur.getString(_hhid));
				mi.setDate(cur.getString(_date));
				mi.setReadStatus(cur.getInt(_mark));				
				messages.add(mi);
			} while (cur.moveToNext());
		}
		cur.close();
		db.close();
		return messages;		
	}

}
