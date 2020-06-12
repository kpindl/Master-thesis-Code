package midas_server_v2.model.tables;

import lombok.*;
import org.hibernate.annotations.Formula;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "cnv_class", schema = "ngs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CNVClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ccl")
    private Integer idCcl;

    @Column(name = "id_cnv")
    private Integer idCnv;

    @Column(name = "id_cla")
    private Integer idCla;

    @Column(name = "id_usr")
    private Integer idUsr;

    @Formula("(SELECT c.name FROM ngs.classification c WHERE c.id_cla = id_cla)")
    private String name;
    private Boolean draft;
    private LocalDateTime createtime;
    private String evaluationtext;
    private String comment;
    private String report_ref;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ccl")
    private Set<CNVClassificationCriterion> criteria;
}
