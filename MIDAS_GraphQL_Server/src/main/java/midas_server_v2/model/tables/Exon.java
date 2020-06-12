package midas_server_v2.model.tables;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "exon", schema = "ngs")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Exon {

    @Id
    @Column(name = "id_exon")
    private Integer idExon;
    private String identifier;

    @Column(name = "g_start")
    private Integer gStart;

    @Column(name = "g_end")
    private Integer gEnd;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tra")
    private Transcript transcript;

}
