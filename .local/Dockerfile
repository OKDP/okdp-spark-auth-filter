ARG BASE_IMAGE=apache/spark:3.5.6-scala2.12-java17-ubuntu
FROM $BASE_IMAGE

COPY .local/spark-defaults.conf /opt/spark/conf/
COPY target/okdp-spark-auth-filter-*-SNAPSHOT.jar /opt/spark/jars/

