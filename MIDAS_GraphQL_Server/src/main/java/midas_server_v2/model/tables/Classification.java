package midas_server_v2.model.tables;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "classification", schema = "ngs")
@Getter
@Setter
@NoArgsConstructor
public class Classification {

    @Id
    @Column(name = "id_cla")
    private int idCla;
    private String name;
    private String color;
}
