/*
 *  midas: ngs.client
 * Autor: Lydia Leifels <lydia.leifels@medizinische-genetik.de>
 *
 * Zentrum für Humangenetik und Laboratoriumsdiagnostik (MVZ)
 * Dr. Klein, Dr. Rost und Kollegen
 * Copyright (c) 2019. All rights reserved
 *
 * Abhängigkeiten: siehe pom.xml
 * Kurzbeschreibung: siehe Javadoc
 * $Revision$
 */

package laufdb.tasks;

import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import javafx.concurrent.Task;
import new_variantview.model.SamInvCoverageContainer;
import new_variantview.model.VariantViewModel;
import new_variantview.reports.RegionCoverageModel;
import ngstransfer.entities.laufdb.Investigation;
import ngstransfer.entities.laufdb.SampleInvestigationContainer;
import ngstransfer.entities.variantdb.regioncoverage.RegionCoverage;
import ngstransfer.new_variantview.VariantTableRecord;
import variantdb.models.CNV;
import variantdb.models.CNVModel;
import variantdb.wrapper.QueryWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Create new Task for the transfer of a saminv-container, getting all Data and returning
 * a List of VariantTableRecord that will be displayed in VarinatView and all coverage information
 */
public class AddToVariantViewTask extends Task<SamInvCoverageContainer> {

    private SampleInvestigationContainer sampleInvestigationContainer;

    // Implemented by another developer
    /**
     * Creates new task requesting a list of Variants from the server
     *
     * @param sampleInvestigationContainer sample and respective investigation to request variants for
     * @throws NullPointerException if param is {@code null}
     */
    public AddToVariantViewTask(SampleInvestigationContainer sampleInvestigationContainer) {
        super();
        //null check for saminvContainer, if null throws exception
        Objects.requireNonNull(sampleInvestigationContainer);
        this.sampleInvestigationContainer = sampleInvestigationContainer;
    }

    // Implemented by another developer
    public SampleInvestigationContainer getSampleInvestigationContainer() {
        return sampleInvestigationContainer;
    }



    // Implemented by another developer - modified by Kathrin Pindl to add CNV
    /**
     * Executed by task executor service
     *
     * @return
     * @throws Exception
     */
    @Override
    protected SamInvCoverageContainer call() throws Exception {
        VariantViewModel variantViewModel = new VariantViewModel();
        Investigation investigation = sampleInvestigationContainer.getInvestigation();
        investigation.setRegions(variantViewModel.getInvestigationGenes(investigation.getId_inv()));

        // TODO: uncomment two lines and comment next two lines
//        List<VariantTableRecord> records = variantViewModel.getInvestigationVariantsByPanel(sampleInvestigationContainer);
//        List<RegionCoverage> regionCoverage = RegionCoverageModel.getRegionCoverageOfInvestigation(sampleInvestigationContainer);
        List<VariantTableRecord> records = new ArrayList<>();
        List<RegionCoverage> regionCoverage = new ArrayList<>();

        List<CNV> cnvs = CNVModel.getCNVsOfSampleInvestigation(sampleInvestigationContainer);
        SamInvCoverageContainer samInvCoverageContainer = new SamInvCoverageContainer();
        samInvCoverageContainer.setRegionCoverages(regionCoverage);
        samInvCoverageContainer.setSampleInvestigationCont(sampleInvestigationContainer);
        samInvCoverageContainer.setVariantTableRecords(records);
        samInvCoverageContainer.setCnvTableRecords(cnvs);

        return samInvCoverageContainer;
    }
}
