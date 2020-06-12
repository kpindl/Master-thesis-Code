package midas_server_v2.datafetcher;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.Criterion;
import midas_server_v2.repository.CriterionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CriterionDataFetcher {

    @Autowired
    private CriterionRepository criterionRepository;

    @GraphQLQuery(name = "criteria")
    public List<Criterion> getAllCriteria() {
        return criterionRepository.findAll();
    }
}
