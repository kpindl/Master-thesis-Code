package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.CNV;
import midas_server_v2.model.tables.CNVClassification;
import midas_server_v2.model.tables.CNVClassificationCriterion;
import midas_server_v2.model.tables.Target;
import midas_server_v2.repository.CNVClassificationCriterionRepository;
import midas_server_v2.repository.CNVClassificationRepository;
import midas_server_v2.repository.CNVRepository;
import midas_server_v2.repository.TargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CNVDataFetcher {


    // region repositories
    @Autowired
    private CNVRepository cnvRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private CNVClassificationRepository classificationRepository;

    @Autowired
    private CNVClassificationCriterionRepository criterionRepository;
    // endregion



    // region graphql queries
    @GraphQLQuery(name = "targets")
    public List<Target> getTargets(@GraphQLContext CNV cnv) {
        List<Target> targets = targetRepository.findByIdCnv(cnv.getIdCnv());
        targets.forEach(t -> t.setIdSam(cnv.getIdSam()));
        return targets;
    }


    @GraphQLQuery
    public List<CNVClassification> classifications(@GraphQLContext CNV cnv) {
        return classificationRepository.findByIdCnvOrderByCreatetimeDesc(cnv.getIdCnv());
    }


    @GraphQLQuery(name = "cnvs")
    public List<CNV> getCNVBySampleId(@GraphQLArgument(name = "id_sam") Integer id_sam) {
        return cnvRepository.findBySampleId(id_sam);
    }
    // endregion


    // region graphlql mutations
    @GraphQLMutation(name = "updateCNVByNomenclature")
    public void updateCNVByNomenclature(@GraphQLArgument(name = "id_cnv") Integer id_cnv,
                                        @GraphQLArgument(name = "hgvs") String hgvs,
                                        @GraphQLArgument(name = "iscn") String iscn){
        cnvRepository.updateNomenclature(id_cnv, hgvs, iscn);
    }

    @GraphQLMutation(name = "insertCNVClassification")
    public CNVClassification insertCNVClassification(@GraphQLArgument(name = "classification") CNVClassification classification) {
        return classificationRepository.save(classification);
    }


    @GraphQLMutation(name = "insertCNVClassificationCriterion")
    public CNVClassificationCriterion insertCNVClassificationCriterion(@GraphQLArgument(name = "criterion") CNVClassificationCriterion criterion) {
        return criterionRepository.save(criterion);
    }
    // endregion

}
