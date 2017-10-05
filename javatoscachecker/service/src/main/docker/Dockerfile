FROM openjdk:8

MAINTAINER "sj2381@att.com"

ADD Service-1.0.0.jar /opt/modeling/Service-1.0.0.jar
ADD application.properties /opt/modeling/application.properties
VOLUME /etc
ADD startService.sh /opt/modeling/startService.sh
RUN chmod 700 /opt/modeling/startService.sh

WORKDIR /opt/modeling/
EXPOSE 8080
ENTRYPOINT /opt/modeling/startService.sh


