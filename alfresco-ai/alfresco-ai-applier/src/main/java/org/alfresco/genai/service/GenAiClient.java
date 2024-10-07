package org.alfresco.genai.service;

import jakarta.annotation.PostConstruct;
import okhttp3.*;

import org.alfresco.genai.action.AiApplierEntityLinkDBpedia;
import org.alfresco.genai.model.Description;
import org.alfresco.genai.model.EntityLinks;
import org.alfresco.genai.model.Summary;
import org.alfresco.genai.model.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.List;

/**
 * The {@code GenAiClient} class is a Spring service responsible for interacting with the GenAI service to obtain
 * summaries and terms. It uses an {@link OkHttpClient} to perform HTTP requests to the GenAI service endpoint.
 */
@Service
public class GenAiClient {

    static final Logger LOG = LoggerFactory.getLogger(GenAiClient.class);
    
    /**
     * The base URL of the GenAI service obtained from configuration.
     */
    @Value("${genai.url}")
    String genaiUrl;

    /**
     * The request timeout for GenAI service requests obtained from configuration.
     */
    @Value("${genai.request.timeout}")
    Integer genaiTimeout;

   
    /**
     * Static instance of {@link JsonParser} to parse JSON responses from the GenAI service.
     */
    static final JsonParser JSON_PARSER = JsonParserFactory.getJsonParser();

    /**
     * The OkHttpClient instance for making HTTP requests to the GenAI service.
     */
    OkHttpClient client;

    /**
     * Initializes the OkHttpClient with specified timeouts during bean creation.
     */
    @PostConstruct
    public void init() {
        client = new OkHttpClient()
                .newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(genaiTimeout, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Retrieves a document summary from the GenAI service for the provided PDF file.
     *
     * @param pdfFile The PDF file for which the summary is requested.
     * @return A {@link Summary} object containing the summary, tags, and model information.
     * @throws IOException If an I/O error occurs during the HTTP request or response processing.
     */
    public Summary getSummary(File pdfFile) throws IOException {

        RequestBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", pdfFile.getName(), RequestBody.create(pdfFile, MediaType.parse("application/pdf")))
                .build();

        Request request = new Request
                .Builder()
                .url(genaiUrl + "/summary")
                .post(requestBody)
                .build();

        String response = client.newCall(request).execute().body().string();
        Map<String, Object> aiResponse = JSON_PARSER.parseMap(response);
        return new Summary()
                .summary(aiResponse.get("summary").toString().trim())
                .tags(Arrays.asList(aiResponse.get("tags").toString().split(",", -1)))
                .model(aiResponse.get("model").toString());
    }

    /**
     * Selects a term from a term list using the GenAI service for the provided PDF file.
     *
     * @param pdfFile   The PDF file containing the document related to the question.
     * @param termList  List of terms that includes options to be selected.
     * @return An {@link Term} object containing the term and the model information.
     * @throws IOException If an I/O error occurs during the HTTP request or response processing.
     */
    public Term getTerm(File pdfFile, String termList) throws IOException {

        RequestBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", pdfFile.getName(), RequestBody.create(pdfFile, MediaType.parse("application/pdf")))
                .build();

        HttpUrl httpUrl = HttpUrl.parse(genaiUrl + "/classify")
                .newBuilder()
                .addQueryParameter("termList", "\"" + termList + "\"")
                .build();

        Request request = new Request
                .Builder()
                .url(httpUrl)
                .post(requestBody)
                .build();

        String response = client.newCall(request).execute().body().string();

        Map<String, Object> aiResponse = JSON_PARSER.parseMap(response);
        return new Term()
                .term(aiResponse.get("term").toString().trim())
                .model(aiResponse.get("model").toString());

    }

    /**
     * Describes a picture using the GenAI service for the provided picture file.
     *
     * @param pictureFile   The picture file containing the image related to the question.
     * @return An {@link Description} object containing the description and the model information.
     * @throws IOException If an I/O error occurs during the HTTP request or response processing.
     */
    public Description getDescription(File pictureFile) throws IOException {

        RequestBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", pictureFile.getName(), RequestBody.create(pictureFile, MediaType.parse("Binary data")))
                .build();

        HttpUrl httpUrl = HttpUrl.parse(genaiUrl + "/describe")
                .newBuilder()
                .build();

        Request request = new Request
                .Builder()
                .url(httpUrl)
                .post(requestBody)
                .build();

        String response = client.newCall(request).execute().body().string();

        Map<String, Object> aiResponse = JSON_PARSER.parseMap(response);
        return new Description()
                .description(aiResponse.get("description").toString().trim())
                .model(aiResponse.get("model").toString());

    }
    
    /**
     * Retrieves a document Wikidata entity links from the GenAI service for the provided PDF file.
     *
     * @param pdfFile The PDF file for which the entity links are requested.
     * @return A {@link EntityLinks} object containing the entity links information.
     * @throws IOException If an I/O error occurs during the HTTP request or response processing.
     */
    public EntityLinks getEntityLinksWikidata(File pdfFile) throws IOException {

    	LOG.debug("ai-applier GenAiClient getEntityLinksWikidata");

    	RequestBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", pdfFile.getName(), RequestBody.create(pdfFile, MediaType.parse("application/pdf")))
                .build();

        Request request = new Request
                .Builder()
                .url(genaiUrl + "/entitylink-wikidata")
                .post(requestBody)
                .build();

        String response = client.newCall(request).execute().body().string();
               
        Map<String, Object> aiResponse = JSON_PARSER.parseMap(response);
        
        String json = aiResponse.get("labels").toString();
        List<Object> objectList = JSON_PARSER.parseList(json);
		List<String> stringListLabels = objectList.stream()
		                                    .map(Object::toString)
		                                    .collect(Collectors.toList());

		json = aiResponse.get("links").toString();
        objectList = JSON_PARSER.parseList(json);
		List<String> stringListLinks = objectList.stream()
		                                    .map(Object::toString)
		                                    .collect(Collectors.toList());
		
		json = aiResponse.get("type_lists").toString();
        objectList = JSON_PARSER.parseList(json);
		List<String> stringListTypeLists = objectList.stream()
		                                    .map(Object::toString)
		                                    .collect(Collectors.toList());
	
        return new EntityLinks()
                .entityLabels(stringListLabels)
                .entityLinks(stringListLinks)
                .entityTypeLists(stringListTypeLists)
                .target("Wikidata");
    }

    /**
     * Retrieves a document DBpedia entity links from the GenAI service for the provided PDF file.
     *
     * @param pdfFile The PDF file for which the entity links are requested.
     * @return A {@link EntityLinks} object containing the entity links information.
     * @throws IOException If an I/O error occurs during the HTTP request or response processing.
     */
    public EntityLinks getEntityLinksDBpedia(File pdfFile) throws IOException {
    	
    	LOG.debug("ai-applier GenAiClient getEntityLinksDBpedia");

    	RequestBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", pdfFile.getName(), RequestBody.create(pdfFile, MediaType.parse("application/pdf")))
                .build();

        Request request = new Request
                .Builder()
                .url(genaiUrl + "/entitylink-dbpedia")
                .post(requestBody)
                .build();

        String response = client.newCall(request).execute().body().string();
               
        Map<String, Object> aiResponse = JSON_PARSER.parseMap(response);
        
        String json = aiResponse.get("labels").toString();
        List<Object> objectList = JSON_PARSER.parseList(json);
		List<String> stringListLabels = objectList.stream()
		                                    .map(Object::toString)
		                                    .collect(Collectors.toList());

		json = aiResponse.get("links").toString();
        objectList = JSON_PARSER.parseList(json);
		List<String> stringListLinks = objectList.stream()
		                                    .map(Object::toString)
		                                    .collect(Collectors.toList());
		
		json = aiResponse.get("type_lists").toString();
        objectList = JSON_PARSER.parseList(json);
		List<String> stringListTypeLists = objectList.stream()
		                                    .map(Object::toString)
		                                    .collect(Collectors.toList());

		return new EntityLinks()
                .entityLabels(stringListLabels)
                .entityLinks(stringListLinks)
                .entityTypeLists(stringListTypeLists)
                .target("DBpedia");      
    }

}
