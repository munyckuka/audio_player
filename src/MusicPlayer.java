import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.*;
import java.rmi.server.ExportException;
import java.util.ArrayList;

public class MusicPlayer extends PlaybackListener {
    private static final Object playSignal = new Object(); // to sync slider and music
    private MusicPlayerGUI musicPlayerGUI;
    private Song currentSong;
    private ArrayList<Song> playlist;
    private int currentPlaylistIndex;
    private AdvancedPlayer advancedPlayer;
    private boolean isPaused;
    private boolean songFinished;
    private boolean pressedNext;
    private boolean pressedPrev;
    private int currentFrame; //time when music is stopped
    private int seconds =0;
    private int minutes = 0;



    public void setCurrentFrame(int frame){
        currentFrame = frame;
    }

    public void setCurrentTimeInMilli(int timeInMilli){
        currentTimeInMilli = timeInMilli;
    }
    public Song getCurrentSong(){
        return currentSong;
    }
    public void loadPlaylist(File playlistFile){
        playlist = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(playlistFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String songPath;
            while ((songPath = bufferedReader.readLine()) != null){
                Song song = new Song(songPath);
                playlist.add(song);
            }

            if (playlist.size()>0){
                musicPlayerGUI.setPlaybackSliderValue(0);
                currentTimeInMilli = 0;
                currentSong = playlist.get(0);
                currentFrame = 0;

                musicPlayerGUI.enablePauseButtonDissablePlayButton();
                musicPlayerGUI.updateSongTitleAndArtist(currentSong);
                musicPlayerGUI.updatePlaybackSlider(currentSong);

                playCurrentSong();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public void nextSong(){
        if(playlist==null)return;
        if (currentPlaylistIndex+1 > playlist.size()-1)return;
        if (!songFinished)stopSong();
        pressedNext = true;
        currentPlaylistIndex++;
        currentSong  = playlist.get(currentPlaylistIndex);
        currentFrame=0;
        currentTimeInMilli=0;
        musicPlayerGUI.enablePauseButtonDissablePlayButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlaybackSlider(currentSong);
        playCurrentSong();
    }
    public void prevSong(){
        if(playlist==null)return;
        if (currentPlaylistIndex-1 < 0)return;

        if (!songFinished)stopSong();
        pressedPrev = true;
        currentPlaylistIndex--;
        currentSong  = playlist.get(currentPlaylistIndex);
        currentFrame=0;
        currentTimeInMilli=0;
        musicPlayerGUI.enablePauseButtonDissablePlayButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlaybackSlider(currentSong);
        playCurrentSong();
    }

    private int currentTimeInMilli;
    public MusicPlayer(MusicPlayerGUI musicPlayerGUI){
        this.musicPlayerGUI = musicPlayerGUI;
    }

    //  load song and insta play it
    public void loadSong(Song song){
        currentSong = song;
        playlist = null;
        if (!songFinished) stopSong();
        if(currentSong != null){
            currentFrame = 0;
            currentTimeInMilli =0;
            musicPlayerGUI.setPlaybackSliderValue(0);
            playCurrentSong();
        }
    }

//    pause
    public void pauseSong(){
        if(advancedPlayer != null){
            isPaused = true;
            stopSong();
        }
    }
    public void stopSong(){
        if(advancedPlayer != null){
            advancedPlayer.stop();
            advancedPlayer.close();
            advancedPlayer = null;
        }
    }

//   creating player to play music
    public void playCurrentSong() {
        if(currentSong == null) return; //prevent bug. if no song is loaded exception accures
        try {
            FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            advancedPlayer = new AdvancedPlayer(bufferedInputStream);
            advancedPlayer.setPlayBackListener(this);

            startMusicThread();

            startPlaybackSliderTread();

        }catch (Exception e){
            e.printStackTrace();
        }
    }
//  thread to play/resume music
    private void startMusicThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(isPaused){
                        synchronized (playSignal){
                            isPaused = false;

                            playSignal.notify();
                        }
                        advancedPlayer.play(currentFrame, Integer.MAX_VALUE);
                    }else {
//                        play from the beginning
                        advancedPlayer.play();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startPlaybackSliderTread(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isPaused){
                    try {
                        synchronized (playSignal){
                            playSignal.wait();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                while (!isPaused && !songFinished && !pressedNext && !pressedPrev){
                    try {
                        currentTimeInMilli++;
//                        formula taken from stackoverflow
                        int calculatedFrame = (int)((double)currentTimeInMilli *2.08* currentSong.getFrameRatePerMillisecond());

                        musicPlayerGUI.setPlaybackSliderValue(calculatedFrame);

                        if (currentTimeInMilli % 1000 == 0){
                            seconds++;
                            if (seconds == 60) {
                                seconds = 0;
                                minutes++;
                            }
                        }
                        String newtime = String.format("%02d:%02d", minutes, seconds);
                        System.out.println(currentTimeInMilli);
                        musicPlayerGUI.setCurrentSongTime(newtime);
                        Thread.sleep(1);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void playbackStarted(PlaybackEvent evt) {
        System.out.println("Starting music player");
        songFinished = false;
    }

    @Override
    public void playbackFinished(PlaybackEvent evt) {
        System.out.println("stopped at @" + evt.getFrame());
        if (isPaused) {
            currentFrame += (int) ((double) evt.getFrame() * currentSong.getFrameRatePerMillisecond()); //calculating current frame
        } else { //when song ends
//            if pressed next. song is not finished. and other code nod needed to execute
            if (pressedNext || pressedPrev)return;
            songFinished = true;
            if (playlist==null){
                musicPlayerGUI.enablePlayButtonDissablePauseButton();
            }else {
                if (currentPlaylistIndex==playlist.size()-1){ //if last song in playlist
                    musicPlayerGUI.enablePlayButtonDissablePauseButton();
                }else { // if its not last song, play next
                    nextSong();
                }
            }
        }
    }


}


/*
mm:ss
long minutes = mp3File.getLengthInSeconds() / 60;
long seconds = mp3File.getLengthInSeconds() % 60;
seconds + 1
if seconds == 60;
m++; second =0;
 */