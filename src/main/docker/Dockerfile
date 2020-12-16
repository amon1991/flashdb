# From java image, version : 8
FROM hub.c.163.com/library/java:8-jre
MAINTAINER yaming.chen <yaming.chen@foxmail.com>

# We added a VOLUME pointing to "/tmp" because that is where a Spring Boot application creates working directories for Tomcat by default. The effect is to create a temporary file on your host under "/var/lib/docker" and link it to the container under "/tmp".
VOLUME /tmp

# COPY or ADD to image
COPY flashtsdb-1.0.1-RELEASE.jar app.jar

#RUN bash -c "touch /app.jar"
EXPOSE 8066

ENTRYPOINT ["sh","-c","java -jar app.jar ${BOOT_OPTIONS}"]