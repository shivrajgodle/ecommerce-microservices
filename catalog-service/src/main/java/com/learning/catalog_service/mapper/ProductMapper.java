package com.learning.catalog_service.mapper;

import com.learning.catalog_service.dto.response.ProductResponse;
import com.learning.catalog_service.entity.Product;
import com.learning.catalog_service.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Mapper(componentModel = "spring") tells MapStruct's annotation
 * processor to generate an implementation class registered as a Spring
 * @Component — meaning you inject ProductMapper like any other bean;
 * you never write or see the generated class's source directly (it
 * lands in target/generated-sources during compilation).
 */

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // source = "category.name" reaches through the Product -> Category
    // relationship — MapStruct generates the null-safe navigation
    // (checks category != null before calling .getName()) for you.
    @Mapping(target = "categoryName", source = "category.name")
    // MapStruct can't automatically know how to turn Set<Tag> into
    // Set<String> — it needs a method with that exact signature
    // somewhere in this interface (mapTags below) and wires it in
    // automatically by matching parameter/return types. This is the
    // "you write the hard 10%, generate the easy 90%" model MapStruct
    // actually operates on.
    @Mapping(target = "tags", source = "tags")
    ProductResponse toResponse(Product product);

    default Set<String> mapTags(Set<Tag> tags) {
        return tags == null ? Set.of() :
                tags.stream().map(Tag::getName).collect(Collectors.toSet());
    }

    // Deliberately NO toEntity(ProductRequest) method here. Building a
    // Product needs a real Category entity (a database lookup by
    // categoryId) and resolved/created Tag entities — work a pure
    // mapper has no business doing (it shouldn't depend on a
    // repository). That construction logic belongs in the service,
    // shown next


}
