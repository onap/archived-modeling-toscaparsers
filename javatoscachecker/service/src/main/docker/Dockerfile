FROM ubuntu:16.04

MAINTAINER "sj2381@att.com"
LABEL Description="This immage contains an updated ubuntu 16.04 with the openjdk installed" Version="16.04-8"

ADD Service-1.0.0-SNAPSHOT.jar /opt/modeling/Service-1.0.0-SNAPSHOT.jar
ADD application.properties /opt/modeling/application.properties
VOLUME /etc
ADD startService.sh /opt/modeling/startService.sh
RUN chmod 700 /opt/modeling/startService.sh

WORKDIR /opt/modeling/
ENTRYPOINT ./startService.sh 
