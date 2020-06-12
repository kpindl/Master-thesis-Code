package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.Run;
import midas_server_v2.repository.RunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class RunDataFetcher {
    private static Logger LOGGER = Logger.getLogger(RunDataFetcher.class.getName());

    @Autowired
    private RunRepository runRepository;


    @GraphQLQuery(name = "runByIlluminaRunName")
    public List<Run> getRun(@GraphQLArgument(name = "illuminarunname") String illuminarunname) {
        return runRepository.findByIlluminarunname(illuminarunname);
    }


    @GraphQLQuery(name = "runById")
    public Optional<Run> getRunById(@GraphQLArgument(name = "id_run") Integer id_run) {
        return runRepository.findById(id_run);
    }
}
