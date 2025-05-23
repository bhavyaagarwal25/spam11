package com.example.spam;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/* Bare-minimum provider so Android sees one and lets us be default SMS app */
public class SmsContentProvider extends ContentProvider {

    public static final String AUTH = "com.example.spam.provider";

    @Override public boolean onCreate() { return true; }

    @Nullable @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] p, @Nullable String s,
                        @Nullable String[] a, @Nullable String o) { return null; }

    @Nullable @Override
    public String getType(@NonNull Uri uri) { return null; }

    @Nullable @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues v) { return null; }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] a) { return 0; }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues v,
                      @Nullable String s, @Nullable String[] a) { return 0; }
}
