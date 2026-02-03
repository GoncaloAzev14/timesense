
# Get all the dependencies to support running curl so that we can use
# that for our health checks
FROM registry.access.redhat.com/ubi9 AS ubi-micro-build
RUN mkdir -p /mnt/rootfs
RUN dnf install --installroot /mnt/rootfs curl \
                --releasever 9 --setopt install_weak_deps=false --nodocs -y && \
    dnf --installroot /mnt/rootfs clean all && \
    rpm --root /mnt/rootfs -e --nodeps setup

# Prepare keycloak for executiion
FROM quay.io/keycloak/keycloak:latest as builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_FEATURES=hostname:v2

# Configure a database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak
# for demonstration purposes only, please make sure to use proper certificates in production instead
RUN keytool -genkeypair -storepass password \
            -storetype PKCS12 -keyalg RSA -keysize 2048 \
            -dname "CN=server" -alias server \
            -ext "SAN:c=DNS:localhost,IP:127.0.0.1" \
            -keystore conf/server.keystore
RUN /opt/keycloak/bin/kc.sh build

# Build the final image collecting curl from the base builder and keycloak
# from the keycloak builder
FROM quay.io/keycloak/keycloak:latest
COPY --from=ubi-micro-build /mnt/rootfs /
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# change these values to point to a running postgres instance
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--health-enabled=true", "--http-enabled=true"]