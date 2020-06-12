package midas_server_v2.model.tables;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Table(name = "region", schema = "ngs")
@Getter
@Setter
public class Region {

    @Id
    @Column(name = "id_reg")
    private int idReg;

    @Column(name = "id_typ")
    private int idTyp;
    private String identifier;
    private int g_start;
    private int g_end;
}
