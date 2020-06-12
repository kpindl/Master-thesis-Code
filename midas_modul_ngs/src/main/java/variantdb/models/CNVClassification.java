package variantdb.models;

import common.session.user.SessionUser;
import io.aexp.nodes.graphql.InputObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class CNVClassification {

    private Integer idCnv;
    private Integer idCla;
    private Integer idUsr;
    private String name;
    private Boolean draft;
    private LocalDateTime createtime;
    private String evaluationtext;
    private String comment;
    private String report_ref;
    private List<CNVClassificationCriterion> criteria;

    /**
     * No arguments constructor
     */
    public CNVClassification() {
        this.criteria = new ArrayList<>();
    }


    /**
     * Creates a GraphQL InputObject
     *
     * @return
     */
    public InputObject toInputObject() {
        return new InputObject.Builder<>()
                .put("idCnv", this.idCnv)
                .put("idCla", this.idCla)
                .put("idUsr", SessionUser.getInstance().getActualUser().getId())
                .put("draft", this.draft)
                .put("createtime", LocalDateTime.now().toString())
                .put("evaluationtext", this.evaluationtext)
                .put("comment", this.comment)
                .put("report_ref", this.report_ref)
                .build();
    }


    public String printClassification(){
        StringBuilder s = new StringBuilder("Classification: " + name + "\n");
        for (CNVClassificationCriterion criterion : criteria) {
            s.append(criterion.printCriterion());
        }
        s.append("\n--------------");
        return s.toString();
    }


    // region inner class
    @Setter
    @Getter
    public static class CNVClassificationCriterion {

        private Integer idCcl;
        private Integer idCri;
        private String shortid;
        private Integer idCrs;
        private String comment;
        private String value;

        /**
         * Return formated string to print criterion
         *
         * @return
         */
        private String printCriterion() {
            return "\tShortId: " + shortid
                    + "\n\tid_cri: " + idCri
                    + "\n\tComment: " + comment
                    + "\n\tValue: " + value
                    + "\n\tid_crs: " + idCrs + "\n\n";
        }


        /**
         * Creates a GraphQL InputObject
         */
        public InputObject toInputObject() {
            return new InputObject.Builder<>()
                    .put("idCri", this.idCri)
                    .put("shortid", this.shortid)
                    .put("idCrs", this.idCrs)
                    .put("comment", this.comment)
                    .put("value", this.value)
                    .build();
        }
    }
    //endregion
}
