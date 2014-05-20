import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.Locale;

class Poc {
    public static Locale en = Locale.ENGLISH;

    public static void main(String[] args) {

        //masterData(current(name(en="MB PREMIUM TECH T"))) and id = "e7ba4c75-b1bb-483d-94d8-2c4a10f78472"
        final ProductQueryModel q = ProductQueryModel.instance();
        final Predicate<Product> predicate = q.id().is("e7ba4c75-b1bb-483d-94d8-2c4a10f78472");
        final Predicate<Product> productPredicate =
                q.masterData().current().name(en).is("MB PREMIUM TECH T").and(predicate);
        //String ist viel kürzer, leichter zu lesen und leichter zu implementieren


        System.out.println(predicate.toSphereQuery());

        //masterData(current(slug(en="peter-42") and name(en="Peter")))
//        final ProductDataQueryModel<Product> a = q.masterData().current();
//        a.slug(en).is("peter-42").and(a.name(en).is("Peter"));
    }


    static class ProductQueryModel {
        private static final ProductQueryModel instance = new ProductQueryModel();


        ProductQueryModel() {
        }

        public ProductCatalogDataQueryModel<Product> masterData() {
            return new ProductCatalogDataQueryModel<Product>("masterData");
        }

        public StringQueryModel<Product> id() {
            return new StringQueryModel("id");
        }

        public static ProductQueryModel instance() {
            return instance;
        }
    }

    static abstract class QueryModel<T> {
        private final String pathSegment;
        private final Optional<QueryModel<T>> parent;

        protected  QueryModel(String pathSegment) {
            this(Optional.<QueryModel<T>>absent(), pathSegment);
        }

        public QueryModel(QueryModel<T> parent, String pathSegment) {
            this(Optional.fromNullable(parent), pathSegment);
        }

        public QueryModel(Optional<QueryModel<T>> parent, String pathSegment) {
            this.parent = parent;
            this.pathSegment = pathSegment;
        }

        public String buildQuery(final String definition) {
            final String currentQuery = pathSegment + "(" + definition + ")";
            return parent.transform(new Function<QueryModel<T>, String>() {
                @Override
                public String apply(QueryModel<T> input) {
                    return input.buildQuery(currentQuery);
                }
            }).or(currentQuery);
        }
    }


    static interface Predicate<T> {
        Predicate<T> or(Predicate<T> other);

        Predicate<T> and(Predicate<T> other);

        String toSphereQuery();
    }

    static abstract class PredicateBase<T> implements Predicate<T> {


        private final QueryModel<T> queryModel;

        protected PredicateBase(QueryModel<T> queryModel) {
            this.queryModel = queryModel;
        }

        @Override
        public Predicate<T> or(Predicate<T> other) {
            return null;
        }

        @Override
        public Predicate<T> and(Predicate<T> other) {
            return null;
        }
//
//        @Override
//        public String toSphereQuery() {
//            return null;
//        }


        protected QueryModel<T> getQueryModel() {
            return queryModel;
        }
    }




    static class StringQueryModel<T> extends QueryModel<T> {

        protected StringQueryModel(String pathSegment) {
            super(pathSegment);
        }

        public Predicate<T> is(String s) {
            return null;
        }
    }



    //should be package scope?
    static class PredicateFactory {

    }

    static class Eq<T> extends PredicateBase<T> {
        private final T value;

        Eq(QueryModel<T> queryModel, T value) {
            super(queryModel);
            this.value = value;
        }

        @Override
        public String toSphereQuery() {
            return "=" + value;
        }
    }








    static class Product {

    }

    static interface PredicateProducer<T> {

    }


    static class LocalizedStringLangQueryModel<T> implements PredicateProducer<T>{

        public Predicate<T> is(String s) {
            return null;
        }
    }


    static class LocalizedStringQueryModel<T> {
    }

    static class ProductCatalogDataQueryModel<T> extends QueryModel<T> {
        public ProductCatalogDataQueryModel(String pathSegment) {
            super(pathSegment);
        }

        public ProductDataQueryModel<T> current() {
            return new ProductDataQueryModel(this, "current");
        }
    }

    static class ProductDataQueryModel<T> extends QueryModel<T> {

        public ProductDataQueryModel(QueryModel<T> parent, String pathSegment) {
            super(parent, pathSegment);
        }

        public LocalizedStringQueryModel<T> slug() {
            return null;
        }

        public LocalizedStringQueryModel<T> name() {
            return null;
        }

        public LocalizedStringLangQueryModel<T> name(Locale locale) {
            return null;
        }

        public LocalizedStringLangQueryModel<T> slug(Locale locale) {
            return null;
        }
    }
}
