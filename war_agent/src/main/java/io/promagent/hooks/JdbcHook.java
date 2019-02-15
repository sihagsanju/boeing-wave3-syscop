
package io.promagent.hooks;

import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.MetricDef;
import io.promagent.hookcontext.MetricsStore;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;

import java.util.concurrent.TimeUnit;

import static io.promagent.hooks.HttpContext.HTTP_METHOD;
import static io.promagent.hooks.HttpContext.HTTP_PATH;

@Hook(instruments = {
        "java.sql.Statement",
        "java.sql.Connection"
})
public class JdbcHook {

    private final Counter sqlQueriesTotal;
    private final Summary sqlQueriesDuration;
    private long startTime = 0;

    public JdbcHook(MetricsStore metricsStore) {

        sqlQueriesTotal = metricsStore.createOrGet(new MetricDef<>(
                "sql_queries_total",
                (name, registry) -> Counter.build()
                        .name(name)
                        .labelNames("method", "path", "query")
                        .help("Total number of sql queries.")
                        .register(registry)
        ));

        sqlQueriesDuration = metricsStore.createOrGet(new MetricDef<>(
                "sql_query_duration",
                (name, registry) -> Summary.build()
                        .quantile(0.5, 0.05)   // Add 50th percentile (= median) with 5% tolerated error
                        .quantile(0.9, 0.01)   // Add 90th percentile with 1% tolerated error
                        .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
                        .name(name)
                        .labelNames("method", "path", "query")
                        .help("Duration for serving the sql queries in seconds.")
                        .register(registry)
        ));
    }

    private String stripValues(String query) {
        // We want the structure of the query as labels, not the actual values.
        // Therefore, we replace:
        // insert into Member (id, name, email, phone_number) values (0, 'John Smith', 'john.smith@mailinator.com', '2125551212')
        // with
        // insert into Member (id, name, email, phone_number) values (...)
        return query.replaceAll("values\\s*\\(.*?\\)", "values (...)");
    }

    // --- before

    @Before(method = {"execute", "executeQuery", "executeUpdate", "executeLargeUpdate", "prepareStatement", "prepareCall"})
    public void before(String sql) {
        startTime = System.nanoTime();
    }

    @Before(method = {"execute", "executeUpdate", "executeLargeUpdate", "prepareStatement"})
    public void before(String sql, int autoGeneratedKeys) {
        before(sql);
    }

    @Before(method = {"execute", "executeUpdate", "executeLargeUpdate", "prepareStatement"})
    public void before(String sql, int[] columnIndexes) {
        before(sql);
    }

    @Before(method = {"execute", "executeUpdate", "executeLargeUpdate", "prepareStatement"})
    public void before(String sql, String[] columnNames) {
        before(sql);
    }

    @Before(method = {"prepareStatement", "prepareCall"})
    public void before(String sql, int resultSetType, int resultSetConcurrency) {
        before(sql);
    }

    @Before(method = {"prepareStatement", "prepareCall"})
    public void before(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        before(sql);
    }

    // --- after

    @After(method = {"execute", "executeQuery", "executeUpdate", "executeLargeUpdate", "prepareStatement", "prepareCall"})
    public void after(String sql) throws Exception {
        double duration = ((double) System.nanoTime() - startTime) / (double) TimeUnit.SECONDS.toNanos(1L);
        String method = HttpContext.get(HTTP_METHOD).orElse("no http context");
        String path = HttpContext.get(HTTP_PATH).orElse("no http context");
        String query = stripValues(sql);
        sqlQueriesTotal.labels(method, path, query).inc();
        sqlQueriesDuration.labels(method, path, query).observe(duration);
    }

    @After(method = {"execute", "executeUpdate", "executeLargeUpdate", "prepareStatement"})
    public void after(String sql, int autoGeneratedKeys) throws Exception {
        after(sql);
    }

    @After(method = {"execute", "executeUpdate", "executeLargeUpdate", "prepareStatement"})
    public void after(String sql, int[] columnIndexes) throws Exception {
        after(sql);
    }

    @After(method = {"execute", "executeUpdate", "executeLargeUpdate", "prepareStatement"})
    public void after(String sql, String[] columnNames) throws Exception {
        after(sql);
    }

    @After(method = {"prepareStatement", "prepareCall"})
    public void after(String sql, int resultSetType, int resultSetConcurrency) throws Exception {
        after(sql);
    }

    @After(method = {"prepareStatement", "prepareCall"})
    public void after(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws Exception {
        after(sql);
    }
}
