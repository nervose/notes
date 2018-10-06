FROM alpine

ENV KUBE_LATEST_VERSION="v1.11.3"

RUN apk --update add git openssh bash && \
    rm -rf /var/lib/apt/lists/* && \
    rm /var/cache/apk/*

RUN sed -i -e "s/bin\/ash/bin\/bash/" /etc/passwd

COPY kubectl /usr/local/bin/kubectl
COPY helm /usr/local/bin/helm
RUN chmod +x /usr/local/bin/kubectl && \
    chmod +x /usr/local/bin/helm
WORKDIR /root
ENTRYPOINT ["/bin/bash"]