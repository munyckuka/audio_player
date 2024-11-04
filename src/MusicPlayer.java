import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.rmi.server.ExportException;

public class MusicPlayer extends PlaybackListener {
    private Song currentSong;

    private AdvancedPlayer advancedPlayer;
    private boolean isPaused;
    private int currentFrame; //time when music is stopped
    public MusicPlayer(){

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
//                        resume song
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
                while ()
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
