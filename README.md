#Account Service

Provides High-Loaded reliable cachable RMI service, that implements interface:

public interface AccountService {

    /**
     * Retrieves current balance or zero if addAmount() method was not called before for specified.
     *
     * @param id balance identifier.
     * @return current balance or zero if addAmount() method was not called before for specified.
     */
    Long getAmount(Integer id);

    /**
     * Increases balance or set if addAmount() method was called first time.
     *     
     * @param id balance identifier.
     * @param value positive or negative value, which must be added to current balance.
     */
    void addAmount(Integer id, Long value);
}

AccountService installation 

1) Install MySQL Server v5.1 or higher, download link: https://dev.mysql.com/downloads/mysql
   
Execute accountDB_DDL.sql on installed MySQL Server;

2) Install message broker Apache Kafka v0.8.2 or higher, download link: http://kafka.apache.org/downloads.html
   
To start message broker go to Kafka installation directory, then type:

Linux:
bin/zookeeper-server-start.sh config/zookeeper.properties

Windows:
bin\windows\zookeeper-server-start.bat config\zookeeper.properties

(If "Unrecognized VM option 'UseCompressedOops'" error occurs, then edit file kafka-run-class and remove string

-XX:+UseCompressedOops)

Next type:

Linux:
bin/kafka-server-start.sh config/server.properties

Windows:
bin\windows\kafka-server-start.bat config\server.properties

Create two Kafka topics:

Linux:
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 5 --topic accountTopic

Windows:
bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 5 --topic accountTopic

Linux:
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 5 --topic testAccountTopic

Windows:
bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 5 --topic testAccountTopic

3) Make sure the AccountService settings in /src/main/resources:

datasource-tx-jpa.xml - database connection settings;

kafka.properties - Kafka Server connection settings;

log4j.xml - log settings;

rmi.xml - rmi settings;

service-ehcache.xml - cache settings.

4) Install Maven. Go to AccountService directory, then:

- to unit test service type: mvn test

- to integration test service (Kafka Server must be launched) type: mvn verify

8) To run AccountService type: mvn exec:java

9) Service statistic and logging control avaliable via JConsole.
