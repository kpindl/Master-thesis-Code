package midas_server_v2.model.tables;

import lombok.*;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "run", schema = "ngs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Run {

    @Id
    // GenerationType.IDENTITY: performance issues!
    // Better alternative: GenerationType.SEQUENCE
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int	id_run;
    private String	illuminarunname;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "run_dep",
            schema = "ngs",
            joinColumns = @JoinColumn(name = "id_run"),
            inverseJoinColumns = @JoinColumn(name = "id_dep")
    )
    private Set<Department> departments;


    //    private int	id_ins;
//    private int	id_rus;
//    private String	flowcellid;
//    private String	assay;
//    private LocalDate runstartLocalDate;
//    private LocalDate	wetlabstartLocalDate;
//    private double	yield;
//    private double	cluster;
//    private double	clusterpf;
//    private double	percentpf;
//    private double	percentreadsidentifiedpf;
//    private double	clusterdensity;
//    private double	percentaligned;
//    private String	experimentname;
//    private String	investigatorname;
//    private double	forwardreadlength;
//    private double	reversereadlength;
//    private double	averagequality;
//    private double	qualover30;
//    private String	flowcellserialbarcode;
//    private String	flowcelllotnumber;
//    private String	sbsserialbarcode;
//    private String	sbslotnumber;
//    private String	clusterserialbarcode;
//    private String	clusterlotnumber;
//    private String	bufferserialbarcode;
//    private String	bufferlotnumber;

}
