package es.jimy12.jaudioplayer.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import es.jimy12.jaudioplayer.R;

public class FileUtils {
	
	// Song names from file to Global ArrayList
	public static void extractSongNames(ArrayList<String> songNames, ArrayList<String> songURLs){
		
		String fileName = getAppDir() + "/audio/" + JAudioPlayerApp.getAppContext().getString(R.string.songs_file);
		try {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8")); 

		    // do reading, usually loop until end of file reading 
		    String mLine = reader.readLine();
		    while (mLine != null) {
		        //process line
		    	String name = mLine.substring(0,mLine.indexOf("http")-1);
		    	String url = mLine.substring(mLine.indexOf("http"));
		    	songNames.add(name);
		    	songURLs.add(url);
		    	mLine = reader.readLine(); 
		    }

		    reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
    	
	// Get audio folder in device's storage
	public static String getAppDir (){
		String appDir;
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
			appDir = android.os.Environment.getExternalStorageDirectory() + "/data/" + JAudioPlayerApp.getAppContext().getPackageName();
			new File(appDir).mkdirs();
			new File(appDir + "/audio").mkdirs();
		} else {
			PackageManager m = JAudioPlayerApp.getAppContext().getPackageManager();
			appDir = JAudioPlayerApp.getAppContext().getPackageName();
			try {
			    PackageInfo p = m.getPackageInfo(appDir, 0);
			    appDir = p.applicationInfo.dataDir;
				new File(appDir,"audio").mkdirs();
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
        }
		return appDir;
	}
	
	// Read file with songs list
	public static String readFileFromAudioDir(String fileName) {
		String result = "";
		fileName = getAppDir() + "/audio/" + fileName;
		try {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8")); 

		    // do reading, usually loop until end of file reading 
		    String mLine = reader.readLine();
		    while (mLine != null) {
		       //process line
		       result += mLine;
		       mLine = reader.readLine(); 
		    }

		    reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	// Check if song file exist in audio folder
	public static Boolean existAudio(String audioName) {
		File f = new File(getAppDir() + "/audio/" + audioName);
		if (!f.exists()) {
			return false;
		}
		return true;
	}
	
	public static Boolean existFile(String fileName) {
		File f = new File(fileName);
		if (!f.exists()) {
			return false;
		}
		return true;
	}
	
	public static void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}
	
	public static void copyFromAssets(String fileName){
		try {
			InputStream is = JAudioPlayerApp.getAppContext().getAssets().open("files/" + fileName);
			String file = getAppDir() + "/audio/" + fileName;
			FileOutputStream os = new FileOutputStream (file);
			copyFile(is, os);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void removeFile (String fileName){
		File f = new File(fileName);
		f.delete();
	}
	
	public static void removeAudio (String audioName){
		File f = new File(getAppDir() + "/audio/" + audioName);
		f.delete();
	}	
	
	// Download file from Internet
	public static void downloadFile(final String fileURL){
		new Thread(new Runnable() {
			public void run() {
				FileOutputStream fos;
				String tempFileName;
				String finalFileName;
				BufferedInputStream in;
				String fileName = fileFromURL(fileURL);
				if(JAudioPlayerApp.isOnline()){
					try {
						tempFileName = getAppDir() + "/audio/" + "temp" + fileName;
						finalFileName = getAppDir() + "/audio/" + fileName;
						fos = new FileOutputStream(tempFileName);
						in = new BufferedInputStream(new URL(fileURL).openStream());
						byte data[] = new byte[1024];
						int count;
						while ((count = in.read(data, 0, 1024)) != -1) {
							fos.write(data, 0, count);
						}
						File tempFile = new File(tempFileName);
						File finalFile = new File(finalFileName);
						tempFile.renameTo(finalFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	// Get file name from URL
	public static String fileFromURL(String url){
		String fileName = url.substring(url.lastIndexOf("/")+1);
		fileName = fileName.replaceAll("%20"," ");
		return fileName;
	}
	
	// Check an incomplete download of file
	public static Integer existTempFileWithName(String name){
		String folderName = getAppDir() + "/audio/";
		File folder = new File(folderName);
		
		File files[] = folder.listFiles();
		for(int i=0; i<files.length; i++){
			if(files[i].getName().contains(name) && files[i].getName().contains("temp")){
				Integer totalSize = Integer.valueOf(files[i].getName().substring(4, 16));
				return totalSize;
			}
		}
		return 0;
	}
}

