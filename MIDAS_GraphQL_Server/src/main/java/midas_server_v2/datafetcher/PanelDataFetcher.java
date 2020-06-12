package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.Panel;
import midas_server_v2.repository.PanelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class PanelDataFetcher {
    private static Logger LOGGER = Logger.getLogger(PanelDataFetcher.class.getName());

    @Autowired
    private PanelRepository panelRepository;


    @GraphQLQuery(name = "panels")
    public List<Panel> getPanel(@GraphQLArgument(name = "name") String name,
                                @GraphQLArgument(name = "mainpanel") Boolean mainpanel) {

        if (!Objects.isNull(name) && !Objects.isNull(mainpanel)) {
            return panelRepository.findByNameAndMainpanel(name, mainpanel);
        }
        if (!Objects.isNull(name) && Objects.isNull(mainpanel)) {
            return panelRepository.findByName(name);
        }
        if (Objects.isNull(name) && !Objects.isNull(mainpanel)) {
            return panelRepository.findByMainpanel(mainpanel);
        }
        if (Objects.isNull(name) && Objects.isNull(mainpanel)) {
            return panelRepository.findAll();
        }

        return null;
    }

    @GraphQLQuery(name = "panelById")
    public Optional<Panel> getPanelById(@GraphQLArgument(name = "id_pan") Integer id_pan) {
        return panelRepository.findById(id_pan);
    }

    @GraphQLMutation(name = "insertPanel")
    public Panel insertPanel(@GraphQLArgument(name = "panel") Panel panel) {
        return panelRepository.save(panel);
    }
}
