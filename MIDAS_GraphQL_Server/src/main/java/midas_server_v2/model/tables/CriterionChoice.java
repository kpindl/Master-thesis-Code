package midas_server_v2.model.tables;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "criteriachoice", schema = "ngs")
@Getter
@Setter
public class CriterionChoice {

    @Id
    @Column(name = "id_crs")
    private Integer idCrs;
    private String dropdownchoice;
}
