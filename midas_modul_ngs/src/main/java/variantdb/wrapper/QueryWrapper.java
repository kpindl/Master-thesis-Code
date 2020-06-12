package variantdb.wrapper;

import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;
import lombok.Getter;
import lombok.Setter;
import variantdb.models.CNV;
import variantdb.models.Classification;
import variantdb.models.Criterion;
import variantdb.models.CriterionChoice;

import java.net.MalformedURLException;
import java.util.List;

@Getter
@Setter
public class QueryWrapper extends Wrapper {

    private List<Criterion> criteria;
    private List<CNV> cnvs;
    private List<CriterionChoice> criterionChoices;
    private List<Classification> classifications;
    private Boolean isTranscriptInSampleInv;
    private Integer numberSamplesPerTarget;
    private Integer numberSamplesPerEnrvers;


    public static QueryWrapper getQueryWrapper(Class<?> clazz, Arguments arguments) {
        try {
            requestEntity = GraphQLRequestEntity.Builder()
                    .url(URL)
                    .request(clazz)
                    .arguments(arguments)
                    .build();
            System.out.println(requestEntity.getRequest());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        GraphQLResponseEntity<QueryWrapper> responseEntity = graphQLTemplate.query(requestEntity, QueryWrapper.class);
        return responseEntity.getResponse();
    }

    public static QueryWrapper getQueryWrapper(Class<?> clazz) {
        try {
            requestEntity = GraphQLRequestEntity.Builder()
                    .url(URL)
                    .request(clazz)
                    .build();
            System.out.println(requestEntity.getRequest());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        GraphQLResponseEntity<QueryWrapper> responseEntity = graphQLTemplate.query(requestEntity, QueryWrapper.class);
        return responseEntity.getResponse();
    }
}

