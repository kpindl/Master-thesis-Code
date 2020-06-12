package midas_server_v2.model.tables;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "category", schema = "ngs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue
    @Column(name = "id_cat")
    private Integer idCat;
    private String name;
    private String color;

}
