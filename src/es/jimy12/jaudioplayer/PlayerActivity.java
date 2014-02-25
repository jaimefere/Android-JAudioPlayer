package es.jimy12.jaudioplayer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import es.jimy12.jaudioplayer.utils.FileUtils;
import es.jimy12.jaudioplayer.utils.JAudioPlayerApp;

@SuppressLint("NewApi")
public class PlayerActivity extends Activity implements OnItemClickListener{
	
	public enum SongStatus {
		INIT, DOWNLOADABLE, PLAYING, DOWNLOADING, PAUSE;
	}
	
	private ArrayAdapter<String> songsAA;
	private Integer selectedSong;
	private MediaPlayer mediaPlayer;
	private SongStatus songStatus;
	private DownloadAsyncTask downloadAsyncTask;
	private ArrayList<Integer> downloadedSongs;
	private ArrayList<Integer> playedSongs;
	private Boolean downloadingAll;

	private ListView songsLV;
	private ProgressBar progressBar;
	private Button backButton;
	private Button playButton;
	private Button nextButton;
	private TextView startText;
	private TextView endText;
	private ToggleButton randomTB;
	private ToggleButton repeatTB;
	
	private Boolean startPlaying;
		
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);

		startPlaying = true;	
					
		songStatus = SongStatus.INIT;
		mediaPlayer = null;
		downloadingAll = false;
		playedSongs = new ArrayList<Integer>();
		
		songsAA = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, JAudioPlayerApp.getSongNameList());

		SharedPreferences settings = JAudioPlayerApp.getAppContext().getSharedPreferences(getString(R.string.shared_preferences_file), 0);
		
		repeatTB = (ToggleButton) findViewById(R.id.repeatToggleButton);
		repeatTB.setChecked(settings.getBoolean(getString(R.string.repeat_toggle), false));
		repeatTB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	calculateSongsOrder();
        		playedSongs = new ArrayList<Integer>();

        		SharedPreferences settings = JAudioPlayerApp.getAppContext().getSharedPreferences(getString(R.string.shared_preferences_file), 0);
        	    SharedPreferences.Editor editor = settings.edit();
        	    editor.putBoolean(getString(R.string.repeat_toggle), repeatTB.isChecked());
        	    editor.commit();
            }
        });
		randomTB = (ToggleButton) findViewById(R.id.randomToggleButton);
		randomTB.setChecked(settings.getBoolean(getString(R.string.random_toggle), false));
		randomTB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	calculateSongsOrder();

        		SharedPreferences settings = JAudioPlayerApp.getAppContext().getSharedPreferences(getString(R.string.shared_preferences_file), 0);
        	    SharedPreferences.Editor editor = settings.edit();
        	    editor.putBoolean(getString(R.string.random_toggle), randomTB.isChecked());
        	    editor.commit();
            }
        });
		
		progressBar = (ProgressBar) findViewById(R.id.songProgress);
		progressBar.setProgress(0);		
		progressBar.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if((songStatus==SongStatus.PLAYING) || (songStatus==SongStatus.PAUSE)){
					progressBar.setProgress((int)(((double)arg1.getX()/(double)arg0.getWidth())*((int)((ProgressBar)arg0).getMax())));
					mediaPlayer.start();
					mediaPlayer.seekTo(progressBar.getProgress());
					if(songStatus==SongStatus.PAUSE){
						songStatus = SongStatus.PLAYING;
						customizePlayButton();
					}
					new RefreshSongTimeAsyncTask().execute();
				}
				return true;
			}
		});

		startText = (TextView) findViewById(R.id.startTimeTextView);
		endText = (TextView) findViewById(R.id.endTimeTextView);
		
		backButton = (Button) findViewById(R.id.backButton);
		backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	selectBackSong();
    			refreshListView();
            }
        });		
		playButton = (Button) findViewById(R.id.playButton);
		playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	playButtonAction();
            }
        });		
		nextButton = (Button) findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	selectNextSong();
    			refreshListView();
            }
        });	
				
		songsLV = (ListView) findViewById(R.id.listViewSongs);			
		songsLV.setAdapter(songsAA);
		songsLV.setOnItemClickListener(this);		
		songsLV.setOnScrollListener(new OnScrollListener() {
		    @Override
		    public void onScrollStateChanged(AbsListView view, int scrollState) { 
		    	refreshListView();
		    }
		    @Override
		    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		    	
		    }
		});
		
		selectedSong = calculateSongsOrder();
		moveToSelectedSong();
		refreshListView();
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	// Menu options
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if((downloadedSongs.size()<JAudioPlayerApp.getSongNameList().size())){
			String fileName = JAudioPlayerApp.getSongNameList().get(selectedSong);
			if(FileUtils.existAudio(fileName)){
				getMenuInflater().inflate(R.menu.remove_song_and_download_all_menu, menu);
			} else {
				getMenuInflater().inflate(R.menu.download_all_menu, menu);
			}
		} else {
			getMenuInflater().inflate(R.menu.remove_song_menu, menu);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.action_download_all:
	    	downloadAll();
	        return true;
	    case R.id.action_remove_download:
	    	removeDownload();
	        return true;
	    default: 
	    	return super.onOptionsItemSelected(item);  
	    }
	}

	// When user touch a song of list
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {		
		selectedSong = position;
		String fileName = JAudioPlayerApp.getSongNameList().get(selectedSong);

		invalidateOptionsMenu();
		
		// stop before downloading/playing song
		switch (songStatus) {
		case DOWNLOADING:
			downloadAsyncTask.closeStream();
			downloadAsyncTask.cancel(true);
			break;
		case PLAYING:
			if ((mediaPlayer != null) && (mediaPlayer.isPlaying())) {
				mediaPlayer.stop();
			}			
			break;
		default:
			break;
		}
			
		// Know if download of song is finished
		if(FileUtils.existAudio(fileName)){
			String finalFileName = FileUtils.getAppDir() + "/audio/" + fileName;
			Uri uri = Uri.fromFile(new File(finalFileName));
			mediaPlayer = MediaPlayer.create(this, uri);
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
	            @Override
	            public void onCompletion(MediaPlayer mp) {
	                selectNextSong();
	    			refreshListView();
	            }
	        });
			if ((songStatus == SongStatus.INIT) || (songStatus == SongStatus.DOWNLOADABLE)) {
				songStatus = (!startPlaying) ? SongStatus.PAUSE : SongStatus.PLAYING;
			}
			int duration = mediaPlayer.getDuration();
			progressBar.setMax(duration);
        	if(!(!repeatTB.isChecked() && (playedSongs.contains(selectedSong)))){
        		if(songStatus == SongStatus.PLAYING){
        			playSong();
        		}
        	}
		} else{
			mediaPlayer = null;
			songStatus = SongStatus.DOWNLOADABLE;
		}
		progressBar.setProgress(0);
		new RefreshSongTimeAsyncTask().execute(); 
		customizePlayButton();

		refreshListView();

	}
	
	// Refresh selected song in list
	public void refreshListView(){
		for ( int i=0; i < songsLV.getChildCount(); i++) {
			String textItem = ((TextView)songsLV.getChildAt(i).findViewById(android.R.id.text1)).getText().toString();
			if(textItem.compareTo(JAudioPlayerApp.getSongNameList().get(selectedSong))!=0){
				songsLV.getChildAt(i).setBackgroundColor(getResources().getColor(android.R.color.white));
				((TextView)songsLV.getChildAt(i).findViewById(android.R.id.text1)).setTextColor(getResources().getColor(android.R.color.black));
			} else {
				songsLV.getChildAt(i).setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
				((TextView)songsLV.getChildAt(i).findViewById(android.R.id.text1)).setTextColor(getResources().getColor(android.R.color.white));
			}
		}
	}
	
	// Go to new song
	public void moveToSelectedSong(){
		songsLV.performItemClick(songsLV.getAdapter().getView(0, null, null), selectedSong, songsLV.getAdapter().getItemId(selectedSong));
		songsLV.smoothScrollToPosition(selectedSong);
	}
	
	// Calculate order to playing list of songs according to replay and random toggle buttons
	public Integer calculateSongsOrder(){
		downloadedSongs = new ArrayList<Integer>();		
		for(int s=0; s<JAudioPlayerApp.getSongNameList().size(); s++){
			String fileName = JAudioPlayerApp.getSongNameList().get(s);
			if(FileUtils.existAudio(fileName)){
				downloadedSongs.add(s);
			}
		}		
		if(randomTB.isChecked()){
			Collections.shuffle(downloadedSongs);
		}		
		if(downloadedSongs.size() > 0){
			return downloadedSongs.get(0);			
		} else {
			return 0;
		}
	}
	
	// Change to next song
	public void selectNextSong(){
		Integer nextSong;
		switch (songStatus) {
		case PLAYING:
		case PAUSE:
			nextSong = downloadedSongs.indexOf(selectedSong)+1;
			if(nextSong==downloadedSongs.size()){
				selectedSong = downloadedSongs.get(0);
			} else {
				selectedSong = downloadedSongs.get(nextSong);
			}
			break;
		default:
			selectedSong = (selectedSong+1 == JAudioPlayerApp.getSongNameList().size()) ? 0 : selectedSong+1; 
			break;
		}
		moveToSelectedSong();
	}
	
	// Change to before song
	public void selectBackSong(){
		Integer backSong;
		switch (songStatus) {
		case PLAYING:
		case PAUSE:
			playedSongs.remove(selectedSong);
			backSong = downloadedSongs.indexOf(selectedSong)-1;
			if(backSong==-1){
				selectedSong = downloadedSongs.get(downloadedSongs.size()-1);
			} else {
				selectedSong = downloadedSongs.get(backSong);
			}
			playedSongs.remove(selectedSong);
			break;
		default:
			selectedSong = (selectedSong == 0) ? JAudioPlayerApp.getSongNameList().size()-1 : selectedSong-1; 
			break;
		}
		moveToSelectedSong();
	}
	
	// Download all unfinished download of songs
	public void downloadAll(){
		downloadingAll = true;
		if(existUndownloadedSong()){
			moveToSelectedSong();
			downloadAsyncTask = new DownloadAsyncTask();
			downloadAsyncTask.execute();
		} else {
			downloadingAll = false;
		}
	}
	
	public void removeDownload(){
		String fileName = JAudioPlayerApp.getSongNameList().get(selectedSong);
		mediaPlayer.stop();
		FileUtils.removeAudio(fileName);
		calculateSongsOrder();
		songStatus = SongStatus.DOWNLOADABLE;
		new RefreshSongTimeAsyncTask().execute();
		customizePlayButton();
		invalidateOptionsMenu();
	}
	
	// Know if all songs has been download
	public Boolean existUndownloadedSong(){
		for(int s=0; s<JAudioPlayerApp.getSongNameList().size(); s++){
			String fileName = JAudioPlayerApp.getSongNameList().get(s);
			if(!FileUtils.existAudio(fileName)){
				selectedSong = s;
				moveToSelectedSong();
    			refreshListView();
				return true;
			}
		}
		return false;
	}

	// Apply action according song and play button status
	public void playButtonAction(){
		switch (songStatus) {
		case DOWNLOADING:
			downloadAsyncTask.closeStream();
			downloadAsyncTask.cancel(true);
			break;
		case PLAYING:
			pauseSong();
			break;
		case DOWNLOADABLE:
			//download file
			downloadAsyncTask = new DownloadAsyncTask();
			downloadAsyncTask.execute();
			break;
		default:
			//play song
			playSong();
			break;
		}
	}
	
	// Change play button text
	public void customizePlayButton(){
		switch (songStatus) {
		case DOWNLOADING:
		case PLAYING:
			playButton.setText("pause");
			break;
		case DOWNLOADABLE:
			playButton.setText("download");
			break;
		default:
			playButton.setText("play");
			break;
		}
	}
		
	// PLAYER OPTIONS
	public void playSong(){
		mediaPlayer.start();
		songStatus = SongStatus.PLAYING;
		new RefreshSongTimeAsyncTask().execute();
		customizePlayButton();
		playedSongs.add(selectedSong);
	}	
	public void pauseSong(){
		mediaPlayer.pause();
		songStatus = SongStatus.PAUSE;
		customizePlayButton();
	}
	
	public String numberToString(Integer number){
		String numberString;
		
		if(number == -1){
			number = 0;
		}
		
		numberString = Integer.toString(number);
		while(numberString.length()<12){
			numberString = "0" + numberString;
		}
		return numberString;
	}

	
	// Refresh progress bar and text about times and percentages of download of the song
	public class RefreshSongTimeAsyncTask extends AsyncTask<Void, Integer, Void> {
		Integer totalBytes;
		int countSum;
		
		@Override
		protected void onPostExecute(Void result) {

		}

		@Override
		protected void onPreExecute() {
			String fileName = JAudioPlayerApp.getSongNameList().get(selectedSong);
			if(FileUtils.existAudio(fileName) && (mediaPlayer!=null)){
				int currentMillis = mediaPlayer.getCurrentPosition();
				int totalMillis = mediaPlayer.getDuration();
				int seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(currentMillis) % 60);
				if(seconds>9){
					startText.setText(TimeUnit.MILLISECONDS.toMinutes(currentMillis) + ":" + seconds);
				} else{
					startText.setText(TimeUnit.MILLISECONDS.toMinutes(currentMillis) + ":" + "0" + seconds);
				}
				seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(totalMillis) % 60);
				if(seconds>9){
					endText.setText(TimeUnit.MILLISECONDS.toMinutes(totalMillis) + ":" + seconds);
				} else{
					endText.setText(TimeUnit.MILLISECONDS.toMinutes(totalMillis) + ":" + "0" + seconds);
				}
				progressBar.setProgress(currentMillis);
				progressBar.setMax(totalMillis);
			} else{
				startText.setText("0%");
				endText.setText("100%");
				progressBar.setProgress(0);
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			while(songStatus == SongStatus.PLAYING){
        		int millis = mediaPlayer.getCurrentPosition();
        		publishProgress(millis);
        		SystemClock.sleep(1000);
        	}
			if(songStatus == SongStatus.DOWNLOADABLE){
				String fileName = JAudioPlayerApp.getSongNameList().get(selectedSong);
				totalBytes = FileUtils.existTempFileWithName(fileName);
				if(totalBytes>0){
					String tempFileName = FileUtils.getAppDir() + "/audio/" + "temp" + numberToString(totalBytes) + fileName;
					File f = new File(tempFileName);
					countSum = (int) f.length();
					progressBar.setMax(totalBytes);
					publishProgress(countSum);
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			switch (songStatus) {
			case PLAYING:
				int seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(values[0]) % 60);
				if(seconds>9){
					startText.setText(TimeUnit.MILLISECONDS.toMinutes(values[0]) + ":" + seconds);
				} else{
					startText.setText(TimeUnit.MILLISECONDS.toMinutes(values[0]) + ":" + "0" + seconds);
				}					
				progressBar.setProgress(values[0]);	
				break;
			case DOWNLOADABLE:
				startText.setText(values[0]/(progressBar.getMax()/100) + "%");
				progressBar.setProgress(values[0]);			
				break;
			default:
				break;
			}	
			refreshListView();
		}
	}
	
	
	// Download song asynchronously 
	public class DownloadAsyncTask extends AsyncTask<Void, Integer, Void> {
		int myProgress;
		FileOutputStream fos = null;
		int offset = 0;
		String tempFileName;
		String finalFileName;
		Integer totalBytes;

		@Override
		protected void onPostExecute(Void result) {
			startText.setText("100%");
        	calculateSongsOrder();
        	if(downloadingAll && (downloadedSongs.size()<JAudioPlayerApp.getSongNameList().size())){
        		downloadAll();
        	} else if(downloadingAll && (downloadedSongs.size()==JAudioPlayerApp.getSongNameList().size())){
				invalidateOptionsMenu();
        		songStatus = SongStatus.PLAYING;
				selectNextSong();
    			refreshListView();
        	} else {
        		invalidateOptionsMenu();
        		Uri uri = Uri.fromFile(new File(finalFileName));
    			mediaPlayer = MediaPlayer.create(JAudioPlayerApp.getAppContext(), uri);
        		songStatus = SongStatus.PLAYING;
    			playSong();
        	}
		}

		@Override
		protected void onPreExecute() {
			myProgress = 0;
			songStatus = SongStatus.DOWNLOADING;
			customizePlayButton();
		}

		@Override
		protected Void doInBackground(Void... params) {
			BufferedInputStream in;
			String fileURL = JAudioPlayerApp.getSongURLList().get(selectedSong);
			String fileName = JAudioPlayerApp.getSongNameList().get(selectedSong);
			if(JAudioPlayerApp.isOnline()){
				try {					
					URL url = new URL(fileURL);
		            URLConnection urlConnection = url.openConnection();					
					finalFileName = FileUtils.getAppDir() + "/audio/" + fileName;
					totalBytes = FileUtils.existTempFileWithName(fileName);
		            progressBar.setMax(totalBytes);
					if(totalBytes > 0){
						tempFileName = FileUtils.getAppDir() + "/audio/" + "temp" + numberToString(totalBytes) + fileName;
						fos = new FileOutputStream(tempFileName,true);
						File f = new File(tempFileName);
						offset = (int) f.length();
						publishProgress(offset);						
						urlConnection.setAllowUserInteraction(true);
						// Specify what portion of file to download.
						urlConnection.setRequestProperty("Range", "bytes=" + offset + "-" + totalBytes.toString());
			            urlConnection.setDoInput(true);
			            urlConnection.setDoOutput(true);
			            urlConnection.connect();	
					} else{
			            urlConnection.connect();
						totalBytes = urlConnection.getContentLength();
			            progressBar.setMax(totalBytes);
						tempFileName = FileUtils.getAppDir() + "/audio/" + "temp" + numberToString(totalBytes) + fileName;
						fos = new FileOutputStream(tempFileName);
					}
		            in = new BufferedInputStream(urlConnection.getInputStream());
					byte data[] = new byte[1024];
					int count;
					int countSum = offset;					
					while ((count = in.read(data, 0, 1024)) != -1) {
						fos.write(data, 0, count);
						countSum += count;
						publishProgress(countSum);
					}					
					File tempFile = new File(tempFileName);
					File finalFile = new File(finalFileName);
					tempFile.renameTo(finalFile);
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}			
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(mediaPlayer == null){
				progressBar.setProgress(values[0]);
				startText.setText(values[0]/(totalBytes/100)+"%");	
			}			
		}
		
		public void closeStream(){
			if (fos != null){
				try {
					fos.close();
					songStatus = SongStatus.DOWNLOADABLE;
					customizePlayButton();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}	
}
