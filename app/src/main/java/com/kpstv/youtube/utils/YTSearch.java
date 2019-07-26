package com.kpstv.youtube.utils;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class YTSearch {

    ArrayList<String> videoIDs; String TAG = "YTSearchThread";

    public YTSearch(String query) {

        String tosearch = query.replace(" ","+");

        videoIDs = new ArrayList<>();
        try {
            String url = "https://www.youtube.com/results?search_query="+tosearch;
            URLConnection connection = (new URL(url)).openConnection();
            connection.connect();
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line = null;
            while((line = reader.readLine()) != null)
            {
                if (line.contains("src=\"https://i.ytimg.com"))
                    videoIDs.add(line.split("/")[4]);
            }
            in.close();

        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
        } catch (ProtocolException e) {
            Log.e(TAG, "ProtocolException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    public ArrayList<String> getVideoIDs() {
        return videoIDs;
    }
}