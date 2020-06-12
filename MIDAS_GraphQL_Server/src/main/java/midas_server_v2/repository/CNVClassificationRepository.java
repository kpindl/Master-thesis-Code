package midas_server_v2.repository;

import midas_server_v2.model.tables.CNVClassification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CNVClassificationRepository extends JpaRepository<CNVClassification, Integer> {

    List<CNVClassification> findByIdCnvOrderByCreatetimeDesc(Integer id_cnv);
}
