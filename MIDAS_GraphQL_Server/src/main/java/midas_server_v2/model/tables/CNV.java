package midas_server_v2.model.tables;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "sam_cnv", schema = "ngs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CNV {

    @Id
    @Column(name = "id_cnv")
    private int idCnv;

    @Column(name = "id_sam")
    private int idSam;

    private String identifier;

    @Formula("(SELECT csv.name FROM ngs.con_sam_var csv WHERE csv.id_csv = id_csv)")
    private String approved;

    private String method;

    @Column(name = "min_size")
    private Integer minSize;

    @Column(name = "max_size")
    private Integer maxSize;

    private String type;
    private String hgvs;
    private String iscn;
}
