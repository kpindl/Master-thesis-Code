package variantdb.models;

import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import ngstransfer.entities.laufdb.SampleInvestigationContainer;
import variantdb.RequestObject;
import variantdb.wrapper.QueryWrapper;

import java.util.List;
import java.util.stream.Collectors;

public class CNVModel {


    /**
     * Get all CNVs for given sample investigation
     * @param sampleInvestigationContainer
     * @return
     */
    public static List<CNV> getCNVsOfSampleInvestigation(SampleInvestigationContainer sampleInvestigationContainer) {

        int id_sam = sampleInvestigationContainer.getSample().getId();
        int id_inv = sampleInvestigationContainer.getInvestigation().getId_inv();
        long id_enrvers = sampleInvestigationContainer.getSample().getEnrichmentversion().getID_enrvers();

        List<CNV> cnvs = QueryWrapper.getQueryWrapper(CNV.class, new Arguments("cnvs",
                new Argument("id_sam", id_sam))).getCnvs();

        cnvs.forEach(t -> t.setGenes(t.getGenesFromTargets()));
        cnvs = filterCNVInInvestigation(cnvs, id_sam, id_inv);
        calculateTargetFrequencies(cnvs, id_enrvers);

        return cnvs;
    }

    /**
     * Select only CNVs that overlap with sample investigation
     * and filter out anonymous CNVs
     *
     * Note: CNV is removed from list if no gene name is assigned to it
     * I.e. CNV is not part of investigation and thus anonymous
     *
     * @param cnvs
     */
    private static List<CNV> filterCNVInInvestigation(List<CNV> cnvs, int id_sam, int id_inv) {
        return cnvs.stream()
                .filter(c -> isCNVInInvestigation(c, id_sam, id_inv))
                .collect(Collectors.toList());
    }


    /**
     * Calculate the inhouse frequency for all targets
     *
     * @param cnvs
     */
    private static void calculateTargetFrequencies(List<CNV> cnvs, long id_enrvers) {
        for (CNV cnv : cnvs) {
            for (Target target : cnv.getTargets()) {


                int numberSamplesPerTarget = QueryWrapper.getQueryWrapper(RequestObject.NumberSamplesPerTarget.class,
                        new Arguments("numberSamplesPerTarget",
                        new Argument("id_reg", target.getRegion().getIdReg()),
                        new Argument("evidence", target.getNumCaller()),
                        new Argument("id_enrvers", id_enrvers)))
                        .getNumberSamplesPerTarget();


                int numSamplesPerEnrvers = QueryWrapper.getQueryWrapper(RequestObject.NumberSamplesPerEnrvers.class,
                        new Arguments("numberSamplesPerEnrvers",
                        new Argument("id_enrvers", id_enrvers)))
                        .getNumberSamplesPerEnrvers();

                target.setNumSamplesIH(numberSamplesPerTarget);
                target.setFrequencyIH((double) numberSamplesPerTarget/numSamplesPerEnrvers);
            }
        }
    }

    /**
     * Checks whether at least one target of given CNV is in sample investigation
     *
     * @param cnv
     * @param id_inv
     * @param id_sam
     * @return
     */
    private static boolean isCNVInInvestigation(CNV cnv, int id_sam, int id_inv) {
        for (Target target : cnv.getTargets()) {
            for (Transcript transcript : target.getTranscripts()) {

                Boolean isTranscriptInInvestigation = QueryWrapper.getQueryWrapper(RequestObject.TranscriptInSampleInv.class,
                        new Arguments("isTranscriptInSampleInv",
                        new Argument("id_sam", id_sam),
                        new Argument("id_tra", transcript.getIdTra()),
                        new Argument("id_inv", id_inv)))
                        .getIsTranscriptInSampleInv();

                if(isTranscriptInInvestigation) return true;
            }
        }

        return false;
    }
}
