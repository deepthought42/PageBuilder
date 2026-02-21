[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e2376d355755402aaa5bf7c533750851)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=deepthought42/PageBuilder&amp;utm_campaign=Badge_Grade)

# PageBuilder

Service responsible for building `PageState` and related objects from incoming Pub/Sub audit messages.

## Getting Started

### Launch JAR locally

#### Command Line Interface (CLI)

```bash
mvn clean install
java -ea -jar target/Look-see-#.#.#.jar
```

> NOTE: The `-ea` flag runs the JVM with assertions enabled.

### Docker

```bash
mvn clean install
docker build --tag look-see .
docker run -p 80:80 -p 8080:8080 -p 9080:9080 -p 443:443 --name look-see look-see
```

### Deploy Docker container to GCR

```bash
gcloud auth print-access-token | sudo docker login -u oauth2accesstoken --password-stdin https://us-central1-docker.pkg.dev
sudo docker build --no-cache -t us-central1-docker.pkg.dev/cosmic-envoy-280619/page-builder/#.#.# .
sudo docker push us-central1-docker.pkg.dev/cosmic-envoy-280619/page-builder/#.#.#

sudo docker build --no-cache -t us-central1-docker.pkg.dev/cosmic-envoy-280619/page-builder/page-builder .
sudo docker push us-central1-docker.pkg.dev/cosmic-envoy-280619/page-builder/page-builder
```

## Security

### Generating a new PKCS12 certificate for SSL

```bash
openssl pkcs12 -export -inkey private.key -in certificate.crt -out api_key.p12
```

## Testing

### Sending URL message

1. Log in to Google Cloud Console and navigate to Pub/Sub.
2. Under topics, find the URL topic and select **Messages**.
3. Send a message like:

```json
{
  "domainId": 1,
  "accountId": 5,
  "domainAuditRecordId": 11,
  "pageAuditId": -1,
  "url": "look-see.com"
}
```

## Code Review Summary (2026-02)

A targeted review was completed in this repo with emphasis on input safety, error handling, dead code, and documentation consistency.

### Fixes applied in this change set

1. **Input validation and defensive request handling**
   - Added explicit null/blank checks for `message.data` and now return `400 Bad Request` for malformed input.
   - Added handling for invalid Base64 / invalid JSON payloads before main processing begins.
2. **Logging improvements**
   - Replaced `printStackTrace()` with structured `log.error(..., e)` logging.
3. **Code cleanup**
   - Removed an unused helper method (`saveNewElements`) and an unused injected field.
   - Corrected typo in success response message (`verifed` -> `verified`).

### Follow-up items addressed in this update

1. Added a `400` response to the controller OpenAPI docs so the API contract matches runtime behavior for invalid payloads.

2. Refactored `AuditStartMessageSchema` to represent only decoded payload fields and no longer inherit from envelope schema types.

3. Removed duplicate `jackson-annotations` declaration from `pom.xml` to reduce dependency drift/noise.

4. Remaining recommendation: continue reducing explicit versions that are already managed by the Spring dependency BOM when repository dependency resolution is stable in CI.

## Migration notes

- 01-06-2023: Replace `isSecure` property with `secured` property in `PageState` objects.

```cypher
MATCH (n:PageState) SET n.secured=n.isSecure RETURN n
MATCH (n:PageState) SET n.isSecure=NULL RETURN n
```
