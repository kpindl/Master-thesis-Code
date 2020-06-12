package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SampleDataFetcher {

    @Autowired
    private SampleRepository sampleRepository;

    @GraphQLQuery(name = "numberSamplesPerEnrvers")
    public Integer getNumberSamplesPerEnrvers(@GraphQLArgument(name = "id_enrvers") int id_enrvers) {
        return sampleRepository.findByIdEnrvers(id_enrvers).size();
    }
}
