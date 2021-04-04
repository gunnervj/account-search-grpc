package com.bbb.grpc.account.dao;

import com.bbb.grpc.account.beans.Account;
import com.bbb.grpc.account.beans.SearchKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountDao {
    private final Logger logger = Logger.getLogger(AccountDao.class.getName());
    private final RestHighLevelClient elasticClient;

    public AccountDao() {
        this.elasticClient = createEasticClient();
    }

    private RestHighLevelClient createEasticClient() {
        List<HttpHost> nodes = new ArrayList<>();
        getElasticHostNames().forEach(hostDet -> {
            createHttpHost(hostDet).ifPresent(nodes::add);
        });
        return new RestHighLevelClient(RestClient.builder(nodes.toArray(new HttpHost[0])));
    }

    public Optional<SearchResponse> searchElastic(List<SearchKey> searchKeys, Object[] searchAfterValue) {
        logger.log(Level.FINE, "Init elastic search request");
        Account.Builder accountBuilder = Account.newBuilder();
        try {
            SearchRequest searchRequest = new SearchRequest("accounts");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.size(5);
            if (null != searchAfterValue && searchAfterValue.length != 0) {
                searchSourceBuilder.searchAfter(searchAfterValue);
            }
            searchSourceBuilder.sort("id", SortOrder.ASC);
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
            searchKeys.forEach(key -> {
                logger.log(Level.FINE, "Criteria Key: " + key.getName() + ", Value: " + key.getValue());
                queryBuilder.filter(QueryBuilders.matchQuery(key.getName(), key.getValue()));
            });
            searchSourceBuilder.query(queryBuilder);
            searchRequest.source(searchSourceBuilder);

            logger.log(Level.FINE, "Performing search");
            SearchResponse searchResponse =
                    elasticClient.search(searchRequest, RequestOptions.DEFAULT);
            logger.log(Level.FINER, "Received elastic response. Hits: " + Arrays.toString(searchResponse.getHits().getHits()));

            if (null != searchResponse.getHits() &&
                    searchResponse.getHits().getHits().length > 0) {
                return Optional.of(searchResponse);
            }

        } catch (Exception e) {
            logger.log(Level.ALL, "Error while searching elastic search", e);
        }

        return Optional.empty();
    }

    private List<String> getElasticHostNames() {
        String elasticHosts = System.getenv("ELASTIC_HOSTS");
        if (StringUtils.isBlank(elasticHosts)) {
            return Collections.singletonList("http://localhost:9200");
        } else {
            return Arrays.asList(elasticHosts.split(","));
        }
    }

    private Optional<HttpHost> createHttpHost(String hostUrlString) {
        try {
            URL hostUrl = new URL(hostUrlString);
            logger.log(Level.INFO, "Adding Host: " + hostUrl.toString());
            return Optional.of(new HttpHost(hostUrl.getHost(), hostUrl.getPort(), hostUrl.getProtocol()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while creating HttpHost: Message: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

}
