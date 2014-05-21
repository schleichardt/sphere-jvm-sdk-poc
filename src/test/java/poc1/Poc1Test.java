package poc1;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static poc1.Poc1.*;
import static poc1.Poc1.SortDirection.*;

public class Poc1Test {
    final ProductQueryModel q = ProductQueryModel.instance();
    final Predicate<Product> query1 = q.id().is("e7ba4c75-b1bb-483d-94d8-2c4a10f78472");
    final Predicate<Product> query2 = q.masterData().current().name(en).is("MB PREMIUM TECH T");
    final String expected1 = "id=\"e7ba4c75-b1bb-483d-94d8-2c4a10f78472\"";
    final String expected2 = "masterData(current(name(en=\"MB PREMIUM TECH T\")))";
    final Predicate<Product> query3 = query1.and(query2);


    @Test
    public void simplePredicate() {
        assertThat(query1.toSphereQuery()).isEqualTo(expected1);
    }

    @Test
    public void deepAttributePredicate() {
        assertThat(query2.toSphereQuery()).isEqualTo(expected2);
    }

    @Test
    public void connectPredicatesWithAnd() {
        final String expected = expected1 + " and " + expected2;
        assertThat(query3.toSphereQuery()).isEqualTo(expected);
    }

    @Test
    public void sort() {
        final String expected = "masterData.current.name.en asc";
        final String actual = q.masterData().current().name(en).sort(ASC).toSphereSort();
        assertThat(actual).isEqualTo(expected);
    }
}
