FROM openjdk:11

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list \
    && curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add \
    && apt-get update \
    && apt-get install -y libxtst6 libx11-6 libxrender1 xvfb openssh-server python3 \
        python3-pip sbt libssl-dev pkg-config x11-apps imagemagick \
    && curl -Lo /usr/bin/coursier https://git.io/coursier-cli-linux && chmod +x /usr/bin/coursier \
    && sbt --version

ADD . /tests
WORKDIR /tests
RUN ci/setup-consents.sh
