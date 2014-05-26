package poc1;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static poc1.Poc1.*;
import static poc1.Poc1.SortDirection.*;

public class Poc1Test {

    @Test
    public void simplePredicate() {
        assertThat(ProductModel.get().id().is("ABC-12").toSphereQuery())
            .isEqualTo("id=\"ABC-12\"");
    }

    @Test
    public void deepAttributePredicate() {
        assertThat(ProductModel.get().masterData().current().name().lang(en).is("MB PREMIUM TECH T").toSphereQuery())
            .isEqualTo("masterData(current(name(en=\"MB PREMIUM TECH T\")))");
    }

    @Test
    public void embedded1LevelDeepPredicate() {
        Predicate<ProductDataModel> dataQuery =
            ProductDataModel.get().name().lang(en).is("Yes").or(ProductDataModel.get().name().lang(en).is("Ja"));

        assertThat(ProductModel.get().masterData().staged().where(dataQuery).toSphereQuery())
        .isEqualTo("masterData(staged(name(en=\"Yes\") or name(en=\"Ja\")))");
    }

    @Test
    public void embedded2LevelDeepPredicate() {
        Predicate<ProductDataModel> dataQuery =
            ProductDataModel.get().name().lang(en).is("Yes").or(ProductDataModel.get().name().lang(en).is("Ja"));

        Predicate<ProductCatalogDataModel> catalogQuery =
            ProductCatalogDataModel.get().current().where(dataQuery)
                .or(ProductCatalogDataModel.get().staged().where(dataQuery));

        assertThat(
            ProductModel.get().masterData().where(catalogQuery).toSphereQuery())
        .isEqualTo("masterData(current(name(en=\"Yes\") or name(en=\"Ja\")) or staged(name(en=\"Yes\") or name(en=\"Ja\")))");
    }

    @Test
    public void connectPredicatesWithAnd() {
        assertThat(
            ProductModel.get().id().is("ABC-12")
            .and(ProductModel.get().masterData().current().name().lang(en).is("MB PREMIUM TECH T"))
            .toSphereQuery())
        .isEqualTo("id=\"ABC-12\" and " +
                "masterData(current(name(en=\"MB PREMIUM TECH T\")))");
    }

    @Test
    public void sort() {
        final String expected = "masterData.current.name.en asc";
        final String actual = ProductModel.get().masterData().current().name().lang(en).sort(ASC).toSphereSort();

        assertThat(actual).isEqualTo(expected);
    }
}
