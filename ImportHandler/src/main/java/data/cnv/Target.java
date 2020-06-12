package data.cnv;

public class Target implements Comparable<Target> {

    private int id_rrf;
    private Integer id_cal;
    private String caller;
    private String identifier;
    private Integer g_start;
    private Integer g_end;
    private Integer evidencegroup;
    private String type;

    // region constructor
    /**
     * Default constructor
     */
    public Target() {}

    /**
     * All arguments constructor
     *
     * @param id_rrf
     * @param id_cal
     * @param caller
     * @param identifier
     * @param g_start
     * @param g_end
     * @param evidencegroup
     * @param type
     */
    public Target(Integer id_rrf, Integer id_cal, String caller, String identifier, Integer g_start, Integer g_end, Integer evidencegroup, String type) {
        this.id_rrf = id_rrf;
        this.id_cal = id_cal;
        this.caller = caller;
        this.identifier = identifier;
        this.g_start = g_start;
        this.g_end = g_end;
        this.evidencegroup = evidencegroup;
        this.type = type;
    }

    /**
     * Copy constructor
     *
     * @param refTarget
     */
    public Target(Target refTarget) {
        this.id_rrf = refTarget.getId_rrf();
        this.id_cal = refTarget.getId_cal();
        this.caller = refTarget.getCaller();
        this.identifier = refTarget.getIdentifier();
        this.g_start = refTarget.getG_start();
        this.g_end = refTarget.getG_end();
        this.evidencegroup = refTarget.getEvidencegroup();
    }
    // endregion

    // region getter and setter
    public int getId_rrf() {
        return id_rrf;
    }

    public Integer getId_cal() {
        return id_cal;
    }

    public String getCaller() {
        return caller;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Integer getG_start() {
        return g_start;
    }

    public Integer getG_end() {
        return g_end;
    }

    public Integer getEvidencegroup() {
        return evidencegroup;
    }

    public String getType() {
        return type;
    }

    public void setId_rrf(Integer id_rrf) {
        this.id_rrf = id_rrf;
    }

    public void setId_cal(Integer id_cal) {
        this.id_cal = id_cal;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setG_start(Integer g_start) {
        this.g_start = g_start;
    }

    public void setG_end(Integer g_end) {
        this.g_end = g_end;
    }

    public void setEvidencegroup(Integer evidencegroup) {
        this.evidencegroup = evidencegroup;
    }

    public void setType(String type) {
        this.type = type;
    }

    // endregion

    @Override
    public int compareTo(Target target) {

        // sorting order: chromosome, start, stop, type
        int result = this.identifier.compareTo(target.identifier);

        if(result == 0) {
            result = this.g_start - target.g_start;
        }

        if(result == 0) {
            result = this.g_end - target.g_end;
        }

        if(result == 0) {
            result = this.type.compareTo(target.type);
        }

        return result;
    }
}
