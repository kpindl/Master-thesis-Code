package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.Transcript;
import midas_server_v2.model.views.TranscriptInvestigationView;
import midas_server_v2.repository.TranscriptInvestigationViewRepository;
import midas_server_v2.repository.TranscriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TranscriptDataFetcher {

    @Autowired
    TranscriptRepository transcriptRepository;

    @Autowired
    TranscriptInvestigationViewRepository transcriptInvestigationViewRepository;


    @GraphQLQuery(name = "transcripts")
    public List<Transcript> getAllTranscript() {
        return transcriptRepository.findAll();
    }

    @GraphQLQuery(name = "transcriptById")
    public Optional<Transcript> getTranscriptById(@GraphQLArgument(name = "id_tra") Integer id_tra) {
        return transcriptRepository.findById(id_tra);
    }

    @GraphQLQuery(name = "isTranscriptInSampleInv")
    public Boolean isTranscriptInSampleInv(@GraphQLArgument(name = "id_tra") Integer id_tra,
                                           @GraphQLArgument(name = "id_sam") Integer id_sam,
                                           @GraphQLArgument(name = "id_inv") Integer id_inv) {
        Optional<TranscriptInvestigationView> transcript = transcriptInvestigationViewRepository.findByIdTraAndIdSamAndIdInv(id_tra, id_sam, id_inv);

        if(transcript.isPresent()) return true;
        return false;
    }
}