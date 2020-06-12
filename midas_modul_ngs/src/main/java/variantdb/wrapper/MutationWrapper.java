package variantdb.wrapper;

import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;
import lombok.Getter;
import lombok.Setter;
import java.net.MalformedURLException;


@Getter
@Setter
public class MutationWrapper extends Wrapper {

    private Boolean updateCNVByNomenclature;
    private InsertClassification insertCNVClassification;
    private InsertCriterion insertCNVClassificationCriterion;
    private InsertAttachment insertCNVAttachment;

    public static MutationWrapper getMutationWrapper(Class<?> clazz, Arguments arguments) {
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

        GraphQLResponseEntity<MutationWrapper> responseEntity = graphQLTemplate.mutate(requestEntity, MutationWrapper.class);
        return responseEntity.getResponse();
    }


    // region inner classes
    @GraphQLProperty(name = "updateCNVByNomenclature", arguments = {@GraphQLArgument(name = "id_cnv"),
            @GraphQLArgument(name = "hgvs"),
            @GraphQLArgument(name = "iscn")})
    public class UpdateCNV {}

    @Setter @Getter
    @GraphQLProperty(name = "insertCNVClassification", arguments = @GraphQLArgument(name = "classification"))
    public class InsertClassification {
        private Integer idCcl;
    }

    @Setter @Getter
    @GraphQLProperty(name = "insertCNVClassificationCriterion", arguments = @GraphQLArgument(name = "criterion"))
    public class InsertCriterion {
        private Integer idCcc;
    }

    @Setter @Getter
    @GraphQLProperty(name = "insertCNVAttachment", arguments = @GraphQLArgument(name = "attachment"))
    public class InsertAttachment {
        private Integer idCat;
    }

    //endregion

}
