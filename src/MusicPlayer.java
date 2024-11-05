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
    private AdvancedPlayer advancedPlayer;
    private boolean isPaused;
    private int currentFrame; //time when music is stopped


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


    private int currentTimeInMilli;
    public MusicPlayer(MusicPlayerGUI musicPlayerGUI){
        this.musicPlayerGUI = musicPlayerGUI;
    }

    //  load song and insta play it
    public void loadSong(Song song){
        currentSong = song;
        if(currentSong != null){
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
                while (!isPaused){
                    try {
                        currentTimeInMilli++;
//                        formula taken from stackoverflow
                        int calculatedFrame = (int)((double)currentTimeInMilli *2.08* currentSong.getFrameRatePerMillisecond());

                        musicPlayerGUI.setPlaybackSliderValue(calculatedFrame);
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
    }

    @Override
    public void playbackFinished(PlaybackEvent evt) {
        System.out.println("stopped at @" + evt.getFrame());

        currentFrame += (int) ((double) evt.getFrame() * currentSong.getFrameRatePerMillisecond()); //calculating current frame
    }


}
