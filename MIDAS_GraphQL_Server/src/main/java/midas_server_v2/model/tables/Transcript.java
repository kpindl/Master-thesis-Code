package midas_server_v2.model.tables;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "transcript", schema = "ngs")
@Getter @Setter @NoArgsConstructor
public class Transcript {

    @Id
    @Column(name = "id_tra")
    private Integer idTra;
    private String name;
    private String identifier;
    private Integer g_start;
    private Integer g_end;

    @Formula("(SELECT s.name " +
            "FROM ngs.symbol s " +
            "WHERE s.id_reg = id_reg " +
            "AND s.hgnc_approved = true " +
            "LIMIT 1)")
    private String gene;
}
