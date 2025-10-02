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
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

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


    @Test
    public void testDefaultConstructor() {
        // Test the default constructor coverage
        // Note: This may fail in environments without AWS credentials/region configured
        try {
            GetAllCausesHandler handler = new GetAllCausesHandler();
            assertNotNull(handler);

            // Verify DynamoDbClient was created (using reflection to access private field)
            try {
                Field dynamoDbField = GetAllCausesHandler.class.getDeclaredField("dynamoDb");
                dynamoDbField.setAccessible(true);
                DynamoDbClient dynamoDb = (DynamoDbClient) dynamoDbField.get(handler);
                assertNotNull(dynamoDb);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                fail("Failed to access DynamoDbClient field: " + e.getMessage());
            }
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            // AWS SDK can't initialize due to missing region configuration
            // This is expected in Jenkins without AWS credentials - test passes
            System.out.println("Skipping DynamoDbClient verification due to AWS SDK configuration: " + e.getMessage());
        }
    }

    @Test
    public void testUnauthorizedUser() {
        // Test the unauthorized block coverage
        handler = new GetAllCausesHandler(dynamoDbMock);

        // Create event without JWT token (or invalid token that returns null sub)
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        // No authorizer context, so JwtUtility.getSubFromRestEvent will return null

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized"));
    }

    @Test
    public void testJsonProcessingExceptionInResponse() throws Exception {
        // Test JsonProcessingException coverage in response method by using reflection
        handler = new GetAllCausesHandler(dynamoDbMock);

        // Use reflection to access the private response method
        java.lang.reflect.Method responseMethod = GetAllCausesHandler.class.getDeclaredMethod("response", int.class, Object.class);
        responseMethod.setAccessible(true);

        // Create an object that will cause JsonProcessingException
        Object problematicObject = new Object() {
            public Object writeReplace() throws java.io.ObjectStreamException {
                throw new java.io.NotSerializableException("Not serializable");
            }
        };

        // Create a circular reference object that will cause JsonProcessingException
        Map<String, Object> circularMap = new HashMap<>();
        circularMap.put("self", circularMap);

        // This should trigger the JsonProcessingException -> RuntimeException path
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            try {
                responseMethod.invoke(handler, 500, circularMap);
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e.getCause());
            }
        });

        // Verify it's ultimately caused by JsonProcessingException
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof JsonProcessingException,
                "Expected JsonProcessingException, got: " + cause.getClass().getSimpleName());
    }

    @Test
    public void testGenericExceptionHandling() {
        // Test the generic Exception catch block coverage
        handler = new GetAllCausesHandler(dynamoDbMock);

        // Create a valid JWT event
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);

        // Mock DynamoDB to throw a generic exception (e.g., RuntimeException)
        when(dynamoDbMock.scan(any(ScanRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        // Assert
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Unexpected server error"));
        assertTrue(response.getBody().contains("Database connection failed"));
    }

}