package midas_server_v2.model.views;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "sam_inv_trans_view")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class TranscriptInvestigationView {

    @Id
    @Column(name = "id_tra")
    private Integer idTra;

    @Column(name = "id_sam")
    private Integer idSam;

    @Column(name = "id_inv")
    private Integer idInv;
    private String name;
    private String identifier;
    private Integer g_start;
    private Integer g_end;
}
