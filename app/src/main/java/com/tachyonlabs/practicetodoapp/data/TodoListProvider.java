package com.tachyonlabs.practicetodoapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class TodoListProvider extends ContentProvider{
    public static final int CODE_TODOS = 100;
    public static final int CODE_TODO_WITH_ID = 101;

    private static UriMatcher sUriMatcher = buildUriMatcher();
    private TodoListDbHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new TodoListDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor;

        switch (sUriMatcher.match(uri)) {
            case CODE_TODOS:
                cursor = mOpenHelper.getReadableDatabase().query(
                        TodoListContract.TodoListEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case CODE_TODOS:
                long id = db.insert(TodoListContract.TodoListEntry.TABLE_NAME, null, contentValues);
                if ( id > 0 ) {
                    returnUri = ContentUris.withAppendedId(TodoListContract.TodoListEntry.CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        int tasksDeleted;

        switch (match) {
            case CODE_TODO_WITH_ID:
                String id = uri.getPathSegments().get(1);
                tasksDeleted = db.delete(TodoListContract.TodoListEntry.TABLE_NAME, "_id=?", new String[]{id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (tasksDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return tasksDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        int tasksUpdated;

        switch (match) {
            case CODE_TODO_WITH_ID:
                String id = uri.getPathSegments().get(1);
                tasksUpdated = db.update(TodoListContract.TodoListEntry.TABLE_NAME, contentValues, "_id=?", new String[]{id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (tasksUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return tasksUpdated;
    }

    public static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = TodoListContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, TodoListContract.PATH_TODOLIST, CODE_TODOS);
        return matcher;
    }



}