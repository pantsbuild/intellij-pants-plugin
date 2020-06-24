FROM openjdk:11

RUN apt update \
    && apt-get install -y libxtst6 libx11-6 libxrender1 xvfb openssh-server python3 \
       python3-pip libssl-dev pkg-config x11-apps imagemagick \
    && curl -Lo /usr/bin/coursier https://git.io/coursier-cli-linux && chmod +x /usr/bin/coursier
ADD . /tests
WORKDIR /tests
RUN ./setup-consents.sh
