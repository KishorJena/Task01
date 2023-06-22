package com.jena.task01;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mydatabase2.db";
    private static final int DATABASE_VERSION = 1;

    private static final String query1 = "CREATE TABLE user ";
    private static final String query2 = "(_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, surname TEXT, gender TEXT, image BLOB, uploaded INTEGER DEFAULT 0)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery =  query1 + query2;
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrade if needed
    }

    public void saveUser(ContentValues user) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.insert("user", null, user);

        db.close();
    }


    public List<ContentValues> getAllUsers3() {
        List<ContentValues> userList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query("user", null, null, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("_id");
                int nameIndex = cursor.getColumnIndex("name");
                int surnameIndex = cursor.getColumnIndex("surname");
                int genderIndex = cursor.getColumnIndex("gender");
                int imageIndex = cursor.getColumnIndex("image");
                int uploadedIndex = cursor.getColumnIndex("uploaded");

                do {
                    ContentValues values = new ContentValues();
                    values.put("_id", cursor.getInt(idIndex));
                    values.put("name", cursor.getString(nameIndex));
                    values.put("surname", cursor.getString(surnameIndex));
                    values.put("gender", cursor.getString(genderIndex));
                    values.put("image", cursor.getBlob(imageIndex));
                    values.put("uploaded", cursor.getInt(uploadedIndex));

                    userList.add(values);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        db.close();

        return userList;
    }
    public void setUploadedStatus(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("uploaded", 1);
        db.update("user", values, "_id=?", new String[]{String.valueOf(id)});
        db.close();
    }


}
