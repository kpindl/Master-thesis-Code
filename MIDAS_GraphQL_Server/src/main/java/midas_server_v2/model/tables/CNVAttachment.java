package midas_server_v2.model.tables;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Table(name = "cnv_attachment", schema = "ngs")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CNVAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cat")
    private Integer idCat;

    @Column(name = "id_ccl")
    private Integer idCcl;
    private String path;
    private String file;
}
