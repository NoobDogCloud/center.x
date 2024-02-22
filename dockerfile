FROM ghcr.io/graalvm/graalvm-community:21
COPY install.json /home/app/
COPY gfw.cfg /home/app/
COPY ./target/#{f} /home/app/
WORKDIR /home/app/
ENTRYPOINT java "-Dfile.encoding=utf-8" -jar #{f}