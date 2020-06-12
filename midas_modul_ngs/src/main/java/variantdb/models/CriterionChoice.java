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
@GraphQLProperty(name = "criterionChoices")
public class CriterionChoice {

    private Integer idCrs;
    private String dropdownchoice;

    @Override
    public String toString(){
        return this.dropdownchoice;
    }
}
