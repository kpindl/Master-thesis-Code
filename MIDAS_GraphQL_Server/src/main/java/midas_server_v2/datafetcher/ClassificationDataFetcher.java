package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.Classification;
import midas_server_v2.repository.ClassificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClassificationDataFetcher {

    @Autowired
    ClassificationRepository classificationRepository;

    @GraphQLQuery(name = "classifications")
    public List<Classification> getAllClassifications() {
        return classificationRepository.findAll();
    }
}
