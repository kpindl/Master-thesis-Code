package midas_server_v2.repository;

import midas_server_v2.model.views.TranscriptInvestigationView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface TranscriptInvestigationViewRepository extends JpaRepository<TranscriptInvestigationView, Integer> {

    @Query("SELECT t " +
            "FROM  TranscriptInvestigationView t " +
            "WHERE t.idSam = ?1 " +
            "AND   t.identifier = ?2 " +
            "AND   t.g_start <= ?3 " +
            "AND   t.g_end >= ?4")
    List<TranscriptInvestigationView> findTranscriptByTarget(int id_sam, String identifier, int g_start, int g_end);

    Optional<TranscriptInvestigationView> findByIdTraAndIdSamAndIdInv(int id_tra, int id_sam, int id_inv);
}
