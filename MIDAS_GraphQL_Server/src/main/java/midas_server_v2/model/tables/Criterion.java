package midas_server_v2.model.tables;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "criteria", schema="ngs")
@Getter @Setter
@NoArgsConstructor
public class Criterion {

    @Id
    @Column(name = "id_cri")
    private Integer idCri;
    private String shortid;
    private String description;
}
