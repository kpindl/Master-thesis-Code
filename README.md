# Master thesis - Kathrin Pindl 

Source code of Master thesis 

Java classes/methods/code snipets implemented by another software developer are labelled accordingly 



## 1. ImportHandler
### Java classes
* **MIDAS_Import_Handler_GATK**: Main class for running the data import
```
processCNVs(MIDAS_DB database)
```
Main function for parsing and processing all raw CNV data files in given directory

```
groupSampleCNVs(Sample sample, HashMap<Integer, Integer> orderReferenceTargets, MIDAS_DB database)
```
Method for grouping targets by means of a dynamic programming approach; Approach consists of three steps: Initialization, Recursion, Traceback

* **MIDAS_DB**: Inits connection to database, executes and caches SQL queries
* **CNV**: POJO Java class of CNV data
* **Target**: POJO Java class of target data




## 2. TargetImport
### Java classes
* **TargetImportMain**: Main class for running the target regions import
```
startImport()
```
Main method: Inserts the target regions and assigns them to the sequencing enrichment version

* **MIDAS_DB**: Inits connection to database, executes and caches SQL queries




## 3. MIDAS Client
### Java classes

* **Controller**: Controller classes define the functionalities of the fxml files
* **Models**: POJO java classes representing data
* **Wrapper**: Wrapper classes are helper classes for executing GraphQL queries and mutations
* **AddToVariantViewTask**: Class sends GraphQL query to server to get all CNV data of a certain sample 


### Resources
The resources folder contains the fxml files that define the user interface. The elements of the fxml file are assigned with an identifier name that can be used in the controller classes to give the elements a functionality. 






## 4. MIDAS GraphQL Server
Spring Boot GraphQL server for interaction between software client and database 

### Java classes

* **SpringBootGraphQLApplication**: Main class that starts the server 

* **GraphQLProvider**: GraphQL schema is built and provided by this class

* **DataFetcher**: DataFetcher classes are responsible for defining the GraphQL schema (queries and mutations) and implement the functionality of these GraphQL operations (e.g. fetching data from the database) 

* **Repository**: Repository interfaces provide the SQL queries that are send to the database (extend the JPARepository)
* **Model**: Each model class represents a SQL table or view in the database; class attributes represent the table columns











