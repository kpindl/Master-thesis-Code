package data.cnv;

import database.MIDAS_DB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CNV {

    private Integer id_sam;
    private Integer id_reg;
    private String identifier;
    private Integer min_size;
    private Integer max_size;
    private String type;
    private List<Target> targets;

    // Constructor
    public CNV() {
        this.targets = new ArrayList<>();
    }

    // region getter and setter
    public Integer getId_sam() {
        return id_sam;
    }

    public Integer getId_reg() {
        return id_reg;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Integer getMin_size() {
        return min_size;
    }

    public Integer getMax_size() {
        return max_size;
    }

    public String getType() {
        return type;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void setId_sam(Integer id_sam) {
        this.id_sam = id_sam;
    }

    public void setId_reg(Integer id_reg) {
        this.id_reg = id_reg;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setMin_size(Integer min_size) {
        this.min_size = min_size;
    }

    public void setMax_size(Integer max_size) {
        this.max_size = max_size;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }
    // endregion


    /**
     * Determine minimum cnv size based on first and last target
     */
    public void determineMinSize() {
        Collections.sort(targets);
        this.min_size = targets.get(targets.size() - 1).getG_end() - targets.get(0).getG_start();
    }

    /**
     * Determine maximum cnv size based on targets an
     */
    public void determineMaxSize(int id_enrvers, MIDAS_DB database) {
        Optional<Integer> lowerLimit = database.getG_EndOfPrevTarget(targets.get(0), id_enrvers);
        Optional<Integer> upperLimit = database.getG_StartOfNextTarget(targets.get(targets.size() - 1), id_enrvers);

        if(!lowerLimit.isPresent() || !upperLimit.isPresent()) return;

        this.max_size = upperLimit.get() - lowerLimit.get();
    }
}
