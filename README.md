# Alfresco integration with Generative AI and spaCy nlp

This project provides a collection of resources to enable the utilization of Private Generative AI in conjunction with Alfresco. Each service within the project is designed to operate locally, offering flexibility for usage in a development environment.

The primary scenarios covered by this project are centered around a document:

* Summarize a document in any language and recognize various tags
* Select a term from a provided list that characterizes a document
* Answer to a question related to the document

In addition, it includes a use case related to images:

* Provide a description of a picture


## Requirements

Following tools can be used to build and deploy this project:

* [Docker 4.25](https://docs.docker.com/get-docker/) (with 20 GB of RAM allocated)
* [ollama](https://ollama.ai/)
* [Java 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
* [Maven 3.9](https://maven.apache.org/download.cgi)

>> Deploying this project in a production environment could require additional steps to ensure minimal performance and successful execution of actions


## Description

The project includes following components:

* [genai-stack](genai-stack) folder is using https://github.com/docker/genai-stack project to build a REST endpoint that provides AI services for a given document
* [alfresco](alfresco) folder includes a Docker Compose template to deploy Alfresco Community 23.1
* [alfresco-ai](alfresco-ai) folder includes a set of projects
  * [alfresco-ai-model](alfresco-ai/alfresco-ai-model) defines a custom Alfresco content model to store summaries, terms and prompts to be deployed in Alfresco Repository and Share App
  * [alfresco-ai-applier](alfresco-ai/alfresco-ai-applier) uses the Alfresco REST API to apply summaries or terms for a populated Alfresco Repository based on the application of the `genai:summarizable` aspect
  * [alfresco-ai-listener](alfresco-ai/alfresco-ai-listener) listens to messages and generates summaries, apply terms and reply answers for create or updated nodes in Alfresco Repository
* [compose.yaml](compose.yaml) file describes a deployment for Alfresco and GenAI Stack services using `include` directive

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                                            │
│                        ┌─Compose────────────────────┐          ┌─Compose────────────────────┐              │
│                        │                            │          │                            │              │
│                        │       A L F R E S C O      │          │     G E N  A I  Stack      │              │
│                        │                            │          │                            │              │
│                        │ ┌──────────┐ ┌───────────┐ │          │ ┌───┐ ┌─────┐ ┌─────────┐  │              │
│           ┌────────────┤ │model-repo│ │model-share│ │          │ │llm│ │neo4j│ │langchain│  │              │
│           │            │ └──────────┘ └───────────┘ │          │ └───┘ └─────┘ └─────────┘  │              │
│           │            │                            │          │  ollama                    │              │
│           │            └──────────────▲─────────────┘          └─────────────▲──────────────┘              │
│           │                           │                                      │                             │
│           │                           │ http://alfresco:8080                 │ http://genai:8506/summary   │
│           │            ┌─App──────────┴─────────────┐                        │ http://genai:8506/classify  │
│           │            │                            │                        │ http://genai:8506/prompt    │
│           │            │   alfresco-ai-applier      ├────────────────────────┤ http://genai:8506/describe  │
│           │            │                            │                        │                             │
│           │            └──────────────┬─────────────┘                        │                             │
│           │                           │                                      │                             │
│           │                           │                                      │                             │
│           │            ┌─Service──────┴─────────────┐                        │                             │
│           │            │                            │                        │                             │
│           └────────────►   alfresco-ai-listener     ├────────────────────────┘                             │
│   tcp://activemq:61616 │                            │                                                      │
│                        └────────────────────────────┘                                                      │
│                                                                                                            │
└────────────────────────────────────────────────────────────────────────────────────────DOCKER NETWORK──────┘
```


## GenAI Stack

This service, available in [genai-stack](genai-stack) folder, offers various REST endpoints for applying AI operations to a given document.

```
 ┌─Compose────────────────────┐              
 │                            │              
 │     G E N  A I  Stack      │              
 │                            │              
 │ ┌───┐ ┌─────┐ ┌─────────┐  │              
 │ │llm│ │neo4j│ │langchain│  │              
 │ └───┘ └─────┘ └─────────┘  │              
 │  ollama                    │              
 └─────────────▲──────────────┘              
               │                             
               │ http://genai:8506/
```

* Summarizing a document and extracting tags from it

```bash
curl --location 'http://localhost:8506/summary' --form 'file=@"./file.pdf"'

{
    "summary": " The text discusses...",
    "tags": " Golang, Merkle, Difficulty",
    "model": "mistral"
}
```

* Selecting a term from a list that best matches the document

```bash
curl --location 'http://localhost:8506/classify?termList="Japanese,Spanish,Korean,English,Vietnamese"' --form 'file=./file.pdf"'

{
    "term": " English",
    "model": "mistral"
}
```

* Responding to questions related to the document

```bash
curl --location 'http://localhost:8506/prompt?prompt="What is the name of the son?"' --form 'file=./file.pdf"'

{
    "answer": "The name of the son is Musuko.",
    "model": "mistral"
}
```

* Describing a picture

```bash
curl --location 'http://localhost:8506/describe' --form 'image=@"file.jpg"'

{
    "description": " In the image, a man with a beard is standing in an indoor setting.",
    "model": "llava"
}
```

### Configuration

Modify `.env` file values:

```
# Choose any of the on premise models supported by ollama
LLM=mistral
LLM_VISION=llava

 # Any language name supported by chosen LLM
SUMMARY_LANGUAGE=English
# Number of words for the summary
SUMMARY_SIZE=120
# Number of tags to be identified with the summary
TAGS_NUMBER=3 
```

>> Note that LLM_VISION must be a LLM with vision encoder

## Alfresco

Alfresco service, available in [alfresco](alfresco) folder, includes custom content model definition and additional events configuration.

```
 ┌─Compose────────────────────┐
 │                            │
 │       A L F R E S C O      │
 │                            │
 │ ┌──────────┐ ┌───────────┐ │
 │ │model-repo│ │model-share│ │
 │ └──────────┘ └───────────┘ │
 │                            │
 └──────────────▲─────────────┘
                │              
                │ http://alfresco:8080
```

* Content Model for Repository is available in [genai-model-repo](alfresco-ai/alfresco-ai-model/genai-model-repo)
  * `genai:summarizable` aspect is used to store `summary` and `tags` generated with AI
  * `genai:promptable` aspect is used to store the `question` provided by the user and the `answer` generated with AI
  * `genai:classifiable` aspect is used to store the list of terms available for the AI to classify a document. It should be applied to a folder
  * `genai:classified` aspect is used to store the term selected by the AI. It should be applied to a document
  * `genai:descriptable` aspect is used to store the description generated with AI. It should be applied to a picture

* Forms and configuration to handle custom Content Model from Share are available in [genai-model-share](alfresco-ai/alfresco-ai-model/genai-model-share)

* Additional configuration for Repository
  * Since `alfresco-ai-listener` is listening to renditions, default `event2` filter should be modified. Following configuration has been added to `alfresco` service in `compose.yaml`
```
-Drepo.event2.filter.nodeTypes="sys:*, fm:*, cm:failedThumbnail, cm:rating, rma:rmsite include_subtypes, usr:user"
```  


## Alfresco AI Applier

This Spring Boot application utilizes the Alfresco REST API to fetch all documents from a given Alfresco folder and apply a single action:

* The `Summarizing` action involves retrieving documents from a folder using the Alfresco Search API, checking for the availability of PDF renditions, and updating document nodes with summaries obtained from the GenAi service.
* The `Classifying` action retrieves documents from a folder using the Alfresco Search API, checks for the availability of PDF renditions, and updates document nodes by selecting a term, from a list of terms using the GenAi service.
* The `Describing` actions retrieves pictures from a folder using the Alfresco Search API and updates image nodes with descriptions obtained from the GenAi service.

```
 ┌─Compose────────────────────┐          ┌─Compose────────────────────┐          
 │                            │          │                            │          
 │       A L F R E S C O      │          │     G E N  A I  Stack      │          
 │                            │          │                            │          
 │ ┌──────────┐ ┌───────────┐ │          │ ┌───┐ ┌─────┐ ┌─────────┐  │          
 │ │model-repo│ │model-share│ │          │ │llm│ │neo4j│ │langchain│  │          
 │ └──────────┘ └───────────┘ │          │ └───┘ └─────┘ └─────────┘  │          
 │                            │          │  ollama                    │          
 └──────────────▲─────────────┘          └─────────────▲──────────────┘          
                │                                      │                         
                │ http://alfresco:8080                 │ http://genai:8506/summary
 ┌─App──────────┴─────────────┐                        │ http://genai:8506/classify
 │                            │                        │ http://genai:8506/describe
 │   alfresco-ai-applier      ├────────────────────────┘
 │                            │                        
 └────────────────────────────┘                        
```

### Configuration

Modify property values in `application.properties` file:

```
# Spring Boot properties
# Disable Spring Boot banner
spring.main.banner-mode=off

# Logging Configuration
logging.level.org.springframework=ERROR
logging.level.org.alfresco=INFO
logging.pattern.console=%msg%n

# Alfresco Server Configuration
# Basic authentication credentials for Alfresco Server
content.service.security.basicAuth.username=admin
content.service.security.basicAuth.password=admin
# URL and path for Alfresco Server API
content.service.url=http://localhost:8080
content.service.path=/alfresco/api/-default-/public/alfresco/versions/1

# Alfresco Repository Content Model (Summary)
# Aspect that triggers the summarization task
content.service.summary.aspect=genai:summarizable
# Node property to store the summary obtained from GenAI Stack
content.service.summary.summary.property=genai:summary
# Node property to store tags obtained from GenAI Stack; use TAG as a value to use a tag instead of a property
content.service.summary.tags.property=genai:tags
# Node property to store the Large Language Model (LLM) used; use TAG as a value to use a tag instead of a property
content.service.summary.model.property=genai:llmSummary

# Alfresco Repository Content Model (Classify)
# Node property that includes terms for classification
content.service.classify.terms.property=genai:terms
# Aspect that enables classification task
content.service.classify.aspect=genai:classified
# Node property to fill with the term
content.service.classify.term.property=genai:term
# Node property to fill with the model
content.service.classify.model.property=genai:llmClassify

# Alfresco Repository Content Model (Description)
# Aspect that triggers the description task
content.service.description.aspect=genai:descriptable
# Node property to store the description obtained from GenAI Stack
content.service.description.description.property=genai:description
# Node property to store the Large Language Model (LLM) used; use TAG as a value to use a tag instead of a property
content.service.description.model.property=genai:llmDescription

# GenAI Client Configuration
# Host URL for the Document GenAI service
genai.url=http://localhost:8506
# Request timeout in seconds for GenAI service requests
genai.request.timeout=1200

# Alfresco AI Applier Configuration
# Root folder in Alfresco Repository to apply GenAI action
applier.root.folder=/app:company_home/app:shared
# Choose one action: SUMMARY, CLASSIFY, DESCRIBE
applier.action=SUMMARY
# List of terms to be applied for CLASSIFY action (ignored when using SUMMARY action)
applier.action.classify.term.list=English,Spanish,Japanese,Vietnamese
# Maximum number of items to be retrieved from Alfresco Repository in each iteration
request.max.items=20
```

Configuration parameters can be also used as command line arguments or Docker environment variables, like in the following sample:

```bash
$ java -jar target/alfresco-ai-applier-0.8.0.jar \
--applier.root.folder=/app:company_home/app:shared/cm:picture \
--applier.action=DESCRIBE \
--logging.level.org.alfresco=DEBUG
```


## Alfresco AI Listener

This Spring Boot application is designed to capture summary, classification, or prompting aspect settings by listening to ActiveMQ events. The application then forwards the request to the GenAI Stack and subsequently updates the Alfresco Node using the REST API.

```
              ┌─Compose────────────────────┐          ┌─Compose────────────────────┐
              │                            │          │                            │
              │       A L F R E S C O      │          │     G E N  A I  Stack      │
              │                            │          │                            │
              │ ┌──────────┐ ┌───────────┐ │          │ ┌───┐ ┌─────┐ ┌─────────┐  │
 ┌────────────┤ │model-repo│ │model-share│ │          │ │llm│ │neo4j│ │langchain│  │
 │            │ └──────────┘ └───────────┘ │          │ └───┘ └─────┘ └─────────┘  │
 │            │                            │          │  ollama                    │
 │            └──────────────▲─────────────┘          └─────────────▲──────────────┘
 │ tcp://activemq:61616      │                                      │               
 │                           │ http://alfresco:8080                 │ http://genai:8506/summary
 │            ┌─Service──────┴─────────────┐                        │ http://genai:8506/classify
 │            │                            │                        │ http://genai:8506/prompt
 └────────────►   alfresco-ai-listener     ├────────────────────────┘ http://genai:8506/describe                        
              │                            │                                                  
              └────────────────────────────┘                                                  
```

### Configuration

Modify property values in `application.properties` file or use Docker environment setings:

```
# Spring Boot properties
spring.main.banner-mode=off
logging.level.org.springframework=ERROR
logging.level.org.alfresco=INFO
logging.pattern.console=%msg%n

# Alfresco Server Configuration
# Basic authentication credentials for Alfresco Server
content.service.security.basicAuth.username=admin
content.service.security.basicAuth.password=admin
# URL and path for Alfresco Server API
content.service.url=http://localhost:8080
content.service.path=/alfresco/api/-default-/public/alfresco/versions/1


# Alfresco Repository Content Model (Summary)
# Aspect that triggers the summarization task
content.service.summary.aspect=genai:summarizable
# Node property to store the summary obtained from GenAI Stack
content.service.summary.summary.property=genai:summary
# Node property to store tags obtained from GenAI Stack; use TAG as a value to use a tag instead of a property
content.service.summary.tags.property=genai:tags
# Node property to store the Large Language Model (LLM) used; use TAG as a value to use a tag instead of a property
content.service.summary.model.property=genai:llmSummary

# Alfresco Repository Content Model (Prompt)
# Aspect that enables prompt task
content.service.prompt.aspect=genai:promptable
# Node property that contains a question
content.service.prompt.question.property=genai:question
# Node property to fill with the answer
content.service.prompt.answer.property=genai:answer
# Node property to fill with the model
content.service.prompt.model.property=genai:llmPrompt

# Alfresco Repository Content Model (Classify)
# Node property that includes terms for classification
content.service.classify.terms.property=genai:terms
# Aspect that enables classification task
content.service.classify.aspect=genai:classified
# Node property to fill with the term
content.service.classify.term.property=genai:term
# Node property to fill with the model
content.service.classify.model.property=genai:llmClassify

# Alfresco Repository Content Model (Description)
# Aspect that triggers the description task
content.service.description.aspect=genai:descriptable
# Node property to store the description obtained from GenAI Stack
content.service.description.description.property=genai:description
# Node property to store the Large Language Model (LLM) used; use TAG as a value to use a tag instead of a property
content.service.description.model.property=genai:llmDescription


# ActiveMQ Server
spring.activemq.brokerUrl=tcp://localhost:61616
spring.jms.cache.enabled=false
alfresco.events.enableSpringIntegration=false
alfresco.events.enableHandlers=true

# GenAI Client Configuration
# Host URL for the Document GenAI service
genai.url=http://localhost:8506
# Request timeout in seconds for GenAI service requests
genai.request.timeout=1200
```

Configuration parameters can be also used as command line arguments or Docker environment variables, like in the following sample:

```bash
$ java -jar target/alfresco-ai-listener-0.8.0.jar --logging.level.org.alfresco=DEBUG
```

# Use Case 1: Existing Content

0. Before proceeding, ensure that Docker, ollama, Java, and Maven are up and working:

```sh
$ ollama -v
ollama version is 0.1.31
$ docker -v
Docker version 25.0.3, build 4debf41
$ java -version
openjdk version "17.0.5" 2022-10-18
$ mvn -v
Apache Maven 3.9.6
```

1. Verify that `compose-ai.yaml` is commented in [compose.yaml](compose.yaml)

```sh
$ cat compose.yaml
include:
  - genai-stack/compose.yaml
  - alfresco/compose.yaml
#  - alfresco/compose-ai.yaml
```

2. Start Docker containers for Alfresco and GenAI Stack

```sh
$ docker compose up
```

3. Once Alfresco is up & running, upload a number of documents to a given folder, for instance `/app:company_home/app:shared`. You may use the Legacy UI with default credentials (admin/admin) available in http://localhost:8080/share

4. Compile the Alfresco AI Applier (if required)

```sh
$ cd alfresco-ai/alfresco-ai-applier
$ mvn clean package
```

5. Run the Alfresco AI Applier to summarize the documents in a given folder

```sh
$ java -jar target/alfresco-ai-applier-0.8.0.jar \
  --applier.root.folder=/app:company_home/app:shared \
  --applier.action=SUMMARY
```

>> Once this command has finished, every document in the folder should include a populated `Summary` property (accessible in "view" mode)

6. Run the Alfresco AI Applier to classify the documents based on a list of terms in a specific folder

```sh
$ cd alfresco-ai/alfresco-ai-applier
$ java -jar target/alfresco-ai-applier-0.8.0.jar \
  --applier.root.folder=/app:company_home/app:shared \
  --applier.action=CLASSIFY \
  --applier.action.classify.term.list=English,Spanish,Japanese,Vietnamese
```

>> Once this command has finished, every document in the folder should include a populated `Term` property selected from the Term List (accessible in "view" mode)

7. Upload a number of pictures to an specific folder, for instance `/app:company_home/app:shared/cm:pictures`

8. Run the Alfresco AI Applier to summarize the documents, note that `applier.root.folder` uses this folder already created

```sh
$ cd alfresco-ai/alfresco-ai-applier
$ java -jar target/alfresco-ai-applier-0.8.0.jar \
  --applier.root.folder=/app:company_home/app:shared/cm:pictures \
  --applier.action=DESCRIBE
```

>> Once this command has finished, every picture in the folder should include a populated `Description` property (accessible in "view" mode)


# Use Case 2: New Content

0. Before proceeding, ensure that Docker, ollama, Java, and Maven are up and working:

```sh
$ ollama -v
ollama version is 0.1.31
$ docker -v
Docker version 25.0.3, build 4debf41
$ java -version
openjdk version "17.0.5" 2022-10-18
$ mvn -v
Apache Maven 3.9.6
```

1. Build `alfresco-ai-listener` Docker Image if required

```sh
$ cd alfresco-ai/alfresco-ai-listener
$ mvn clean package
$ docker build . -t alfresco-ai-listener
```

2. Verify that `compose-ai.yaml` is uncommented in [compose.yaml](compose.yaml)

```sh
$ cat compose.yaml
include:
  - genai-stack/compose.yaml
  - alfresco/compose.yaml
  - alfresco/compose-ai.yaml
```

3. Start Docker containers for Alfresco and GenAI Stack

```sh
$ docker compose up
```

4. Use Alfresco Legacy UI, available in http://localhost:8080/share, to get a summary for a document

* Apply Summarizable with AI (`genai:summarizable`) aspect to a node
* Wait until GenAI populates `Summary` property (accesible in "view" mode)

5. Classify a document 

* Apply Classifiable with AI (`genai:classifiable`) aspect to a folder
* Add a list of terms separated by comma in property Terms (`genai:terms`) of the folder
* Add a document inside this folder 
* Apply the aspect Classified with AI (`genai:classified`) to the document
* Wait until GenAI selects one term from the list and populates `Term` property of the document (accesible in "view" mode)

6. Ask a question

* Apply Promptable with AI (`genai:promptable`) aspect to a document
* Type your question in the property Question
* Wait until GenAI populates `Answer` property (accesible in "view" mode)

7. Describe a picture

* Apply Descriptable with AI (`genai:descriptable`) aspect to a picture
* Wait until GenAI populates `Description` property (accesible in "view" mode)

>> These operations may be automated by creating folder rules that apply required aspects to documents uploaded to an specific folder
