package variantdb.models;

public class Nomenclature {

    private String guideline;
    private String nomenclature;

    // Constructor
    public Nomenclature(String guideline, String nomenclature) {
        this.guideline = guideline;
        this.nomenclature = nomenclature;
    }

    // region getter and setter
    public String getGuideline() {
        return guideline;
    }

    public String getNomenclature() {
        return nomenclature;
    }
    // endregion
}
