package midas_server_v2.datafetcher;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import midas_server_v2.model.tables.Category;
import midas_server_v2.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CategoryDataFetcher {

    @Autowired
    CategoryRepository categoryRepository;


    @GraphQLQuery(name = "categoryById")
    public Optional<Category> getCategoryById(@GraphQLArgument(name = "id_cat") Integer id_cat) {
        return categoryRepository.findById(id_cat);
    }

    @GraphQLQuery(name = "categories")
    public List<Category> getAllCategories(){
        return categoryRepository.findAll();
    }

    @GraphQLMutation(name = "insertCategory")
    public Category insertCategory(@GraphQLArgument(name = "category") Category category) {
        return categoryRepository.save(category);
    }
}
