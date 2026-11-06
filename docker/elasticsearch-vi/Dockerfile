# Elasticsearch 8.19.4 + Vietnamese Analysis Plugin (CocCoc tokenizer).
# vi-search 2026-11-06 — docs/agents/vi-search.md §5.
#
# TAI SAO PHAI BUILD RIENG:
# - ES khoa plugin theo version chinh (8.19.4); plugin CocCoc chi co release
#   9.3.0/8.7.0 nen phai build tu source cho dung 8.19.4.
# - Branch 8.7.0 cua plugin: chua fix "constructor signature changes"
#   dung cho ES 8.x (super(indexSettings, settings, name)). Branch develop
#   da refactor cho ES 9.x (super(name)) — khong compile voi ES 8.19.
# - pom 8.7.0 hardcode <version>8.7.0</version> (khong dung ${revision})
#   nen sed doi version project sang 8.19.4 truoc khi build — ES tu choi
#   plugin sai version.
#
# Stage 1: build libcoccoc_tokenizer_jni.so (C++ + dicts).
# Stage 2: build zip plugin (Maven).
# Stage 3: image ES goc + .so + dicts + plugin install.
#
# Build (tren VPS — may local Windows khong co docker):
#   docker build -t daotrongthuan/elasticsearch-vi:8.19.4 .
FROM docker.elastic.co/elasticsearch/elasticsearch:8.19.4 AS coccoc-builder
USER root
ENV JAVA_HOME=/usr/share/elasticsearch/jdk
ENV PATH=$JAVA_HOME/bin:$PATH
# ES 8.19 image = Ubuntu/Debian base → apt, khong phai microdnf (UBI).
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc g++ make cmake pkg-config git gzip tar ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /tmp
RUN git clone --depth 1 https://github.com/duydo/coccoc-tokenizer.git \
    && mkdir coccoc-tokenizer/build
WORKDIR /tmp/coccoc-tokenizer/build
RUN cmake -DJAVA_HOME=${JAVA_HOME} -DBUILD_JAVA=1 .. && make install

FROM docker.elastic.co/elasticsearch/elasticsearch:8.19.4 AS plugin-builder
USER root
ENV JAVA_HOME=/usr/share/elasticsearch/jdk
ENV PATH=$JAVA_HOME/bin:$PATH
RUN apt-get update && apt-get install -y --no-install-recommends gzip tar ca-certificates curl git \
    && rm -rf /var/lib/apt/lists/*
ARG MVN_VERSION=3.9.9
RUN curl -fsSL -o /tmp/maven.tar.gz \
    https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/apache-maven-${MVN_VERSION}-bin.tar.gz \
    && tar xf /tmp/maven.tar.gz -C /opt && rm /tmp/maven.tar.gz
ENV MVN_HOME=/opt/apache-maven-${MVN_VERSION}
ENV PATH=$MVN_HOME/bin:$PATH
WORKDIR /tmp/elasticsearch-analysis-vietnamese
# Branch 8.7.0 (constructor dung ES 8.x) + 2 bu fix cho ES >= 8.19 entitlement:
# 1) src/main/plugin-metadata/entitlement-policy.yaml khai bao
#    load_native_libraries (ES 8.19 khoa JNI neu khong co — NotEntitledException
#    crash luc tao index; backport tu commit f15be9c6 cua branch develop).
# 2) plugin.xml assembly them file entitlement-policy.yaml vao zip plugin.
RUN git clone --depth 1 -b 8.7.0 https://github.com/duydo/elasticsearch-analysis-vietnamese.git /tmp/src \
    && cp -r /tmp/src/pom.xml /tmp/src/src ./ \
    && sed -i '0,/<version>8.7.0<\/version>/s//<version>8.19.4<\/version>/' pom.xml \
    && mkdir -p src/main/plugin-metadata \
    && printf 'ALL-UNNAMED:\n  - load_native_libraries\n' > src/main/plugin-metadata/entitlement-policy.yaml \
    && python3 - <<'PYEOF'
import re
p = 'src/main/assemblies/plugin.xml'
s = open(p).read()
if 'entitlement-policy.yaml' not in s:
    marker = '</files>'
    add = '''        <file>
            <source>${project.basedir}/src/main/plugin-metadata/entitlement-policy.yaml</source>
            <filtered>true</filtered>
        </file>
'''
    s = s.replace(marker, add + '    ' + marker)
    open(p, 'w').write(s)
    print('plugin.xml patched')
else:
    print('already present')
PYEOF
RUN mvn --batch-mode -Dmaven.test.skip -e package \
    && ls target/releases/

FROM docker.elastic.co/elasticsearch/elasticsearch:8.19.4
USER root
COPY --from=coccoc-builder /usr/local/lib/libcoccoc_tokenizer_jni.so /usr/lib/
COPY --from=coccoc-builder /usr/local/share/tokenizer/dicts /usr/local/share/tokenizer/dicts
COPY --from=plugin-builder \
    /tmp/elasticsearch-analysis-vietnamese/target/releases/elasticsearch-analysis-vietnamese-8.19.4.zip /tmp/
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch \
        file:///tmp/elasticsearch-analysis-vietnamese-8.19.4.zip \
    && rm /tmp/elasticsearch-analysis-vietnamese-8.19.4.zip
USER elasticsearch
