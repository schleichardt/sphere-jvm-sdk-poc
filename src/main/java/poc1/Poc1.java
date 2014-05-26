package poc1;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import javax.annotation.Nullable;
import java.util.Locale;

class Poc1 {
    public static final Locale en = Locale.ENGLISH;
    public static final Locale de = Locale.GERMAN;

    public static enum SortDirection {
        ASC, DESC
    }

    // ---------------------------------------------------------- Sort

    static interface Sort<T> {
        String toSphereSort();
    }

    static class SphereSort<T> implements Sort<T> {
        private final QueryModel<T> path;
        private final SortDirection direction;

        protected SphereSort(QueryModel<T> path, SortDirection direction) {
            this.path = path;
            this.direction = direction;
        }

        public String toSphereSort() {
            return renderPath(path) + " " + direction.toString().toLowerCase();
        }

        private String renderPath(QueryModel<T> model) {
            if (model.getParent().isPresent()) {
                String beginning = renderPath(model.getParent().get());

                return beginning +
                        (model.getPathSegment().isPresent() ?
                                (beginning.isEmpty() ? "" : ".") + model.getPathSegment().get() : "");
            } else {
                return "";
            }
        }
    }

    // ---------------------------------------------------------- Predicate

    static interface Predicate<T> {
        Predicate<T> or(Predicate<T> other);

        Predicate<T> and(Predicate<T> other);

        String toSphereQuery();
    }

    static abstract class PredicateBase<T> implements Predicate<T> {
        public Predicate<T> or(Predicate<T> other) {
            return new PredicateConnector<>("or", this, other);
        }

        public Predicate<T> and(Predicate<T> other) {
            return new PredicateConnector<>("and", this, other);
        }

        public String buildQuery(QueryModel<T> model, String definition) {
            String current = (model.getPathSegment().isPresent() ? model.getPathSegment().get() : "") + definition;

            if (model.getParent().isPresent()) {
                QueryModel<T> parent = model.getParent().get();
                return buildQuery(parent, parent.getPathSegment().isPresent() ? "(" + current + ")" : current);
            } else {
                return current;
            }
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

    static abstract class QueryModelPredicate<T> extends PredicateBase<T> {
        private final QueryModel<T> queryModel;

        protected QueryModelPredicate(QueryModel<T> queryModel) {
            this.queryModel = queryModel;
        }

        @Override
        public final String toSphereQuery() {
            return buildQuery(queryModel, render());
        }

        protected abstract String render();

        protected QueryModel<T> getQueryModel() {
            return queryModel;
        }
    }

    // ---------------------------------------------------------- Sorting Model

    static interface SortingModel<T> {
        public abstract Sort<T> sort(SortDirection sortDirection);
    }

    // ---------------------------------------------------------- Query Model

    static abstract class QueryModel<T> {
        private final Optional<String> pathSegment;
        private final Optional<? extends QueryModel<T>> parent;

        protected QueryModel(Optional<? extends QueryModel<T>> parent, Optional<String> pathSegment) {
            this.parent = parent;
            this.pathSegment = pathSegment;
        }

        public Optional<String> getPathSegment() {
            return pathSegment;
        }

        public Optional<? extends QueryModel<T>> getParent() {
            return parent;
        }
    }

    static abstract class EmbeddedQueryModel<T, C> extends QueryModel<T> {
        protected EmbeddedQueryModel(Optional<? extends QueryModel<T>> parent, Optional<String> pathSegment) {
            super(parent, pathSegment);
        }

        public Predicate<T> where(Predicate<C> embeddedPredicate) {
            return new EmbeddedPredicate<>(this, embeddedPredicate);
        }
    }

    static class StringQueryModel<T> extends QueryModel<T> {
        protected StringQueryModel(Optional<? extends QueryModel<T>> parent, Optional<String> pathSegment) {
            super(parent, pathSegment);
        }

        public Predicate<T> is(String s) {
            return new EqPredicate<>(this, s);
        }
    }

    static class StringQueryWithSoringModel<T> extends StringQueryModel<T> implements SortingModel<T> {
        protected StringQueryWithSoringModel(Optional<? extends QueryModel<T>> parent, Optional<String> pathSegment) {
            super(parent, pathSegment);
        }

        @Override
        public Sort<T> sort(SortDirection sortDirection) {
            return new SphereSort<T>(this, sortDirection);
        }
    }

    static class EqPredicate<T, V> extends QueryModelPredicate<T> {
        private final V value;

        EqPredicate(QueryModel<T> queryModel, V value) {
            super(queryModel);
            this.value = value;
        }

        @Override
        protected String render() {
            return "=\"" + value + '"';
        }
    }

    static class EmbeddedPredicate<T, C> extends QueryModelPredicate<T> {
        private final Predicate<C> embedded;

        protected EmbeddedPredicate(QueryModel<T> queryModel, Predicate<C> embedded) {
            super(queryModel);

            this.embedded = embedded;
        }

        @Override
        protected String render() {
            return "(" + embedded.toSphereQuery() + ")";
        }
    }

    static class LocalizedStringModel<T> extends EmbeddedQueryModel<T, LocalizedStringModel> {
        protected LocalizedStringModel(Optional<? extends QueryModel<T>> parent, Optional<String> pathSegment) {
            super(parent, pathSegment);
        }

        public StringQueryWithSoringModel<T> lang(Locale locale) {
            return new StringQueryWithSoringModel<T>(Optional.of(this), Optional.of(locale.toLanguageTag()));
        }
    }

    // ---------------------------------------------------------- Product Model

    static class ProductModel<T> extends EmbeddedQueryModel<T, ProductModel> {
        private static final ProductModel<ProductModel> instance =
                new ProductModel<>(Optional.<QueryModel<ProductModel>>absent(), Optional.<String>absent());

        public static ProductModel<ProductModel> get() {
            return instance;
        }

        private ProductModel(Optional<QueryModel<T>> parent, Optional<String> pathSegment) {
            super(parent, pathSegment);
        }

        public ProductCatalogDataModel<T> masterData() {
            return new ProductCatalogDataModel<T>(Optional.of(this), Optional.of("masterData"));
        }

        public ProductCatalogDataModel<T> catalogs() {
            return new ProductCatalogDataModel<T>(Optional.of(this), Optional.of("catalogs"));
        }

        public StringQueryWithSoringModel<T> id() {
            return new StringQueryWithSoringModel<T>(Optional.of(this), Optional.of("id"));
        }
    }

    static class ProductCatalogDataModel<T> extends EmbeddedQueryModel<T, ProductCatalogDataModel> {
        private static final ProductCatalogDataModel<ProductCatalogDataModel> instance =
                new ProductCatalogDataModel<>(Optional.<QueryModel<ProductCatalogDataModel>>absent(), Optional.<String>absent());

        public static ProductCatalogDataModel<ProductCatalogDataModel> get() {
            return instance;
        }

        protected ProductCatalogDataModel(Optional<? extends QueryModel<T>> parent, Optional<String> pathSegment) {
            super(parent, pathSegment);
        }

        public ProductDataModel<T> current() {
            return new ProductDataModel<T>(Optional.of(this), Optional.of("current"));
        }

        public ProductDataModel<T> staged() {
            return new ProductDataModel<T>(Optional.of(this), Optional.of("staged"));
        }
    }

    static class ProductDataModel<T> extends EmbeddedQueryModel<T, ProductDataModel> {
        private static final ProductDataModel<ProductDataModel> instance =
                new ProductDataModel<>(Optional.<QueryModel<ProductDataModel>>absent(), Optional.<String>absent());

        public static ProductDataModel<ProductDataModel> get() {
            return instance;
        }

        protected ProductDataModel(Optional<? extends QueryModel<T>> parent, Optional<String> pathSegment) {
            super(parent, pathSegment);
        }

        public LocalizedStringModel<T> name() {
            return new LocalizedStringModel<T>(Optional.of(this), Optional.of("name"));
        }

        public LocalizedStringModel<T> slug() {
            return new LocalizedStringModel<T>(Optional.of(this), Optional.of("slug"));
        }
    }
}
