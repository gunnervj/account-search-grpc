package com.bbb.grpc.account.service;

import com.bbb.grpc.account.beans.*;
import com.bbb.grpc.account.dao.AccountDao;
import com.bbb.grpc.account.exceptions.DataNotFoundException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.elasticsearch.search.SearchHit;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AccountSearchImpl extends AccountSearchServiceGrpc.AccountSearchServiceImplBase {
    private final Logger logger = Logger.getLogger(AccountSearchImpl.class.getName());
    private final AccountDao accountDao;
    private final String RESPONSE_KEY = "response";
    private final String SEARCH_AFTER_KEY = "searchAfter";
    public AccountSearchImpl() {
        this.accountDao = new AccountDao();
    }

    @Override
    public void accountSearch(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
        Object[] searchAfterValue = new Object[0];
        boolean hasNext = true;
        boolean hasResult = false;
        try {
            while (hasNext) {
                Map<String, Object> result = search(request.getSearchFieldList(), searchAfterValue);
                SearchResponse response = (SearchResponse) result.get(RESPONSE_KEY);
                searchAfterValue = (Object[]) result.get(SEARCH_AFTER_KEY);
                if (null == response || response.getAccountsList().size() <=0) {
                    hasNext = false;
                    if (!hasResult) {
                        throw new DataNotFoundException("No Results on the search criteria");
                    }
                } else {
                    responseObserver.onNext(response);
                    hasResult = true;
                }
            }
            responseObserver.onCompleted();
        } catch (DataNotFoundException e) {
            logger.log(Level.WARNING, e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("No Results on the search criteria")
                    .asRuntimeException());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while searching for account: " + e.getMessage(), e);
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Something went wrong. Try later !!")
                    .asRuntimeException());
        }

    }

    private Map<String, Object> search(List<SearchKey> searchKeys, Object[] searchAfterValue) {
        Map<String, Object> result = new HashMap<>();
        Optional<org.elasticsearch.action.search.SearchResponse> elasticResponseOpt = accountDao.searchElastic(searchKeys, searchAfterValue);
        SearchResponse.Builder responseBuilder = SearchResponse.newBuilder();
        AtomicInteger index = new AtomicInteger(0);

        if (elasticResponseOpt.isPresent()) {
            logger.info("Total Hits ::" + elasticResponseOpt.get().getHits().getTotalHits().value);
            elasticResponseOpt.get().getHits().forEach(hit -> {
                responseBuilder.addAccounts(createAccountFromSearchHit(hit));
                index.getAndIncrement();
            });
            responseBuilder.setRecordsFound((int) elasticResponseOpt.get().getHits().getTotalHits().value);
            int indexVal = index.get()-1;
            result.put(RESPONSE_KEY, responseBuilder.build());
            result.put(SEARCH_AFTER_KEY, elasticResponseOpt.get().getHits().getAt(indexVal).getSortValues());

        }

        return result;
    }

    private Account createAccountFromSearchHit(SearchHit searchHit) {
        Map<String, Object> searchResult = searchHit.getSourceAsMap();
        return Account.newBuilder()
                .setAffiliation(searchResult.get("affiliation").toString())
                .setAlterEgo(searchResult.get("alter_ego").toString())
                .setComic(searchResult.get("comic").toString())
                .setName(searchResult.get("name").toString())
                .setRole(searchResult.get("role").toString())
                .build();
    }

}
