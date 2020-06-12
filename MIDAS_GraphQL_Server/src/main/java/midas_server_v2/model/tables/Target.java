package midas_server_v2.model.tables;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Formula;

import javax.persistence.*;
import java.util.List;


@Entity
@Table(name = "sam_target", schema = "ngs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Target {

    @Id
    @Column(name = "id_sat")
    private Integer idSat;

    @Column(name = "id_cnv")
    private Integer idCnv;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_reg")
    private Region region;

    @Transient
    private Integer idSam;
    private Double coverage;

    @OneToMany(mappedBy = "idSat", fetch = FetchType.LAZY)
    private List<TargetCaller> targetCaller;
}
