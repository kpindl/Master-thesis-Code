package midas_server_v2.repository;

import midas_server_v2.model.tables.Panel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface PanelRepository extends JpaRepository<Panel, Integer> {

    List<Panel> findByName(String name);
    List<Panel> findByMainpanel(Boolean mainpanel);
    List<Panel> findByNameAndMainpanel(String name, Boolean mainpanel);
}
