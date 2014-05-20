package poc1;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.Locale;

class Poc1 {
    public static Locale en = Locale.ENGLISH;

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
            final String currentQuery = pathSegment + definition;
            return parent.transform(new Function<QueryModel<T>, String>() {
                @Override
                public String apply(QueryModel<T> input) {
                    return input.buildQuery("(" + currentQuery + ")");
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


        @Override
        public final String toSphereQuery() {
            return queryModel.buildQuery(toSphereQueryInternal());
        }

        protected abstract String toSphereQueryInternal();

        protected QueryModel<T> getQueryModel() {
            return queryModel;
        }
    }

    static class StringQueryModel<T> extends QueryModel<T> {

        protected StringQueryModel(String pathSegment) {
            super(pathSegment);
        }

        public Predicate<T> is(String s) {
            return new EqPredicate<T,String>(this, s);
        }
    }

    static class EqPredicate<T, V> extends PredicateBase<T> {
        private final V value;

        EqPredicate(QueryModel<T> queryModel, V value) {
            super(queryModel);
            this.value = value;
        }

        @Override
        protected String toSphereQueryInternal() {
            return "=\"" + value + '"';
        }
    }

    static class Product {

    }

    static interface PredicateProducer<T> {

    }

    static class LocalizedStringQueryModel<T> extends QueryModel<T>  implements PredicateProducer<T>{


        public LocalizedStringQueryModel(QueryModel<T> parent, String pathSegment) {
            super(parent, pathSegment);
        }

        public StringQueryModel forLang(Locale locale) {
            return new StringQueryModel(locale.toLanguageTag());
        }
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

        public StringQueryModel<T> name(Locale locale) {
            return name().forLang(locale);
        }

        public LocalizedStringQueryModel<T> name() {
            return newLocalizedStringLangQueryModel("name");
        }

        private LocalizedStringQueryModel newLocalizedStringLangQueryModel(String pathSegment) {
            return new LocalizedStringQueryModel(this, pathSegment);
        }

        public LocalizedStringQueryModel<T> slug() {
            return newLocalizedStringLangQueryModel("slug");
        }
    }
}
