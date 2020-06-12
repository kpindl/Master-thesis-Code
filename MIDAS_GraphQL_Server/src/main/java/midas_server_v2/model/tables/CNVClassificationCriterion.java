package midas_server_v2.model.tables;

import lombok.*;
import org.hibernate.annotations.Formula;
import javax.persistence.*;

@Entity
@Table(name = "cnv_class_crit", schema = "ngs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CNVClassificationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ccc")
    private Integer idCcc;
    @Column(name = "id_ccl")
    private Integer idCcl;
    @Column(name = "id_cri")
    private Integer idCri;

    @Formula("(SELECT c.shortid FROM ngs.criteria c WHERE id_cri = c.id_cri)")
    private String shortid;
    @Column(name = "id_crs")
    private Integer idCrs;
    private String comment;
    private String value;
}
