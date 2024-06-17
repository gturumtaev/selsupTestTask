package org.sign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Клиент для работы с API Честного знака
 */
public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient httpClient;
    private final TimeUnit timeUnit;
    private final long duration;
    private final int requestLimit;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private Logger logger;

    /**
     * Создает экземпляр CrptApi, который реализует ограничение на количество запросов к API.
     * @param timeUnit Единица измерения времени, используемая для определения интервала сброса разрешений.
     * @param duration Длительность интервала времени, после которого семафор будет сбрасывать разрешения.
     * @param requestLimit Максимальное количество разрешений (запросов), доступных для отправки за один интервал времени.
     * @param semaphore
     * @param scheduler
     * @param httpClient
     */
    public CrptApi(HttpClient httpClient, TimeUnit timeUnit, long duration, int requestLimit, Semaphore semaphore, ScheduledExecutorService scheduler) {
        if (requestLimit <= 0)
            throw new IllegalArgumentException("Число, указывающее количество запросов, должно быть положительным");
        this.httpClient = httpClient;
        this.timeUnit = timeUnit;
        this.duration = duration;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(() -> semaphore
                .release(requestLimit - semaphore.availablePermits()), 0, duration, timeUnit);
    }

    /**
     * Метод для отправки документа к API, управляет ограничениями доступа через семафор.
     * Приостанавливает выполнение, если количество одновременных запросов достигло своего предела.
     * @param document Документ для отправки.
     * @param signature Подпись документа.
     * @throws IOException
     * @throws InterruptedException если поток был прерван во время ожидания разрешения семафора.
     */
    public void createDocument (Document document, String signature) throws IOException, InterruptedException {
        String jsonDocument = createJsonDocument(document);
        HttpRequest httpRequest = buildHttpRequest(jsonDocument, signature);
        semaphore.acquire();
        handleResponse(httpRequest);
    }

    /**
     * Метод для генерации http запроса.
     * @param jsonDocument JSON представление документа.
     * @param signature Подпись для аутенфикации запроса.
     * @return
     */
    private HttpRequest buildHttpRequest (String jsonDocument, String signature) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                .build();
    }

    /**
     * Метод для подачи семафору новых запросов.
     * @param httpRequest
     * @throws IOException
     * @throws InterruptedException
     */
    private void handleResponse(HttpRequest httpRequest) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) logger.info("Response received successfully");
        else logger.warning("Response with status code " + response.statusCode());
    }

    /**
     * Метод для перевода документа в JSON в формат.
     * @param document
     * @throws JsonProcessingException
     */
    private String createJsonDocument(Document document) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(document);
    }

    /**
     * Класс документа
     */
    public static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products;
        private String regDate;
        private String regNumber;


        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public String getRegDate() {
            return regDate;
        }

        public void setRegDate(String regDate) {
            this.regDate = regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }
    }

    /**
     * Класс описания документа
     */
    public static class Description {
        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    /**
     * Класс продукта документа
     */
    public static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uidCode;
        private String uituCode;
        private String regDate;
        private String regNumber;

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public String getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(String certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getUidCode() {
            return uidCode;
        }

        public void setUidCode(String uidCode) {
            this.uidCode = uidCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }

        public String getRegDate() {
            return regDate;
        }

        public void setRegDate(String regDate) {
            this.regDate = regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }
    }
}

