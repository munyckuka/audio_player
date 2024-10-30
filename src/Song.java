import com.mpatric.mp3agic.Mp3File;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

public class Song{
    private String songTitle;
    private String songArtist;
    private String songLength; //NOTE! add constructor and add to gui
    private String filePath;
    private Mp3File mp3File;
    private double frameRatePerMillisecond;

    public Song(String filePath){
        this.filePath = filePath;
        try {
            mp3File = new Mp3File(filePath);
            frameRatePerMillisecond = (double) mp3File.getFrameCount() / mp3File.getLengthInMilliseconds(); //for calculating current frame
            AudioFile audioFile = AudioFileIO.read(new File(filePath));
//            getting title and artist
            Tag tag = audioFile.getTag();
            if(tag != null){
                songTitle = tag.getFirst(FieldKey.TITLE);
                songArtist = tag.getFirst(FieldKey.ARTIST);
            }else {
                songTitle = "N/A";
                songArtist = "N/A";
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

//     getters

    public String getSongTitle() {
        return songTitle;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public String getSongLength() {
        return songLength;
    }

    public String getFilePath() {
        return filePath;
    }
    public Mp3File getMp3File(){
        return mp3File;
    }
    public double getFrameRatePerMillisecond(){
        return frameRatePerMillisecond;
    }
}
