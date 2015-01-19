package com.iiiP.billboardtubelist;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.OpenableColumns;

public class SQLitHelper extends SQLiteOpenHelper  {
	
	private static final int VERSION = 1;
	


	public SQLitHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS youtubedb");
		final String createTable = "CREATE TABLE IF NOT EXISTS youtubedb"+
				"(_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
				"yindex INTEGER, ytitle TEXT, yartist TEXT, yvideoimg TEXT)";
		
		db.execSQL(createTable);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	   db.execSQL("DROP TABLE IF EXISTS youtubedb"); 
	   onCreate(db);
	
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		super.onOpen(db);
	}

}
