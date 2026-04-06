package com.hw.gallery4;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database name and version
    public static final String DATABASE_NAME = "PhotoAlbumApp.db";
    public static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_ALBUM = "Album";
    public static final String TABLE_IMAGE = "Image";
    public static final String TABLE_ALBUM_IMG = "AlbumImg";
    public static final String TABLE_AUTO_SCAN_FLD = "AutoScanFld";

    // Column names for Album table
    public static final String COLUMN_ALBUM_ID = "ID";
    public static final String COLUMN_ALBUM_NAME = "Name";
    public static final String COLUMN_ALBUM_DESCRIPTION = "Description";

    // Column names for Image table
    public static final String COLUMN_IMG_ADDRESS = "ImgAddress";
    public static final String COLUMN_IMG_DESCRIPTION = "Description";
    public static final String COLUMN_IMG_RATING = "Rating";
    public static final String COLUMN_IMG_FAVORITE = "Favorite";

    // Column names for AlbumImg table
    public static final String COLUMN_ALBUM_IMG_ALBUM_ID = "AlbumID";
    public static final String COLUMN_ALBUM_IMG_IMG_ADDRESS = "ImgAddress";

    // Column names for AutoScanFld table
    public static final String COLUMN_FLD_ADDRESS = "FldAddress";

    // SQL statements to create tables
    private static final String CREATE_TABLE_ALBUM = "CREATE TABLE " + TABLE_ALBUM + "("
            + COLUMN_ALBUM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_ALBUM_NAME + " TEXT,"
            + COLUMN_ALBUM_DESCRIPTION + " TEXT"
            + ")";

    private static final String CREATE_TABLE_IMAGE = "CREATE TABLE " + TABLE_IMAGE + "("
            + COLUMN_IMG_ADDRESS + " TEXT PRIMARY KEY,"
            + COLUMN_IMG_DESCRIPTION + " TEXT,"
            + COLUMN_IMG_RATING + " INTEGER,"
            + COLUMN_IMG_FAVORITE + " INTEGER DEFAULT 0"
            + ")";

    private static final String CREATE_TABLE_ALBUM_IMG = "CREATE TABLE " + TABLE_ALBUM_IMG + "("
            + COLUMN_ALBUM_IMG_ALBUM_ID + " INTEGER,"
            + COLUMN_ALBUM_IMG_IMG_ADDRESS + " TEXT,"
            + "PRIMARY KEY (" + COLUMN_ALBUM_IMG_ALBUM_ID + ", " + COLUMN_ALBUM_IMG_IMG_ADDRESS + "),"
            + "FOREIGN KEY (" + COLUMN_ALBUM_IMG_ALBUM_ID + ") REFERENCES " + TABLE_ALBUM + "(" + COLUMN_ALBUM_ID + "),"
            + "FOREIGN KEY (" + COLUMN_ALBUM_IMG_IMG_ADDRESS + ") REFERENCES " + TABLE_IMAGE + "(" + COLUMN_IMG_ADDRESS + ")"
            + ")";

    private static final String CREATE_TABLE_AUTO_SCAN_FLD = "CREATE TABLE " + TABLE_AUTO_SCAN_FLD + "("
            + COLUMN_FLD_ADDRESS + " TEXT PRIMARY KEY"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables
        db.execSQL(CREATE_TABLE_ALBUM);
        db.execSQL(CREATE_TABLE_IMAGE);
        db.execSQL(CREATE_TABLE_ALBUM_IMG);
        db.execSQL(CREATE_TABLE_AUTO_SCAN_FLD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if they exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUM);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUM_IMG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTO_SCAN_FLD);

        // Create tables again
        onCreate(db);
    }
}