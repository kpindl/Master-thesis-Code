package midas_server_v2.model.tables;


import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "sample", schema = "ngs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sample {

    @Id
    @Column(name = "id_sam")
    private Integer idSam;

    @Column(name = "id_pat")
    private Integer idPat;

    @Column(name = "id_run")
    private Integer idRun;

    @Column(name = "id_enrvers")
    private Integer idEnrvers;

    @Column(name = "id_sas")
    private Integer idSas;

    private String patnr;

    // to be continued ..
}
