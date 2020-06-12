package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.CriterionChoice;
import midas_server_v2.repository.CriterionChoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CriterionChoiceDataFetcher {

    @Autowired
    CriterionChoiceRepository criterionChoiceRepository;

    @GraphQLQuery(name = "criterionChoices")
    public List<CriterionChoice> getAllCriterionChoices() {
        return criterionChoiceRepository.findAll();
    }
}
