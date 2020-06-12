package variantdb.models;

import lombok.NoArgsConstructor;
import variantdb.models.CNVClassification;
import variantdb.models.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
public class CNVTableRecord {

    //region getter
    public String getType() {
        return "";
    }

    public String getIdentifier() {
        return "";
    }

    public String getGenes() {
        return "";
    }

    public String getHgvsNomenclature() {
        return "";
    }

    public String getIscnNomenclature() {
        return "";
    }

    public Integer getMinSize() {
        return null;
    }

    public Integer getMaxSize() {
        return null;
    }

    public List<CNVClassification> getClassifications() { return null; }

    public String getRecentClassificationName() {
        return "";
    }

    public String getStatusApproved() {
        return "";
    }

    public String getMethodApproved() {
        return "";
    }

    public List<Target> getTargets() {
        return new ArrayList<>();
    }

    public Integer getStart() {
        return null;
    }

    public Integer getEnd() {
        return null;
    }

    public Double getCoverage() {
        return null;
    }

    public String getCaller() {return null;}

    public Integer getNumCaller() {return null;}

    public Double getFrequency() {
        return null;
    }

    public Integer getNumSamples() {
        return null;
    }

    public String getTranscriptsAsString() {
        return "";
    }

    public List<Transcript> getTranscripts() { return new ArrayList<>(); }
    //endregion

    // region setter
    public void setNomenclature(String nomenclature, String guideline){}

    public void addClassification(CNVClassification classification){}
    // endregion
}

