package variantdb.models;

import java.util.List;
import java.util.StringJoiner;
import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLIgnore;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@EqualsAndHashCode
@GraphQLProperty(name = "cnvs", arguments = @GraphQLArgument(name = "id_sam"))
public class CNV extends CNVTableRecord {

    private Integer idCnv;
    private Integer idSam;

    @GraphQLIgnore
    private String genes;
    private String identifier;
    private String approved;
    private String type;
    private String method;
    private Integer minSize;
    private Integer maxSize;
    private List<CNVClassification> classifications;
    private String hgvs;
    private String iscn;
    private List<Target> targets;


    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getGenes() {
        return genes;
    }

    @Override
    public String getHgvsNomenclature() {
        return hgvs;
    }

    @Override
    public String getIscnNomenclature() {
        return iscn;
    }

    @Override
    public Integer getMinSize() {
        return minSize;
    }

    @Override
    public Integer getMaxSize() {
        return maxSize;
    }

    @Override
    public List<CNVClassification> getClassifications() {
        return classifications;
    }

    @Override
    public String getRecentClassificationName() {
        if (this.classifications.isEmpty()) {
            return "";
        }

        return this.classifications.get(this.classifications.size()-1).getName();
    }

    @Override
    public String getStatusApproved() {
        return approved;
    }

    @Override
    public String getMethodApproved() {
        return method;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public List<Target> getTargets() {
        return targets;
    }

    @Override
    public void setNomenclature(String guideline, String nomenclature){
        if (guideline.equals("HGVS")){
            this.hgvs = nomenclature;
            return;
        }

        if(guideline.equals("ISCN")) {
            this.iscn = nomenclature;
        }
    }

    @Override
    public void addClassification(CNVClassification classification) {
        this.classifications.add(classification);
    }


    public String getGenesFromTargets(){
        StringJoiner joiner = new StringJoiner(", ");
        this.targets.forEach(ta -> ta.getTranscripts().forEach(tr -> {
            if(!joiner.toString().contains(tr.getGene())){
                joiner.add(tr.getGene());
            }}));

        return joiner.toString();
    }
}
