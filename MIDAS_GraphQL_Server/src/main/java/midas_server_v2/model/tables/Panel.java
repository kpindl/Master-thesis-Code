package midas_server_v2.model.tables;

import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.*;
import org.hibernate.annotations.Fetch;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "panel", schema = "ngs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Panel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pan")
    private Integer idPan;

    @GraphQLQuery(name = "name")
    private String name;

    @GraphQLQuery(name = "mainpanel")
    private Boolean mainpanel;

    private String url;

    @ManyToOne
    @JoinColumn(name = "id_cat")
    private Category category;

    private Boolean released;
    private Timestamp released_dt;
    private Integer released_usr;
    private Boolean decomissioned;
    private Timestamp decomissioned_dt;
    private Integer decomissioned_usr;
    private Integer versionnumber;
    private Boolean tobeorderedgermline;
    private Boolean tobeorderedsomatic;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "pan_trans",
            schema = "ngs",
            joinColumns = @JoinColumn(name = "id_pan"),
            inverseJoinColumns = @JoinColumn(name = "id_tra")
    )
    private Set<Transcript> transcripts;
}
