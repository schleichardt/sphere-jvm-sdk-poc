package poc1;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.Locale;

class Poc1 {
    public static Locale en = Locale.ENGLISH;

    static class ProductQueryModel extends QueryModel<Product> {
        private static final ProductQueryModel instance = new ProductQueryModel();


        ProductQueryModel() {
            super("");
        }

        public ProductCatalogDataQueryModel<Product> masterData() {
            return new ProductCatalogDataQueryModel<Product>("masterData");
        }

        public StringQueryModel<Product> id() {
            return new StringQueryModel(this, "id");
        }

        public static ProductQueryModel instance() {
            return instance;
        }
    }

    static abstract class QueryModel<T> {
        private final String pathSegment;
        private final Optional<QueryModel<T>> parent;

        protected QueryModel(String pathSegment) {
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
                    String current = currentQuery;
                    if (!input.pathSegment.isEmpty()) {
                        current = "(" + currentQuery + ")";
                    }
                    return input.buildQuery(current);
                }
            }).or(currentQuery);
        }

        @Override
        public String toString() {
            return "QueryModel{" +
                    "pathSegment='" + pathSegment + '\'' +
                    ", parent=" + parent +
                    '}';
        }
    }

    static interface Predicate<T> {
        Predicate<T> or(Predicate<T> other);

        Predicate<T> and(Predicate<T> other);

        String toSphereQuery();
    }

    static abstract class PredicateBase<T> implements Predicate<T> {
        @Override
        public Predicate<T> or(Predicate<T> other) {
            return newPredicateConnector(other, "or");
        }

        @Override
        public Predicate<T> and(Predicate<T> other) {
            return newPredicateConnector(other, "and");
        }

        private PredicateConnector newPredicateConnector(Predicate<T> other, String connectorWord) {
            return new PredicateConnector(connectorWord, this, other);
        }
    }

    static class PredicateConnector<T> extends PredicateBase<T> {
        private final String connectorWord;
        private final Predicate<T> leftPredicate;
        private final Predicate<T> rightPredicate;

        PredicateConnector(String connectorWord, Predicate<T> leftPredicate, Predicate<T> rightPredicate) {
            this.connectorWord = connectorWord;
            this.leftPredicate = leftPredicate;
            this.rightPredicate = rightPredicate;
        }

        @Override
        public String toSphereQuery() {
            return leftPredicate.toSphereQuery() + " " + connectorWord + " " + rightPredicate.toSphereQuery();
        }
    }

    static abstract class PredicateWithQueryModelBase<T> extends PredicateBase<T> {


        private final QueryModel<T> queryModel;

        protected PredicateWithQueryModelBase(QueryModel<T> queryModel) {
            this.queryModel = queryModel;
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

        public StringQueryModel(QueryModel<T> parent, String pathSegment) {
            super(parent, pathSegment);
        }

        public Predicate<T> is(String s) {
            return new EqPredicateWithQueryModel<T, String>(this, s);
        }
    }

    static class EqPredicateWithQueryModel<T, V> extends PredicateWithQueryModelBase<T> {
        private final V value;

        EqPredicateWithQueryModel(QueryModel<T> queryModel, V value) {
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

    static class LocalizedStringQueryModel<T> extends QueryModel<T> implements PredicateProducer<T> {


        public LocalizedStringQueryModel(QueryModel<T> parent, String pathSegment) {
            super(parent, pathSegment);
        }

        public StringQueryModel forLang(Locale locale) {
            return new StringQueryModel(this, locale.toLanguageTag());
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
