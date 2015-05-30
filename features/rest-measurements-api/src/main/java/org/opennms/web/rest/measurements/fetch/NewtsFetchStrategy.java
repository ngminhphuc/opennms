package org.opennms.web.rest.measurements.fetch;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.RrdGraphAttribute;
import org.opennms.netmgt.rrd.newts.NewtsUtils;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.AggregationFunction;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.api.query.StandardAggregationFunctions;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;
import org.opennms.web.rest.measurements.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class NewtsFetchStrategy implements MeasurementFetchStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(NewtsFetchStrategy.class);

    private static final int STEP_LOWER_BOUND_IN_MS = 120 * 1000;

    private final ResourceDao m_resourceDao;

    private final MetricRegistry m_registry = new MetricRegistry();

    private SampleRepository m_sampleRepository = null;

    public NewtsFetchStrategy(ResourceDao resourceDao) {
        m_resourceDao = resourceDao;
    }

    @Override
    public FetchResults fetch(long start, long end, long step, int maxrows,
            List<Source> sources) throws Exception {
        // Limit the step with a lower bound in order to prevent
        // extremely large queries
        step = Math.max(STEP_LOWER_BOUND_IN_MS, step);

        Optional<Timestamp> startTs = Optional.of(Timestamp.fromEpochMillis(start));
        Optional<Timestamp> endTs = Optional.of(Timestamp.fromEpochMillis(end));

        // Newts only allows to to perform a query using single resource id
        // To work around this, we group the sources by resource id,
        // perform multiple queries and aggregate the results
        Map<String, List<Source>> sourcesByNewtsResourceId = Maps.newHashMap();

        for (Source source : sources) {
            // Grab the resource
            final OnmsResource resource = m_resourceDao.getResourceById(source
                    .getResourceId());
            if (resource == null) {
                LOG.error("No resource with id: {}", source.getResourceId());
                return null;
            }

            // Grab the attribute
            final RrdGraphAttribute rrdGraphAttribute = resource
                    .getRrdGraphAttributes().get(source.getAttribute());
            if (rrdGraphAttribute == null) {
                LOG.error("No attribute with name: {}", source.getAttribute());
            }

            final String newtsResourceId = rrdGraphAttribute.getRrdRelativePath();

            List<Source> listOfSources = sourcesByNewtsResourceId.get(newtsResourceId);
            // Create the list if it doesn't exist
            if (listOfSources == null) {
                listOfSources = Lists.newArrayList();
                sourcesByNewtsResourceId.put(newtsResourceId, listOfSources);
            }

            listOfSources.add(source);
        }

        // Used to aggregate the results
        long[] timestamps = null;
        final Map<String, double[]> columns = Maps.newHashMap();
        final Map<String, Object> constants = Maps.newHashMap();

        for (Entry<String, List<Source>> entry : sourcesByNewtsResourceId.entrySet()) {
            String newtsResourceId = entry.getKey();
            List<Source> listOfSources = entry.getValue();

            ResultDescriptor resultDescriptor = new ResultDescriptor(step);
            for (Source source : listOfSources) {
                final String metricName = source.getAttribute();
                final String name = source.getLabel();
                final AggregationFunction fn = toAggregationFunction(source.getAggregation());

                resultDescriptor.datasource(name, metricName, 2*step, fn);
                resultDescriptor.export(name);
            }

            // HACK - not sure where the / is coming from right meow
            if (newtsResourceId.startsWith("/")) {
                newtsResourceId = newtsResourceId.substring(1, newtsResourceId.length());
            }

            LOG.debug("Querying Newts for resource id {} with result descriptor: {}", newtsResourceId, resultDescriptor);
            Results<Measurement> results = getSampleRepository().select(new Resource(newtsResourceId), startTs, endTs, resultDescriptor, Duration.millis(2*step));
            Collection<Row<Measurement>> rows = results.getRows();
            LOG.debug("Found {} rows.", rows.size());

            final int N = rows.size();
            boolean setTimestamps = false;
            if (timestamps == null) {
                timestamps = new long[N];
                setTimestamps = true;
            }

            int k = 0;
            for (Row<Measurement> row : results.getRows()) {
                for (Measurement measurement : row.getElements()) {
                    if (k == 0) {
                        columns.put(measurement.getName(), new double[N]);
                    }
                    columns.get(measurement.getName())[k] = measurement.getValue();
                    Map<String, String> attributes = measurement.getAttributes();
                    if (attributes != null) {
                        constants.putAll(measurement.getAttributes());
                    }
                }

                // Only set the timestamps when processing the first resultset
                if (setTimestamps) {
                    timestamps[k] = row.getTimestamp().asMillis();
                }
                k += 1;
            }
        }

        FetchResults fetchResults = new FetchResults(timestamps, columns, step, constants);
        LOG.debug("Fetch results: {}", fetchResults);

        return fetchResults;
    }

    private synchronized SampleRepository getSampleRepository() {
        if (m_sampleRepository == null) {
            SampleProcessorService processors = new SampleProcessorService(1, Collections.emptySet());

            m_sampleRepository = new CassandraSampleRepository(NewtsUtils.getCassrandraSession(), 0, m_registry, processors);
        }
        return m_sampleRepository;
    }

    private static AggregationFunction toAggregationFunction(String fn) {
        if ("average".equalsIgnoreCase(fn) || "avg".equalsIgnoreCase(fn)) {
            return StandardAggregationFunctions.AVERAGE;
        } else if ("max".equalsIgnoreCase(fn)) {
            return StandardAggregationFunctions.MAX;
        } else if ("min".equalsIgnoreCase(fn)) {
            return StandardAggregationFunctions.MIN;
        } else {
            throw new RuntimeException("What? " + fn);
        }
    }

    @VisibleForTesting
    protected void setSampleRepository(SampleRepository sampleRepository) {
        m_sampleRepository = sampleRepository;
    }

}
