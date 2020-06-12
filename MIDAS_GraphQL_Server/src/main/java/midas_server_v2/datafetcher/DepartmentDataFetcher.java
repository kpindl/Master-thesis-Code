package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.Department;
import midas_server_v2.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DepartmentDataFetcher {
    @Autowired
    DepartmentRepository departmentRepository;


    @GraphQLQuery(name = "departments")
    public List<Department> getAllDepartment(){
        return departmentRepository.findAll();
    }

    @GraphQLQuery(name = "departmentById")
    public Optional<Department> getDepartmentById(@GraphQLArgument(name = "id_dep") Integer id_dep){
        return departmentRepository.findById(id_dep);
    }
}
