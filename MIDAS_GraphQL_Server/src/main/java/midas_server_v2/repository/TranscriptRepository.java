package midas_server_v2.repository;

import midas_server_v2.model.tables.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.List;

@Component
public interface TranscriptRepository extends JpaRepository<Transcript, Integer> {

    List<Transcript> findByIdTraIn(Collection<Integer> idTra);


}