package org.nationaldataservice.elasticsearch.priors.test.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.logging.ESLoggerFactory;

/**
 * This is a simple integration test suite for the ElasticSearch Priors
 * Plugin. Use these test cases to verify correctness of the API endpoint, input
 * validation, compare performance, scale testing, etc <br/>
 * Before the test suite runs, the test runner will:
 * 
 * <pre>
 *    * Download ElasticSearch binaries
 *    * Install the ElasticSearch Priors Plugin
 *    * Start up an ElasticSearch cluster
 *    * Run the set of test cases
 *    * Tear down the cluster
 * </pre>
 * 
 * @see {@link AbstractITCase}
 * @see src/test/ant/integration-tests.xml
 * 
 * @author lambert8
 *
 */
public class PriorsIT extends AbstractITCase {
	private static final Logger staticLogger = ESLoggerFactory.getLogger(PriorsIT.class);

	// The common test parameter set (individual tests can still use one-off
	// values)
	private static final String TEST_INDEX = "biocaddie";
	private static final String TEST_TYPE = "dataset";
	private static final int TEST_FB_DOCS = 5;
	
	private final String defaultEndpointParameters = "fbDocs=" + TEST_FB_DOCS;
	private final String priorEndpoint = String.format("/%s/%s/_priorsearch?%s", TEST_INDEX, TEST_TYPE,
			defaultEndpointParameters);

	@Test
	@SuppressWarnings("unchecked")
	public void testPluginIsLoaded() throws Exception {

		Response response = client.performRequest("GET", "/_nodes/plugins");

		Map<String, Object> nodes = (Map<String, Object>) entityAsMap(response).get("nodes");
		for (String nodeName : nodes.keySet()) {
			boolean pluginFound = false;
			Map<String, Object> node = (Map<String, Object>) nodes.get(nodeName);
			List<Map<String, Object>> plugins = (List<Map<String, Object>>) node.get("plugins");
			for (Map<String, Object> plugin : plugins) {
				String pluginName = (String) plugin.get("name");
				if (pluginName.equals("querydocprior")) {
					pluginFound = true;
					break;
				}
			}
			assertThat(pluginFound, is(true));
		}
	}

	@Test
	public void testPriorsEndpoint() throws Exception {
		// Insert test case here using priorEndpoint
	}
}