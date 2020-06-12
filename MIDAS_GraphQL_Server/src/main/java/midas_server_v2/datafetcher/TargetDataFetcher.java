package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.Region;
import midas_server_v2.model.tables.Target;
import midas_server_v2.model.tables.Transcript;
import midas_server_v2.model.views.TranscriptInvestigationView;
import midas_server_v2.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TargetDataFetcher {

    // region autowired repositories
    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private TargetCallerRepository targetCallerRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TranscriptInvestigationViewRepository transcriptInvestigationViewRepository;

    @Autowired
    private TranscriptRepository transcriptRepository;

    @Autowired
    private SampleRepository sampleRepository;
    // endregion


    @GraphQLQuery(name = "region")
    public Region getRegion(@GraphQLContext Target target) {
        return regionRepository.findByIdReg(target.getRegion().getIdReg());
    }


    @GraphQLQuery(name = "caller")
    public List<String> getCaller(@GraphQLContext Target target) {

        return targetCallerRepository.findByIdSat(target.getIdSat())
                .stream()
                .map(t -> t.getCaller())
                .collect(Collectors.toList());
    }


    @GraphQLQuery(name = "transcripts")
    public List<Transcript> getTranscripts(@GraphQLContext Target target) {

        List<Integer> transcriptsId = transcriptInvestigationViewRepository.findTranscriptByTarget(target.getIdSam(),
                target.getRegion().getIdentifier(),
                target.getRegion().getG_start(),
                target.getRegion().getG_end())
                .stream()
                .map(TranscriptInvestigationView::getIdTra)
                .collect(Collectors.toList());

        return transcriptRepository.findByIdTraIn(transcriptsId);
    }


    @GraphQLQuery(name = "numberSamplesPerTarget")
    public Integer getNumberSamplesPerTarget(@GraphQLArgument(name = "id_reg") int id_reg,
                                             @GraphQLArgument(name = "evidence") int evidence,
                                             @GraphQLArgument(name = "id_enrvers") int id_enrvers) {
        return targetRepository.getNumberSamplesIH(id_reg, evidence, id_enrvers);
    }
}
