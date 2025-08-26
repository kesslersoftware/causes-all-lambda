package com.boycottpro.causes;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.boycottpro.models.Causes;
import com.boycottpro.models.Companies;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetAllCausesHandlerTest {

    private static final String TABLE_NAME = "";

    @Mock
    private DynamoDbClient dynamoDbMock;

    @InjectMocks
    private GetAllCausesHandler handler;

    @Test
    public void handleRequest() throws Exception {
        List<Map<String, AttributeValue>> mockItems = new ArrayList<>();
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("cause_id", AttributeValue.builder().s("uuid-1").build());
        item1.put("cause_desc", AttributeValue.builder().s("environment").build());
        item1.put("category", AttributeValue.builder().s("environment").build());
        item1.put("follower_count", AttributeValue.builder().n("171").build());

        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("cause_id", AttributeValue.builder().s("uuid-2").build());
        item2.put("cause_desc", AttributeValue.builder().s("union suppression").build());
        item2.put("category", AttributeValue.builder().s("labor practices").build());
        item2.put("follower_count", AttributeValue.builder().n("234").build());

        mockItems.add(item1);
        mockItems.add(item2);

        // Mock ScanResponse
        ScanResponse mockScanResponse = ScanResponse.builder()
                .items(mockItems)
                .build();
        when(dynamoDbMock.scan(any(ScanRequest.class))).thenReturn(mockScanResponse);
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);
        String body = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Causes> result = objectMapper.readValue(body, new TypeReference<List<Causes>>() {});
        assertEquals(2, result.size());
        Causes first = result.get(0);
        assertEquals("uuid-1", first.getCause_id());
        assertEquals("environment", first.getCause_desc());
        assertEquals("environment", first.getCategory());
        assertEquals(171, first.getFollower_count());
        Causes second = result.get(1);
        assertEquals("uuid-2", second.getCause_id());
        assertEquals("union suppression", second.getCause_desc());
        assertEquals("labor practices", second.getCategory());
        assertEquals(234, second.getFollower_count());
    }

}