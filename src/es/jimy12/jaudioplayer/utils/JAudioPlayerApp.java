package es.jimy12.jaudioplayer.utils;

import java.util.ArrayList;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import es.jimy12.jaudioplayer.R;

public class JAudioPlayerApp extends Application {

	private static Context context;

	private static ArrayList<String> songNameList;
	private static ArrayList<String> songURLList;
	

    public void onCreate(){
        super.onCreate();
        
        songNameList = new ArrayList<String>();
        songURLList = new ArrayList<String>();
        
        JAudioPlayerApp.context = getApplicationContext();      
        
        // Recover songs list
        FileUtils.copyFromAssets(JAudioPlayerApp.getAppContext().getString(R.string.songs_file));		
        
		FileUtils.extractSongNames(songNameList, songURLList);
		
    }
    
	public static Context getAppContext() {
        return JAudioPlayerApp.context;
    }
    
	public static ArrayList<String> getSongNameList() {
        return JAudioPlayerApp.songNameList;
    }
    
	public static ArrayList<String> getSongURLList() {
        return JAudioPlayerApp.songURLList;
    }
	
	// Test Internet connection
	public static boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
}
