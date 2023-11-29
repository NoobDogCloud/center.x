FROM openjdk:21-jdk-bullseye
COPY install.json /home/app/
COPY gfw.cfg /home/app/
COPY ./target/#{f} /home/app/
WORKDIR /home/app/
ENTRYPOINT java "-Dfile.encoding=utf-8" -jar #{f}