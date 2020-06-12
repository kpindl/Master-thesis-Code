package midas_server_v2;

import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import midas_server_v2.datafetcher.*;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.IOException;


@Component
public class GraphQLProvider {

    // region datafetcher
    @Autowired
    private CategoryDataFetcher categoryDataFetcher;

    @Autowired
    private ClassificationDataFetcher classificationDataFetcher;

    @Autowired
    private CNVDataFetcher cnvDataFetcher;

    @Autowired
    private CriterionChoiceDataFetcher criterionChoiceDataFetcher;

    @Autowired
    private CriterionDataFetcher criterionDataFetcher;

    @Autowired
    private DepartmentDataFetcher departmentDataFetcher;

    @Autowired
    private PanelDataFetcher panelDataFetcher;

    @Autowired
    private RunDataFetcher runDataFetcher;

    @Autowired
    private SampleDataFetcher sampleDataFetcher;

    @Autowired
    private TargetDataFetcher targetDataFetcher;

    @Autowired
    private TranscriptDataFetcher transcriptDataFetcher;
    //endregion

    private GraphQL graphQL;

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

    @PostConstruct
    public void init() throws IOException {
        GraphQLSchema graphQLSchema = buildSchema();
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private GraphQLSchema buildSchema() {
        return new GraphQLSchemaGenerator()
                .withResolverBuilders(new AnnotatedResolverBuilder())
                .withOperationsFromSingleton(categoryDataFetcher)
                .withOperationsFromSingleton(classificationDataFetcher)
                .withOperationsFromSingleton(cnvDataFetcher)
                .withOperationsFromSingleton(criterionChoiceDataFetcher)
                .withOperationsFromSingleton(criterionDataFetcher)
                .withOperationsFromSingleton(departmentDataFetcher)
                .withOperationsFromSingleton(panelDataFetcher)
                .withOperationsFromSingleton(runDataFetcher)
                .withOperationsFromSingleton(sampleDataFetcher)
                .withOperationsFromSingleton(targetDataFetcher)
                .withOperationsFromSingleton(transcriptDataFetcher)
                .withValueMapperFactory(new JacksonValueMapperFactory())
                .generate();
    }
}