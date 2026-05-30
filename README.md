# spring-ai

A Spring Boot reference app for building production AI features with Spring AI 2.x — chat memory, structured output, tool calling, and a **multi-model router** that sends cheap queries to a local model and only escalates the hard ones to a frontier cloud model, tracking the cost of every call.

It talks to models over the **Anthropic Messages protocol**: a local model served by [LM Studio](https://lmstudio.ai/) for the everyday traffic, and **Claude (cloud)** for the queries that actually need it.

## What's inside

### Spring AI fundamentals (`/`)

- **Conversational chat with persistent memory** — a rolling 10-message window backed by PostgreSQL via `spring-ai-starter-model-chat-memory-repository-jdbc`, scoped per `conversationId` so multiple users hold independent conversations.
- **Structured output from free-form prompts** — a `/review` endpoint that takes raw source code and returns a typed `CodeReview` record (`summary`, `issues`, `suggestions`, `qualityScore`) parsed directly from the model response.
- **Tool calling with `@Tool`** — a `/chat-with-tools` endpoint wired to a `WeatherTool` the model can invoke on demand, demonstrating how Spring AI bridges chat prompts to plain Java methods.

### Multi-model cost router (`/ai`)

The newest layer. A `QueryRouter` inspects each prompt and decides which tier should answer it:

- **LOCAL** (default) — short, routine queries go to the local LM Studio model. Free.
- **CLOUD** — prompts longer than 500 characters, or containing a "hard" keyword (`architecture`, `design`, `refactor`, `security`, `performance`, `scalability`, `tradeoff`, `compare`, `analyze`, `best practice`), escalate to Claude Opus.

Every routed call is metered by a `CostTracker`, which accumulates request counts, token usage, and dollar cost per tier and keeps a log of recent routing decisions. A live dashboard visualizes the running total and how much the router saved versus sending everything to the cloud.

The cloud pricing baked into the routing config is Claude Opus list price — **$15.00 / million input tokens, $75.00 / million output tokens** — while the local tier is priced at **$0**, so the cost numbers on the dashboard reflect real money saved.

## Why this exists

Part of my **AI Baltics** content — practical AI for Baltic developers. Most Spring AI material stops at "hello world, here's a `ChatClient`." This repo goes a layer deeper into what real apps need: memory, structure, tools — and then the architectural pattern that makes self-hosted AI economical, routing the easy 80% to a local model and paying frontier prices only for the 20% that earns it.

## The interesting bit

Structured output turns the model into a typed API. Define a record, ask for it, get it back:

```java
public record CodeReview(
    String summary,
    List<String> issues,
    List<String> suggestions,
    int qualityScore
) {}

public CodeReview codereview(String code) {
    return chatClient.prompt()
            .user("Review this code. Be specific and concise: " + code)
            .call()
            .entity(CodeReview.class);
}
```

No prompt engineering for JSON, no manual parsing, no "please respond in the following format." Spring AI handles schema negotiation and deserialization.

And routing is just as small — one client per tier, one switch on the decision:

```java
RoutingDecision decision = router.route(prompt);
ChatClient client = (decision.tier() == ModelTier.LOCAL) ? localClient : cloudClient;
ChatResponse response = client.prompt(prompt).call().chatResponse();
tracker.record(decision, inputTokens, outputTokens);
```

## Endpoints

| Method & path          | What it does                                                              |
|------------------------|---------------------------------------------------------------------------|
| `GET  /chat`           | Chat with persistent memory (`message`, `conversationId`)                  |
| `POST /review`         | Structured code review → typed `CodeReview` JSON                          |
| `GET  /chat-with-tools`| Chat with the `WeatherTool` available for tool calling (`message`)        |
| `POST /ai/route`       | Route a prompt to LOCAL or CLOUD and return the answer + routing decision  |
| `GET  /ai/costs`       | Cost snapshot: per-tier requests, tokens, dollars, and recent decisions   |
| `POST /ai/costs/reset` | Reset the cost counters                                                    |
| `GET  /dashboard.html` | Live cost dashboard                                                        |

## Tech

- **Java 21**, **Spring Boot 4.0.2**, **Spring AI 2.0.0-M2**
- `spring-ai-starter-model-anthropic` for both tiers (local LM Studio speaks the Anthropic Messages protocol)
- PostgreSQL 17 for chat-memory persistence (auto-schema on startup)

## Run it locally

**Prerequisites:**

- Java 21 and Docker
- [LM Studio](https://lmstudio.ai/) running a local model on the Anthropic API at `http://127.0.0.1:1234` (default config expects the `google/gemma-4-e2b` model). LM Studio requires a token even for local calls — any non-empty value works.
- An Anthropic API key for the cloud tier.

1. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```

2. Export your keys (PowerShell shown; use `export` on macOS/Linux):
   ```powershell
   $env:LM_STUDIO_API_KEY = "lm-studio"      # any non-empty token
   $env:ANTHROPIC_API_KEY = "sk-ant-..."     # real Anthropic key for the cloud tier
   ```

3. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```

4. Try the endpoints:
   ```bash
   # Chat with memory — same conversationId remembers context
   curl "http://localhost:8080/chat?message=My+name+is+Dmitriy&conversationId=demo"
   curl "http://localhost:8080/chat?message=What+is+my+name?&conversationId=demo"

   # Structured code review
   curl -X POST http://localhost:8080/review \
        -H "Content-Type: text/plain" \
        -d 'public int add(int a, int b) { return a - b; }'

   # Tool calling
   curl "http://localhost:8080/chat-with-tools?message=What+is+the+weather+in+Riga?"

   # Multi-model routing — short query stays LOCAL, "security" escalates to CLOUD
   curl -X POST http://localhost:8080/ai/route \
        -H "Content-Type: text/plain" \
        -d 'Does this method handle null correctly?'
   curl -X POST http://localhost:8080/ai/route \
        -H "Content-Type: text/plain" \
        -d 'Review the security implications of this authentication flow.'

   # Cost snapshot
   curl http://localhost:8080/ai/costs
   ```

5. Open the dashboard at **http://localhost:8080/dashboard.html** to watch costs accumulate.

The JDBC schema for chat memory is auto-initialized on startup (`initialize-schema: always`), so there's no manual migration step.

## Demo runner

`demo_runner.py` replays a scripted set of queries (`demo-queries.txt`) through `/ai/route` so you can watch the router make decisions and the dashboard fill in — handy for recording. Pure Python 3 stdlib, no dependencies:

```bash
python3 demo_runner.py --reset            # clear counters, then run all queries
python3 demo_runner.py --pause 5          # slower pacing for screen recording
```

The queries file is labelled with each expected tier, so you can confirm the router behaves as intended at a glance.
