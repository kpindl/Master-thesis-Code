package variantdb.models;

import io.aexp.nodes.graphql.annotations.GraphQLProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@GraphQLProperty(name = "criteria")
public class Criterion {

    private Integer idCri;
    private String shortid;
    private String description;
}
