package variantdb.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode
public class Transcript {

    private int idTra;
    private String name;
    private String identifier;
    private Integer g_start;
    private Integer g_end;
    private String gene;
}
