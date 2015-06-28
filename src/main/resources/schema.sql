CREATE TABLE Account (
  id int NOT NULL,
  amount BIGINT NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE kafkapartition (
	id INT NOT NULL,
	offset BIGINT NOT NULL,
	PRIMARY KEY (id)
);