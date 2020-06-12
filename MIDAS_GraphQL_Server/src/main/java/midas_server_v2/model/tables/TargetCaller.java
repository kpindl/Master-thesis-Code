package midas_server_v2.model.tables;


import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;


import javax.persistence.*;

@Entity
@Table(name = "sam_tar_cal", schema = "ngs")
@Getter
@Setter
public class TargetCaller {

    @Id
    @Column(name = "id_stc")
    private Integer idStc;

    @Column(name = "id_sat")
    private Integer idSat;

    @Column(name = "id_cal")
    private Integer idCal;
    private String caller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sat", insertable = false, updatable = false)
    private Target target;
}
