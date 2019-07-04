FROM anapsix/alpine-java:8_server-jre

ARG VERSION
COPY target/atomix-raft-protocol.tar.gz /root/atomix-raft-protocol.tar.gz
RUN tar -xvf /root/atomix-raft-protocol.tar.gz -C /root && \
    mv /root/atomix-raft-protocol /root/atomix && \
    chmod u+x /root/atomix/bin/atomix-node && \
    rm /root/atomix-raft-protocol.tar.gz

WORKDIR /root/atomix

EXPOSE 5678

ENTRYPOINT ["./bin/atomix-node"]
