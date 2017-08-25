/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.udacity.example.quizexample;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.example.droidtermsprovider.DroidTermsExampleContract;

/**
 * Gets the data from the ContentProvider and shows a series of flash cards.
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // The data from the DroidTermsExample content provider
    private Cursor mData;

    // The current state of the app
    private int mCurrentState;

    // The index of the definition and word column in the cursor
    private int mDefCol, mWordCol;

    private TextView mWordTextView, mDefinitionTextView;
    private Button mButton;

    // This state is when the word definition is hidden and clicking the button will therefore
    // show the definition
    private final int STATE_HIDDEN = 0;

    // This state is when the word definition is shown and clicking the button will therefore
    // advance the app to the next word
    private final int STATE_SHOWN = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the views
        // COMPLETED (1) You'll probably want more than just the Button
        mWordTextView = (TextView) findViewById(R.id.text_view_word);
        mDefinitionTextView = (TextView) findViewById(R.id.text_view_definition);
        mButton = (Button) findViewById(R.id.button_next);

        //Run the database operation to get the cursor off of the main thread
        new WordFetchTask().execute();

        boolean hasPackage = isPackageExisted(MainActivity.this, "se.sugarest.jane.popularmovies");
        Log.i(TAG, "Popular Movies App exist: " + hasPackage);

        if (hasPackage) {
            exposePopularMovieDbViaContentProvider();
        }
    }

    private void exposePopularMovieDbViaContentProvider() {
        Uri popularMovieCacheTableContentUri = Uri.parse("content://se.sugarest.jane.popularmovies/cache_movie_most_popular");
        ContentProviderClient popularMovieContentProvider = getContentResolver().acquireContentProviderClient(popularMovieCacheTableContentUri);
        try {
            Cursor cursor = popularMovieContentProvider.query(popularMovieCacheTableContentUri, null, null, null, null);
            int i = cursor.getCount();
            Log.i(TAG, "There are " + i + " items inside popular movies cache table.");
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException: cannot find Popular Movies Content Provider. " + e.getMessage());
        } catch (NullPointerException e) {
            Toast.makeText(this, "Please install Popular Movies App first to get data.", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isPackageExisted(Context c, String targetPackage) {

        PackageManager pm = c.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage,
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * This is called from the layout when the button is clicked and switches between the
     * two app states.
     *
     * @param view The view that was clicked
     */
    public void onButtonClick(View view) {

        // Either show the definition of the current word, or if the definition is currently
        // showing, move to the next word.
        switch (mCurrentState) {
            case STATE_HIDDEN:
                showDefinition();
                break;
            case STATE_SHOWN:
                nextWord();
                break;
        }
    }

    public void nextWord() {
        // COMPLETED (3) Go to the next word in the Cursor, show the next word and hide the definition
        // Note that you shouldn't try to do this if the cursor hasn't been set yet.
        // If you reach the end of the list of words, you should start at the beginning again.

        if (mData != null) {
            // Move to the next position in the cursor, if there isn't one, move to the first
            if (!mData.moveToNext()) {
                mData.moveToFirst();
            }
            // Hide the definition TextView
            mDefinitionTextView.setVisibility(View.INVISIBLE);

            // Change button text
            mButton.setText(getString(R.string.show_definition));

            // Get the next word
            mWordTextView.setText(mData.getString(mWordCol));
            mDefinitionTextView.setText(mData.getString(mDefCol));

            mCurrentState = STATE_HIDDEN;
        }
    }

    public void showDefinition() {
        // COMPLETED (4) Show the definition
        if (mData != null) {
            // Show the definition TextView
            mDefinitionTextView.setVisibility(View.VISIBLE);

            // Change button text
            mButton.setText(getString(R.string.next_word));

            mCurrentState = STATE_SHOWN;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // COMPLETED (5) Remember to close your cursor!
        mData.close();
    }

    // Use an async task to do the data fetch off of the main thread.
    public class WordFetchTask extends AsyncTask<Void, Void, Cursor> {

        // Invoked on a background thread
        @Override
        protected Cursor doInBackground(Void... params) {
            // Make the query to get the data

            // Get the content resolver
            ContentResolver resolver = getContentResolver();

            // Call the query method on the resolver with the correct Uri from the contract class
            Cursor cursor = resolver.query(DroidTermsExampleContract.CONTENT_URI,
                    null, null, null, null);
            return cursor;
        }


        // Invoked on UI thread
        @Override
        protected void onPostExecute(Cursor cursor) {
            super.onPostExecute(cursor);
            // COMPLETED (2) Initialize anything that you need the cursor for, such as setting up
            // the screen with the first word and setting any other instance variables

            //Set up a bunch of instance variables based off of the data

            // Set the data for MainActivity
            mData = cursor;
            // Get the column index, in the Cursor, of each piece of data
            mDefCol = mData.getColumnIndex(DroidTermsExampleContract.COLUMN_DEFINITION);
            mWordCol = mData.getColumnIndex(DroidTermsExampleContract.COLUMN_WORD);

            // Set the initial state
            nextWord();
        }
    }

}
