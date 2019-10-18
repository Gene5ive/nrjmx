package org.newrelic.nrjmx.fetcher;

import org.newrelic.nrjmx.err.QueryException;
import org.newrelic.nrjmx.err.ValueException;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;


/**
 * JMXFetcher class reads queries from an InputStream (usually stdin) and sends the results to an OutputStream
 * (usually stdout)
 */
public class LegacyFetcher implements Fetcher {
    private static final Logger LOG = Logger.getLogger("nrjmx");
    private MBeanServerConnection connection;
    private Executor executor;

    public LegacyFetcher(MBeanServerConnection connection, Executor executor) {
        this.connection = connection;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Map<String, Object>> query(String beanName) {
        CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
        executor.execute(() -> {
            ObjectName queryObject;
            try {
                queryObject = new ObjectName(beanName);
            } catch (Exception e) {
                result.completeExceptionally(new QueryException("Can't parse bean name " + beanName, e));
                return;
            }

            Set<ObjectInstance> beans;
            try {
                beans = connection.queryMBeans(queryObject, null);
            } catch (Exception e) {
                result.completeExceptionally(new QueryException("Can't get beans for query " + beanName, e));
                return;
            }

            Map<String, Object> store = new HashMap<>();
            for (ObjectInstance bean : beans) {
                try {
                    queryAttributes(bean, store);
                } catch (Exception e) {
                    // difference with old nrjmx: we ignore this and go for the next attribute
                    LOG.fine("can't query attributes for bean " + bean.getObjectName());
                }
            }
            result.complete(store);
        });
        return result;
    }

    // todo: run in parallel with a timeout, and if it doesnt succeed, just ignore this metric
    private void queryAttributes(ObjectInstance instance, Map<String, Object> store) throws QueryException {
        ObjectName objectName = instance.getObjectName();
        MBeanInfo info;

        try {
            info = connection.getMBeanInfo(objectName);
        } catch (InstanceNotFoundException | IntrospectionException | ReflectionException | IOException e) {
            throw new QueryException("Can't find bean " + objectName.toString(), e);
        }

        MBeanAttributeInfo[] attrInfo = info.getAttributes();

        for (MBeanAttributeInfo attr : attrInfo) {
            if (!attr.isReadable()) {
                continue;
            }

            String attrName = attr.getName();
            Object value;

            try {
                value = connection.getAttribute(objectName, attrName);
                if (value instanceof Attribute) {
                    Attribute jmxAttr = (Attribute) value;
                    value = jmxAttr.getValue();
                }
            } catch (Exception e) {
                LOG.warning("Can't get attribute " + attrName + " for bean " + objectName.toString() + ": " + e.getMessage());
                continue;
            }

            String name = objectName + ",attr=" + attrName;
            try {
                parseValue(name, value, store);
            } catch (Exception e) {
                LOG.fine(e.getMessage());
            }
        }
    }

    private void parseValue(String name, Object value, Map<String, Object> store) throws ValueException {
        if (value == null) {
            throw new ValueException("Found a null value for bean " + name);
        } else if (value instanceof java.lang.Double) {
            Double ddata = parseDouble((Double) value);
            store.put(name, ddata);
        } else if (value instanceof java.lang.Float) {
            Float ddata = parseFloat((Float) value);
            store.put(name, ddata);
        } else if (value instanceof Number || value instanceof String || value instanceof Boolean) {
            store.put(name, value);
        } else if (value instanceof CompositeData) {
            CompositeData cdata = (CompositeData) value;
            Set<String> fieldKeys = cdata.getCompositeType().keySet();

            for (String field : fieldKeys) {
                if (field.length() < 1) continue;

                String fieldKey = Character.toUpperCase(field.charAt(0)) + field.substring(1);
                parseValue(name + "." + fieldKey, cdata.get(field), store);
            }
        } else if (value instanceof HashMap) {
            // TODO: Process hashmaps
            LOG.fine("HashMaps are not supported yet: " + name);
        } else if (value instanceof ArrayList || value.getClass().isArray()) {
            // TODO: Process arrays
            LOG.fine("Arrays are not supported yet: " + name);
        } else {
            throw new ValueException("Unsuported data type (" + value.getClass() + ") for bean " + name);
        }
    }

    /**
     * XXX: JSON does not support NaN, Infinity, or -Infinity as they come back from JMX.
     * So we parse them out to 0, Max Double, and Min Double respectively.
     */
    private Double parseDouble(Double value) {
        if (value.isNaN()) {
            return 0.0;
        } else if (value == Double.NEGATIVE_INFINITY) {
            return Double.MIN_VALUE;
        } else if (value == Double.POSITIVE_INFINITY) {
            return Double.MAX_VALUE;
        }

        return value;
    }

    /**
     * XXX: JSON does not support NaN, Infinity, or -Infinity as they come back from JMX.
     * So we parse them out to 0, Max Double, and Min Double respectively.
     */
    private Float parseFloat(Float value) {
        if (value.isNaN()) {
            return 0.0f;
        } else if (value == Float.NEGATIVE_INFINITY) {
            return Float.MIN_VALUE;
        } else if (value == Float.POSITIVE_INFINITY) {
            return Float.MAX_VALUE;
        }

        return value;
    }
}
