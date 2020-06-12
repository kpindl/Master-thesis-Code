package variantdb.models;

import io.aexp.nodes.graphql.annotations.GraphQLIgnore;
import lombok.*;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Target extends CNVTableRecord {

    private Integer idSat;
    private Integer idCnv;
    private Double coverage;
    private List<String> caller;
    private Region region;
    private List<Transcript> transcripts;

    @GraphQLIgnore
    private Integer numSamplesIH;

    @GraphQLIgnore
    private Double frequencyIH;


    // region getter
    @Override
    public Integer getStart() {
        return region.getG_start();
    }

    @Override
    public Integer getEnd() {
        return region.getG_end();
    }

    @Override
    public Double getCoverage() {
        return coverage;
    }

    @Override
    public String getCaller() {
        StringJoiner joiner = new StringJoiner(", ");
        caller.forEach(c -> joiner.add(c));
        return joiner.toString();
    }

    @Override
    public Integer getNumCaller() {return caller.size(); }

    @Override
    public String getTranscriptsAsString() {
        StringJoiner joiner = new StringJoiner(",\n");
        transcripts.forEach(t -> joiner.add(t.getName()));
        return joiner.toString();
    }

    @Override
    public List<Transcript> getTranscripts() { return transcripts; }

    @Override
    public Integer getNumSamples() {
        return numSamplesIH;
    }

    @Override
    public Double getFrequency() {
        return Math.round(frequencyIH*100.0)/100.0;
    }



    //endregion


    // region inner classes
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Gene {
        private Integer id_reg;
        private String name;


        @Override
        public int hashCode() {
            return Objects.hash(id_reg, name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Gene)) {
                return false;
            }

            Gene gene = (Gene) obj;
            return Objects.equals(this.id_reg, gene.id_reg) &&
                    Objects.equals(this.name, gene.name);
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Region {

        private int idReg;
        private String identifier;
        private int g_start;
        private int g_end;
    }
    //endregion
}
