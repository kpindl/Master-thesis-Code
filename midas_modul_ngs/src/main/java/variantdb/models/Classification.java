package variantdb.models;

import io.aexp.nodes.graphql.annotations.GraphQLProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@GraphQLProperty(name = "classifications")
public class Classification {

    private Integer idCla;
    private String name;
    private String color;

    @Override
    public String toString(){
        return this.name;
    }
}
