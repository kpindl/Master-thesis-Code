package midas_server_v2.model.tables;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "department", schema = "ngs")
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class Department {

    @Id
    @Column(name = "id_dep")
    private Integer idDep;
    private String name;
}
