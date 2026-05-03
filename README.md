# Smart Campus Sensor & Room Management API

## Overview

This is the RESTful API for the university's "Smart Campus" initiative.  
It manages **rooms** and the **sensors** (CO₂, occupancy, lighting, etc.) inside them, along with historical sensor readings.  
The API is built with **JAX‑RS (Jakarta RESTful Web Services)** using **Jersey** and runs on **Apache Tomcat 10+**.

**Base URL:** `http://localhost:8080/SmartCampus/api/v1`

### Main features

- Room CRUD with business rule that a room with sensors cannot be deleted.
- Sensor registration with room validation.
- Filter sensors by type using query parameters.
- Nested sub‑resource for sensor readings (history + add new readings).
- Automated update of a sensor’s `currentValue` when a reading is added.
- Custom exception mappings (409, 422, 403, 500) with JSON error bodies.
- Request/response logging filter (method & URI, status code).

---

## How to build and run

### Prerequisites

- Java 17 or later
- Apache Maven 3.8+
- Apache Tomcat 10+ (Jakarta EE 10 compatible)

### Build

mvn clean package

### Deploy

Copy the generated `target/SmartCampus.war` to Tomcat's `webapps/` folder, then start Tomcat:

- macOS / Linux: `<Tomcat>/bin/startup.sh`
- Windows: `<Tomcat>/bin/startup.bat`

The API will be available at `http://localhost:8080/SmartCampus/api/v1`.

Alternatively, you can deploy directly from Apache NetBeans by adding Tomcat as a server and running the project.

---

## Sample curl commands

### 1. Discovery endpoint
```bash
curl -i http://localhost:8080/SmartCampus/api/v1

2. Create a room
curl -i -X POST http://localhost:8080/SmartCampus/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name":"Lecture Hall A","building":"Cavendish","floor":"Ground"}'

3. Create a sensor in an existing room
curl -i -X POST http://localhost:8080/SmartCampus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type":"CO2","roomId":"<room-id>","status":"ACTIVE","currentValue":420}'

4. Filter sensors by type
curl -i "http://localhost:8080/SmartCampus/api/v1/sensors?type=CO2"

5. Post a reading to a sensor
curl -i -X POST http://localhost:8080/SmartCampus/api/v1/sensors/<sensor-id>/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.1}'
```
(More comprehensive examples can be found in the video demonstration.)

## Part 1 – Service Architecture & Setup

### 1.1 JAX-RS Resource Lifecycle and Data Integrity

By default, a JAX-RS resource class is **request‑scoped**: the JAX‑RS runtime creates a new instance of the resource class for every incoming HTTP request. After the request is processed, that instance is discarded. This behaviour is specified in the Jakarta RESTful Web Services specification (formerly JAX‑RS).

An alternative lifecycle is **singleton**, which must be explicitly activated by annotating the resource class with `@jakarta.inject.Singleton` (or `@javax.inject.Singleton` in older versions). In that case the same instance services all requests.

**Impact on managing in‑memory data structures**

If resource classes are request‑scoped, any data stored in instance fields is lost between requests. Therefore shared state (such as the lists of rooms and sensors) **cannot** live inside the resource classes. The data must be kept in separate, long‑lived objects – typically in a **service layer** that is itself a singleton or is stored in a static field.

Because multiple HTTP requests can arrive concurrently, that shared state must be thread‑safe. A plain `HashMap` is not safe for concurrent reads and writes. Two common strategies are:

- Using **concurrent collections** such as `java.util.concurrent.ConcurrentHashMap` and `CopyOnWriteArrayList`, which handle thread‑safety internally and perform well under high concurrency.
- **Synchronising manual access** using `synchronized` blocks or `ReentrantReadWriteLock` around the maps and lists to ensure atomic updates.

Even if resource classes are singletons and store the data in their own fields, the same concurrency issue exists because multiple request threads can call the singleton simultaneously. Thus the choice between request‑scoped and singleton only affects **where** the shared state lives, not **whether** synchronisation is required.

For this project I will use a separate service class that holds the maps and lists as `ConcurrentHashMap` instances, guaranteeing that the API remains correct under parallel access without loss or corruption of data.

### 1.2 HATEOAS – Hypermedia as the Engine of Application State

HATEOAS is one of the key constraints of the REST architectural style. It means that a REST API response should contain not just data but also **hypermedia links** that guide the client through available next actions. For example, a “discovery” response like the one implemented at `/api/v1` provides links to `/api/v1/rooms` and `/api/v1/sensors`, enabling a client to navigate the API without prior knowledge.

**Benefits for client developers compared to static documentation:**

1. **Self‑discoverability** – A client can start from the root endpoint and explore the entire API dynamically. There is no need to hard‑code URL templates; the server tells the client exactly where each resource is located. This is analogous to how a web browser navigates a website by following links.
2. **Resilience to URL changes** – If the server’s URL structure changes (for example, the rooms endpoint moves from `/api/v1/rooms` to `/api/v1/campus/rooms`), a HATEOAS‑aware client simply follows the updated links in the discovery response. An offline static document would require manual updates and recompilation of the client.
3. **Reduced client logic** – The client does not need to remember complex URL patterns or construct them from parameters. The valid transitions and required parameters are embedded in the representation, which reduces the chance of misconstruction and errors.
4. **Documentation that stays in sync** – Static API documents often become outdated. Hypermedia links are generated live by the server, so the “documentation” is always exactly what the server actually supports.

In short, HATEOAS turns an API from a fixed set of endpoints into a dynamic, navigable service, which is the hallmark of a mature RESTful design.

---

## Part 2 – Room Management

### 2.1 Room resource implementation

When returning a list of rooms, there is a clear trade‑off between returning only room IDs and returning full room objects.

- **Returning only IDs** minimizes the response size and saves bandwidth. However, the client must then make a separate request for each room to obtain the full details, which can dramatically increase both latency and server load in a system with thousands of rooms.
- **Returning full objects** in a single response gives the client everything it needs immediately, reducing the number of round trips and client‑side processing. The downside is a larger payload, which can be problematic for very large collections or low‑bandwidth networks.

For the Smart Campus API, returning the full room objects is the better choice because the room data is small (a few KB per room) and the number of rooms is unlikely to be huge. It simplifies client integration and avoids the “N+1 problem” that would occur with an ID‑only list. If the project later scaled to millions of rooms, pagination and the option to request limited fields (e.g., via `?fields=id,name`) would be the appropriate evolution.

### 2.2 DELETE idempotency

The `DELETE /rooms/{roomId}` operation is **idempotent** in my implementation. Idempotent means that making the same request multiple times has the same effect as making it once.

When the client sends `DELETE /rooms/123`:
- **First request**: If the room exists and has no sensors, the room is deleted and `204 No Content` is returned. The server state changes from “room exists” to “room does not exist”.
- **Second identical request**: The room no longer exists, so the server returns `404 Not Found`. The state remains “room does not exist”. No further change occurs.
- **Third identical request**: The same `404` is returned.

Although the response status code differs between the first and subsequent attempts (204 vs 404), the **side effect on the resource** is the same: after the first successful deletion, the resource is gone and remains gone. According to HTTP semantics, `DELETE` is idempotent; the standard specifically notes that multiple `DELETE` calls may produce different response codes without violating idempotency, as long as the effect on the serviced resource is identical.

If the room still has active sensors, every identical `DELETE` call results in a `409 Conflict` without any state change, reinforcing the idempotent nature.

---

## Part 3 – Sensors & Filtering

### 3.1 Sensor integrity – Content-Type enforcement

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method tells JAX‑RS that this resource method only accepts requests whose `Content-Type` header is `application/json`. If a client sends a request with a different content type (e.g. `text/plain` or `application/xml`), JAX‑RS will examine the incoming request and realise that no resource method matches the supplied media type. Because no method can process the request, the framework automatically returns an HTTP **415 Unsupported Media Type** status to the client. Internally, JAX‑RS uses the `@Consumes` value during the request‑matching phase; if the provided content type is not acceptable for any matching method, the request fails before any resource method is invoked. This is done transparently – you do not need to write extra validation code. The client receives a clear signal that its payload format is not supported, which helps API consumers to correct their requests. Additionally, if the client omits the `Content-Type` header altogether, the server may treat it as a generic request and still result in 415 because no media type matches the requirement.

### 3.2 Filtered retrieval – @QueryParam vs. path parameter

The optional `?type=` query parameter is better suited for filtering a collection than embedding the filter value in the URL path (e.g. `/sensors/type/CO2`). The main reasons are:

- **Semantics** – The path segment identifies a resource; sub‑path `/type/CO2` would imply that “CO2” is a distinct sub‑resource, which is not true. Query parameters, on the other hand, describe how to process the same collection resource – they modify the view, not the identity of the resource.
- **Flexibility and combinability** – Query parameters can be easily combined without creating deeply nested or ambiguous URLs (e.g. `?type=CO2&status=ACTIVE&roomId=42`). Adding multiple path segments quickly complicates the URL structure and binding.
- **Optionality** – A query parameter can be completely omitted; the same endpoint returns the full list. With path segments, omitting the filter would require separate endpoints or complex path matching.
- **Standardisation** – The widespread convention for search, filters, and pagination is to use query parameters, making the API more intuitive for developers.

Thus, `@QueryParam` keeps the resource model clean, supports extendable filtering, and adheres to REST best practices.

---

## Part 4 – Sub‑Resources

### 4.1 Sub-Resource Locator pattern – Architectural benefits

The Sub‑Resource Locator pattern allows a JAX‑RS resource class to delegate handling of a sub‑path to a separate class, rather than defining all nested endpoints in one monolithic controller. For instance, a locator method with `@Path("/{sensorId}/readings")` returns an instance of `SensorReadingResource`, which then handles further HTTP methods on that path.

**Benefits in large APIs:**
- **Separation of concerns** – Each resource class deals only with its own data and logic. `SensorResource` handles sensor collection, while `SensorReadingResource` handles reading history, making the codebase easier to understand and maintain.
- **Modularity and testability** – Smaller, focused classes can be unit‑tested independently. Mocking or replacing one part of the hierarchy does not affect the others.
- **Scalability in development** – Different developers can work on different parts of the URL space without merge conflicts.
- **Cleaner code** – Without sub‑resource locators, a single class would contain methods for `/sensors`, `/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`, creating a “god class” that is hard to navigate. Delegation keeps endpoint logic organised.
- **Flexibility in lifecycle** – The returned sub‑resource instance can carry context (e.g., the parent sensor ID) via its constructor, which is cleaner than repeating `@PathParam` in every method.

### 4.2 Historical Data Management – Side‑effect of updating currentValue

In my implementation, a successful `POST` to `/sensors/{id}/readings` triggers a side‑effect: the parent `Sensor` object’s `currentValue` field is updated to the reading’s value. This keeps the API’s representation of the sensor’s most recent state consistent with the newly submitted reading.

This design mimics real‑world IoT scenarios where the latest telemetry value is always immediately available from the device (or sensor) resource, not hidden deep in the history collection. It improves API efficiency because a client that only needs the current value does not have to fetch the entire reading history. The update is performed atomically within the `DataStore` service, ensuring thread‑safety.

---

## Part 5 – Advanced Error Handling, Exception Mapping & Logging

### 5.2 Why HTTP 422 is more semantically accurate than 404 for a missing dependent resource

When a client sends a perfectly valid JSON payload (e.g. to create a sensor) but the `roomId` within that payload references a non‑existent room, the problem is **not** that the target URI is invalid (which would be a 404). The request was syntactically correct and understood by the server; the issue is semantic – the dependency cannot be satisfied. HTTP 422 Unprocessable Entity explicitly signals that the server understands the content type and the syntax is correct, but it cannot process the contained instructions. This gives the client a precise indication: the request body is fine, but the referenced resource does not exist. A 404 would misleadingly suggest that the entire `/sensors` endpoint is not found or that the request URI itself is wrong, which is inaccurate. Thus, 422 provides a cleaner and more meaningful error for API consumers.

### 5.4 Cybersecurity risks of exposing internal Java stack traces

Returning a raw Java stack trace to an external API consumer is a serious security vulnerability. It reveals:

- Internal file paths and package names, disclosing the server’s directory structure and technology stack.
- Class names and methods involved in the execution chain, which helps an attacker map the application’s architecture.
- Framework and library versions (visible in trace lines), enabling an attacker to search for known vulnerabilities targeting those specific versions.
- Database or middleware hints if they appear in the trace.
- Potentially sensitive logic or business rules embedded in error messages.

Armed with this information, an attacker can probe for weaknesses, craft injection attacks, or exploit unpatched vulnerabilities. A generic 500 error with no stack trace eliminates this information leakage while still informing the client that a server error occurred.

### 5.5 Advantages of JAX‑RS filters for cross‑cutting concerns like logging

Using JAX‑RS `ContainerRequestFilter` and `ContainerResponseFilter` for logging is far superior to manually placing `Logger.info()` calls inside every resource method:

- **Separation of concerns** – Logging logic is completely decoupled from business logic, keeping resource classes clean and focused on their primary tasks.
- **Reusability** – The same filter automatically applies to every endpoint without any code duplication. Changes to logging format or behaviour need only be made in one place.
- **Transparency** – Developers adding new endpoints do not need to remember to add logging statements; the filter guarantees that every request and response is logged.
- **Consistency** – The request/response information is logged in a uniform manner, regardless of who wrote the resource method.
- **Non‑invasive** – The filter can be enabled, disabled, or modified without altering any resource code, simplifying maintenance.

In essence, filters implement the decorator pattern at the JAX‑RS engine level, providing a clean, declarative way to address cross‑cutting concerns.

---

## Explanatory notes

- All responses are returned in JSON format.
- The API uses standard HTTP status codes and includes custom error bodies for 409, 422, 403, and 500.
- The project adheres to the REST architectural style and uses HATEOAS in the discovery endpoint.
- All data is stored in memory (no database) to keep the implementation simple.