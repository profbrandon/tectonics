
import java.util.List;
import java.util.ArrayList;



public class Plate {
    
    private List<Region> mRegions;

    public Plate() {
        mRegions = new ArrayList<>();
    }

    public Plate(final Region region) {
        mRegions = new ArrayList<>();
        mRegions.add(region);
    }

    public Plate(final List<Region> regions) {
        mRegions = regions;
    }

    public List<Region> getRegions() {
        return mRegions;
    }
}
