package com.boycottpro.causes;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.boycottpro.models.Causes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.*;
import java.util.stream.Collectors;

public class GetAllCausesHandler implements RequestHandler<Map<String, Object>, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "causes";
    private final DynamoDbClient dynamoDb;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GetAllCausesHandler() {
        this.dynamoDb = DynamoDbClient.create();
    }

    public GetAllCausesHandler(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(Map<String, Object> input, Context context) {
        try {
            // Scan companies table
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .build();
            ScanResponse scanResponse = dynamoDb.scan(scanRequest);

            // Convert AttributeValue map to Map<String, Object>
            List<Causes> causes = scanResponse.items().stream()
                    .map(this::mapToCauses)
                    .collect(Collectors.toList());

            // Serialize to JSON
            String responseBody = objectMapper.writeValueAsString(causes);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseBody);

        } catch (JsonProcessingException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Failed to serialize items to JSON\"}");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Unexpected server error: " + e.getMessage() + "\"}");
        }
    }

    private Causes mapToCauses(Map<String, AttributeValue> item) {
        Causes cause = new Causes();
        cause.setCause_id(item.get("cause_id").s());
        cause.setCategory(item.get("category").s());
        cause.setCause_desc(item.get("cause_desc").s());
        cause.setFollower_count(Integer.parseInt(item.getOrDefault("follower_count", AttributeValue.fromN("0")).n()));
        return cause;
    }
}