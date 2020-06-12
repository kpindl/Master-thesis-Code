package variantdb.wrapper;

import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;

public abstract class Wrapper {

    static final String URL = "http://localhost:8080/graphql";
    static GraphQLTemplate graphQLTemplate = new GraphQLTemplate();
    static GraphQLRequestEntity requestEntity;
}
