#!/bin/sh
mvn -q clean package -DskipTests && java -jar target/subscription-billing-management-system-1.0-jar-with-dependencies.jar
