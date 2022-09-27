import java.util.Comparator;

import com.example.project_ds_2021.VideoFile;

public class VideoFileSorter implements Comparator<VideoFile>
{
    @Override
    public int compare(VideoFile vf1, VideoFile vf2) {
        return (Integer.parseInt(vf1.getName().substring(0,vf1.getName().indexOf("_")))) - (Integer.parseInt(vf2.getName().substring(0,vf2.getName().indexOf("_"))));
    }
}
