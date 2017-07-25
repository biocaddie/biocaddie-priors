package org.nationaldataservice.elasticsearch.priors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import joptsimple.internal.Strings;

public class QueryDocPriorSearchRestAction extends BaseRestHandler {
	
	@Inject
	public QueryDocPriorSearchRestAction(Settings settings, RestController controller) {
		super(settings);
		
		// Register your handlers here
		controller.registerHandler(RestRequest.Method.GET, "/{index}/{type}/_priorsearch", this);
		controller.registerHandler(RestRequest.Method.GET, "/{index}/_priorsearch", this);
	}

	protected RestChannelConsumer throwError(String error) {
		return throwError(new QueryDocPriorException(error));
	}

	protected RestChannelConsumer throwError(QueryDocPriorException ex) {
		return throwError(ex, RestStatus.BAD_REQUEST);
	}

	protected RestChannelConsumer throwError(QueryDocPriorException ex, RestStatus status) {
		this.logger.error("ERROR: " + ex.getMessage(), ex);
		
		// Log nested errors
		Throwable current = ex.getCause();
		while (current != null) {
			if (ex.getCause() != null) {
				this.logger.error("Caused By: " + current.getMessage(), current);
			}
		}
		
		return channel -> {
			XContentBuilder builder = JsonXContent.contentBuilder();
			builder.startObject();
			builder.field("error", ex.getMessage());
			builder.endObject();
			channel.sendResponse(new BytesRestResponse(status, builder));
		};
	}
	
	@Override
	protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
		this.logger.debug("Executing compute query-dependent document prior + search action!");

		// Required path parameter
		String index = request.param("index");

		// Required query string parameter
		String query = request.param("query");

		// Optional parameters, with sensible defaults
		String type = request.param("type", "dataset");
		String field = request.param("field", "_all");
		int fbDocs = Integer.parseInt(request.param("fbDocs", "10"));
		double epsilon = Double.parseDouble(request.param("epsilon", "1.0"));
		
		// Optional stoplist (defaults to null)
		String stoplist = request.param("stoplist", null);
		
		// Log the request with our full parameter set
		this.logger.info(String.format("Starting QueryDocPriorSearch (index=%s, query=%s, "
				+ "type=%s, field=%s, fbDocs=%d, stoplist=%s)", 
				index, query, type, field, fbDocs, stoplist));

		try {
			// Run initial query to compute priors
			SearchHits fbHits = runQuery(client, index, query, fbDocs);
			
			// Compute priors from feedback docs
			Map<String, Double> priors = new HashMap<String, Double>();
			for (SearchHit fbHit : fbHits) {
				Map<String, Object> source = fbHit.getSource();
				String repository = (String) source.get("REPOSITORY");
				priors.put(repository, (priors.getOrDefault(repository, epsilon) + 1.0));
			}
			for (String repository : priors.keySet()) {
				priors.put(repository, priors.get(repository) / (fbDocs + epsilon * priors.keySet().size()));
			}
			
			// Script parameters maybe?
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("priors", priors);
			
			// Now, perform the actual search with the query document prior
			this.logger.info("Running query with prior against: " + index);
			ScriptScoreFunctionBuilder scoreFunction = ScoreFunctionBuilders.scriptFunction(
					new Script(
							ScriptType.INLINE,
							"painless",
							"_score * priors.getOrDefault(_source.REPOSITORY, 1.0)",
							params));
			QueryStringQueryBuilder queryStringQueryBuilder = new QueryStringQueryBuilder(query);
			FunctionScoreQueryBuilder queryFunction = new FunctionScoreQueryBuilder(queryStringQueryBuilder, scoreFunction);

			SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index);
			searchRequestBuilder.setQuery(queryFunction);
			SearchResponse response = searchRequestBuilder.execute().actionGet();
			SearchHits hits = response.getHits();

			// Build of a response of our search hits
			this.logger.debug("Responding: " + query.toString());
			return channel -> {
				final XContentBuilder builder = JsonXContent.contentBuilder();
				//builder.startObject();
				// TODO: Match return value/structure for _search
				//builder.field("hits");
				builder.startArray();
				for (SearchHit hit : hits) {
					builder.value(hit.getSourceAsString());
				}
				builder.endArray();
				//builder.endObject();
				channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
			};
		} catch (Exception e) {
			// FIXME: Catching generic Exception is bad practice
			// TODO: make this more specific for production
			String errorMessage = e.getMessage();
			if (Strings.isNullOrEmpty(errorMessage)) {
				errorMessage = "An unknown error was encountered.";
			}
			return throwError(new QueryDocPriorException(errorMessage, e.getCause()));
		}
	}
	
	/**
	 * Run the query using the client (this assumes that the client has already
	 * been initialized and is ready to execute)
	 * 
	 * @param query
	 *            Query string
	 * @param numDocs
	 *            Number of results to return
	 * @return SearchHits object
	 */
	public SearchHits runQuery(NodeClient client, String index, String query, int numDocs) {
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index);
		QueryStringQueryBuilder queryStringQueryBuilder = new QueryStringQueryBuilder(query);
		searchRequestBuilder.setQuery(queryStringQueryBuilder).setSize(numDocs);
		SearchResponse response = searchRequestBuilder.execute().actionGet();
		SearchHits hits = response.getHits();
		return hits;
	}
}